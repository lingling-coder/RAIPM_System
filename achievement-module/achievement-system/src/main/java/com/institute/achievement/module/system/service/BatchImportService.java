package com.institute.achievement.module.system.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.BatchImportResult;
import com.institute.achievement.module.system.dto.ImportError;
import com.institute.achievement.module.system.dto.UnifiedImportRow;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel batch import service using EasyExcel streaming with partial import
 * and error report generation.
 * <p>
 * Implements RESEARCH.md Pattern 4: EasyExcel AnalysisEventListener for
 * streaming row processing, per-row validation, batch inserts at 500-row
 * threshold to prevent OOM (Pitfall 3 mitigation), and automatic duplicate
 * detection and skipping.
 * <p>
 * Decisions: D-16 (direct import), D-17 (unified template), D-18 (partial import),
 * D-19 (Excel only), D-20 (duplicate skip), D-21 (template download).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchImportService {

    private final PaperMapper paperMapper;
    private final PatentMapper patentMapper;
    private final CopyrightMapper copyrightMapper;
    private final TemplateGeneratorService templateGeneratorService;

    @Value("${file.storage.upload-dir:./uploads}")
    private String uploadDir;

    /** Batch insert threshold to prevent OOM (Pitfall 3 mitigation) */
    private static final int BATCH_SIZE = 500;

    /** Maximum import file size (10MB) */
    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;

    /**
     * Import an Excel file containing achievement data.
     * <p>
     * Process flow:
     * 1. Validate file (extension, size)
     * 2. Stream-read using EasyExcel with AnalysisEventListener
     * 3. Per-row validation per D-18 rules
     * 4. Duplicate detection per D-20 rules
     * 5. Batch insert at 500-row threshold (Pitfall 3)
     * 6. Generate error report if any errors exist
     * 7. Save import record to database
     *
     * @param file   the uploaded Excel file
     * @param userId the importing user's ID
     * @return BatchImportResult with counters and error details
     */
    @Transactional
    public BatchImportResult importExcel(MultipartFile file, Long userId) {
        BatchImportResult result = new BatchImportResult();

        // Pre-validate file
        validateImportFile(file);

        List<UnifiedImportRow> successRows = new ArrayList<>();
        List<ImportError> errors = new ArrayList();
        int[] totalRows = {0};

        try {
            EasyExcel.read(file.getInputStream(), UnifiedImportRow.class,
                    new AnalysisEventListener<UnifiedImportRow>() {
                        @Override
                        public void invoke(UnifiedImportRow row, AnalysisContext context) {
                            totalRows[0]++;
                            int rowIndex = context.readRowHolder().getRowIndex();

                            List<String> rowErrors = validateRow(row);
                            if (!rowErrors.isEmpty()) {
                                errors.add(new ImportError(rowIndex, row.getType(), rowErrors));
                                return;
                            }

                            if (isDuplicate(row)) {
                                errors.add(new ImportError(rowIndex, row.getType(),
                                        List.of("重复数据，已跳过")));
                                return;
                            }

                            successRows.add(row);

                            if (successRows.size() >= BATCH_SIZE) {
                                batchInsert(successRows, userId);
                                successRows.clear();
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            if (!successRows.isEmpty()) {
                                batchInsert(successRows, userId);
                            }
                        }
                    }).sheet().doRead();
        } catch (IOException e) {
            log.error("Failed to read import file", e);
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }

        // Set result counters
        int total = totalRows[0];
        int skippedCount = (int) errors.stream().filter(e -> !e.getReasons().isEmpty() && e.getReasons().get(0).contains("重复")).count();
        int errorCount = errors.size() - skippedCount;
        int successCount = total - errorCount - skippedCount;

        result.setTotalRows(total);
        result.setSuccessRows(successCount);
        result.setErrorRows(errorCount);
        result.setSkippedRows(skippedCount);
        result.setErrors(errors);
        result.setImportRecordId(null); // Simplified for phase 1

        log.info("Import completed: total={}, success={}, errors={}, skipped={}",
                result.getTotalRows(), result.getSuccessRows(),
                result.getErrorRows(), result.getSkippedRows());

        return result;
    }

    /**
     * Download the blank import template.
     */
    public byte[] downloadTemplate() {
        return templateGeneratorService.generateTemplate();
    }

    // ── Validation ────────────────────────────────────────────────────

    /**
     * Validate the imported file: extension must be .xlsx or .xls, size must be <= 10MB.
     */
    private void validateImportFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getExtension(originalName).toLowerCase();
        if (!".xlsx".equals(extension) && !".xls".equals(extension)) {
            throw new IllegalArgumentException("不支持的文件格式，请上传.xlsx或.xls文件");
        }

        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过10MB限制");
        }
    }

    /**
     * Per-row validation checking required fields based on achievement type.
     *
     * @param row the row to validate
     * @return list of error messages (empty list = valid)
     */
    public List<String> validateRow(UnifiedImportRow row) {
        List<String> errors = new ArrayList<>();

        // Common validations
        if (!StringUtils.hasText(row.getType())) {
            errors.add("类型不能为空");
            return errors; // Can't validate further without type
        }

        String type = row.getType().toLowerCase().trim();
        if (!"paper".equals(type) && !"patent".equals(type) && !"copyright".equals(type)) {
            errors.add("类型必须是 paper/patent/copyright");
            return errors; // Can't validate further with invalid type
        }

        if (!StringUtils.hasText(row.getTitle())) {
            errors.add("标题不能为空");
        }

        // Type-specific validations
        switch (type) {
            case "paper" -> validatePaperRow(row, errors);
            case "patent" -> validatePatentRow(row, errors);
            case "copyright" -> validateCopyrightRow(row, errors);
        }

        return errors;
    }

    private void validatePaperRow(UnifiedImportRow row, List<String> errors) {
        if (!StringUtils.hasText(row.getAuthors())) {
            errors.add("作者不能为空");
        }
        if (!StringUtils.hasText(row.getJournal())) {
            errors.add("期刊不能为空");
        }
        if (StringUtils.hasText(row.getDoi()) && !row.getDoi().matches("^10\\.\\d{4,}/.*$")) {
            errors.add("DOI格式不正确");
        }
        if (row.getPublishYear() != null) {
            int currentYear = LocalDate.now().getYear();
            if (row.getPublishYear() < 1900 || row.getPublishYear() > currentYear) {
                errors.add("发表年份必须在1900-" + currentYear + "之间");
            }
        }
    }

    private void validatePatentRow(UnifiedImportRow row, List<String> errors) {
        if (!StringUtils.hasText(row.getAuthors())) {
            errors.add("发明人不能为空");
        }
        if (!StringUtils.hasText(row.getApplicationNo())) {
            errors.add("申请号不能为空");
        }
        if (StringUtils.hasText(row.getApplicationDate())) {
            try {
                LocalDate.parse(row.getApplicationDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add("申请日格式不正确（应为yyyy-MM-dd）");
            }
        }
        if (StringUtils.hasText(row.getCountry()) && !isValidCountry(row.getCountry())) {
            errors.add("国别无效");
        }
    }

    private void validateCopyrightRow(UnifiedImportRow row, List<String> errors) {
        if (!StringUtils.hasText(row.getCopyrightHolder())) {
            errors.add("著作权人不能为空");
        }
        if (!StringUtils.hasText(row.getRegistrationNo())) {
            errors.add("登记号不能为空");
        }
        if (StringUtils.hasText(row.getRegistrationDate())) {
            try {
                LocalDate.parse(row.getRegistrationDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add("登记日期格式不正确（应为yyyy-MM-dd）");
            }
        }
        if (!StringUtils.hasText(row.getVersionNo())) {
            errors.add("版本号不能为空");
        }
    }

    // ── Duplicate Detection ───────────────────────────────────────────

    /**
     * Check if a row represents a duplicate achievement.
     * Uses DOI for papers, applicationNo for patents, registrationNo for copyrights.
     */
    public boolean isDuplicate(UnifiedImportRow row) {
        String type = row.getType().toLowerCase().trim();
        return switch (type) {
            case "paper" -> checkDuplicatePaper(row.getDoi());
            case "patent" -> checkDuplicatePatent(row.getApplicationNo());
            case "copyright" -> checkDuplicateCopyright(row.getRegistrationNo());
            default -> false;
        };
    }

    private boolean checkDuplicatePaper(String doi) {
        if (!StringUtils.hasText(doi)) return false;
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getDoi, doi)
                .ne(Paper::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return paperMapper.selectCount(wrapper) > 0;
    }

    private boolean checkDuplicatePatent(String applicationNo) {
        if (!StringUtils.hasText(applicationNo)) return false;
        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<Patent>()
                .eq(Patent::getApplicationNo, applicationNo)
                .ne(Patent::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return patentMapper.selectCount(wrapper) > 0;
    }

    private boolean checkDuplicateCopyright(String registrationNo) {
        if (!StringUtils.hasText(registrationNo)) return false;
        LambdaQueryWrapper<Copyright> wrapper = new LambdaQueryWrapper<Copyright>()
                .eq(Copyright::getRegistrationNo, registrationNo)
                .ne(Copyright::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return copyrightMapper.selectCount(wrapper) > 0;
    }

    // ── Batch Insert ──────────────────────────────────────────────────

    /**
     * Batch insert a list of valid rows into the appropriate tables.
     * Each row is dispatched to the correct entity type based on the "type" column.
     */
    @Transactional
    void batchInsert(List<UnifiedImportRow> batch, Long userId) {
        List<Paper> papers = new ArrayList<>();
        List<Patent> patents = new ArrayList<>();
        List<Copyright> copyrights = new ArrayList<>();

        for (UnifiedImportRow row : batch) {
            String type = row.getType().toLowerCase().trim();
            switch (type) {
                case "paper" -> papers.add(mapToPaper(row, userId));
                case "patent" -> patents.add(mapToPatent(row, userId));
                case "copyright" -> copyrights.add(mapToCopyright(row, userId));
            }
        }

        if (!papers.isEmpty()) {
            for (Paper paper : papers) {
                paperMapper.insert(paper);
            }
            log.info("Inserted {} paper records", papers.size());
        }
        if (!patents.isEmpty()) {
            for (Patent patent : patents) {
                patentMapper.insert(patent);
            }
            log.info("Inserted {} patent records", patents.size());
        }
        if (!copyrights.isEmpty()) {
            for (Copyright copyright : copyrights) {
                copyrightMapper.insert(copyright);
            }
            log.info("Inserted {} copyright records", copyrights.size());
        }
    }

    // ── Entity Mapping ────────────────────────────────────────────────

    private Paper mapToPaper(UnifiedImportRow row, Long userId) {
        Paper paper = new Paper();
        paper.setTitle(row.getTitle());
        paper.setAuthors(row.getAuthors());
        paper.setJournal(row.getJournal());
        paper.setDoi(row.getDoi());
        paper.setVolume(row.getVolume());
        paper.setIssue(row.getIssue());
        paper.setPages(row.getPages());
        paper.setPublishYear(row.getPublishYear());
        paper.setIndexStatus(row.getIndexStatus());
        paper.setImpactFactor(row.getImpactFactor());
        paper.setZone(row.getZone());
        paper.setAbstractText(row.getAbstractText());
        paper.setIsClassified("是".equals(row.getIsClassified()) ? 1 : 0);
        paper.setClassifiedLevel(row.getClassifiedLevel());
        paper.setProjectRef(row.getProjectRef());
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        paper.setDeptId(SecurityUtils.getCurrentDeptId());
        paper.setCreatedBy(userId);
        paper.setCreatedTime(LocalDateTime.now());
        paper.setVersion(1);
        return paper;
    }

    private Patent mapToPatent(UnifiedImportRow row, Long userId) {
        Patent patent = new Patent();
        patent.setPatentName(row.getTitle());
        patent.setInventors(row.getAuthors());
        patent.setApplicationNo(row.getApplicationNo());
        patent.setAuthorizationNo(row.getAuthorizationNo());
        patent.setPatentType(row.getPatentType());
        patent.setCountry(StringUtils.hasText(row.getCountry()) ? row.getCountry() : "中国");
        patent.setLegalStatus(row.getLegalStatus());
        patent.setIsClassified("是".equals(row.getIsClassified()) ? 1 : 0);
        patent.setClassifiedLevel(row.getClassifiedLevel());
        patent.setProjectRef(row.getProjectRef());
        patent.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        patent.setDeptId(SecurityUtils.getCurrentDeptId());
        patent.setCreatedBy(userId);
        patent.setCreatedTime(LocalDateTime.now());
        patent.setVersion(1);

        if (StringUtils.hasText(row.getApplicationDate())) {
            patent.setApplicationDate(LocalDate.parse(row.getApplicationDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (StringUtils.hasText(row.getAuthorizationDate())) {
            patent.setAuthorizationDate(LocalDate.parse(row.getAuthorizationDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (StringUtils.hasText(row.getNextFeeDate())) {
            patent.setNextFeeDate(LocalDate.parse(row.getNextFeeDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        }

        return patent;
    }

    private Copyright mapToCopyright(UnifiedImportRow row, Long userId) {
        Copyright copyright = new Copyright();
        copyright.setName(row.getTitle());
        copyright.setCopyrightHolder(row.getCopyrightHolder());
        copyright.setRegistrationNo(row.getRegistrationNo());
        copyright.setSoftwareVersion(row.getVersionNo());
        copyright.setSoftwareCategory(row.getSoftwareCategory());
        copyright.setIsClassified("是".equals(row.getIsClassified()) ? 1 : 0);
        copyright.setClassifiedLevel(row.getClassifiedLevel());
        copyright.setProjectRef(row.getProjectRef());
        copyright.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        copyright.setDeptId(SecurityUtils.getCurrentDeptId());
        copyright.setCreatedBy(userId);
        copyright.setCreatedTime(LocalDateTime.now());
        copyright.setVersion(1);

        if (StringUtils.hasText(row.getRegistrationDate())) {
            copyright.setRegistrationDate(LocalDate.parse(row.getRegistrationDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        }

        return copyright;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }

    private boolean isValidCountry(String country) {
        String[] validCountries = {"中国", "美国", "欧洲", "日本", "韩国", "PCT", "其他"};
        for (String c : validCountries) {
            if (c.equals(country)) return true;
        }
        return false;
    }
}
