package com.institute.achievement.module.system;

import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.module.system.dto.UnifiedImportRow;
import com.institute.achievement.module.system.service.BatchImportService;
import com.institute.achievement.module.system.service.TemplateGeneratorService;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.mapper.PatentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchImportService covering Excel parsing, per-row validation,
 * partial import, duplicate detection, and batch threshold behavior.
 * <p>
 * Requirements: REG-05 (Excel batch import)
 * Decisions: D-16 (direct import), D-17 (unified template), D-18 (partial import),
 * D-19 (Excel only), D-20 (duplicate skip)
 */
@ExtendWith(MockitoExtension.class)
class BatchImportServiceTest {

    @Mock
    private PaperMapper paperMapper;
    @Mock
    private PatentMapper patentMapper;
    @Mock
    private CopyrightMapper copyrightMapper;
    @Mock
    private TemplateGeneratorService templateGeneratorService;

    private BatchImportService batchImportService;

    @BeforeEach
    void setUp() {
        batchImportService = new BatchImportService(
                paperMapper, patentMapper, copyrightMapper, templateGeneratorService);
    }

    @Test
    void testValidateRow_AllFieldsValid() {
        UnifiedImportRow row = createValidPaperRow();
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).isEmpty();
    }

    @Test
    void testValidateRow_MissingType() {
        UnifiedImportRow row = createValidPaperRow();
        row.setType(null);
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("类型"));
    }

    @Test
    void testValidateRow_InvalidType() {
        UnifiedImportRow row = createValidPaperRow();
        row.setType("invalid");
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("类型必须"));
    }

    @Test
    void testValidateRow_MissingTitle() {
        UnifiedImportRow row = createValidPaperRow();
        row.setTitle(null);
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("标题"));
    }

    @Test
    void testValidateRow_PaperMissingAuthorAndJournal() {
        UnifiedImportRow row = createValidPaperRow();
        row.setAuthors(null);
        row.setJournal(null);
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("作者"));
        assertThat(errors).anyMatch(e -> e.contains("期刊"));
    }

    @Test
    void testValidateRow_PatentMissingInventorAndApplicationNo() {
        UnifiedImportRow row = createValidPatentRow();
        row.setAuthors(null);
        row.setApplicationNo(null);
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("发明人"));
        assertThat(errors).anyMatch(e -> e.contains("申请号"));
    }

    @Test
    void testValidateRow_CopyrightMissingHolderAndRegistrationNo() {
        UnifiedImportRow row = createValidCopyrightRow();
        row.setCopyrightHolder(null);
        row.setRegistrationNo(null);
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("著作权人"));
        assertThat(errors).anyMatch(e -> e.contains("登记号"));
    }

    @Test
    void testValidateRow_InvalidDoiFormat() {
        UnifiedImportRow row = createValidPaperRow();
        row.setDoi("invalid-doi");
        List<String> errors = batchImportService.validateRow(row);
        assertThat(errors).anyMatch(e -> e.contains("DOI格式"));
    }

    @Test
    void testIsDuplicate_PaperByDoi() {
        UnifiedImportRow row = createValidPaperRow();
        row.setDoi("10.1234/duplicate");
        when(paperMapper.selectCount(any())).thenReturn(1L);

        boolean result = batchImportService.isDuplicate(row);
        assertThat(result).isTrue();
    }

    @Test
    void testIsDuplicate_PatentByApplicationNo() {
        UnifiedImportRow row = createValidPatentRow();
        row.setApplicationNo("CN2023-001");
        when(patentMapper.selectCount(any())).thenReturn(1L);

        boolean result = batchImportService.isDuplicate(row);
        assertThat(result).isTrue();
    }

    @Test
    void testIsDuplicate_CopyrightByRegistrationNo() {
        UnifiedImportRow row = createValidCopyrightRow();
        row.setRegistrationNo("REG2023-001");
        when(copyrightMapper.selectCount(any())).thenReturn(1L);

        boolean result = batchImportService.isDuplicate(row);
        assertThat(result).isTrue();
    }

    @Test
    void testIsDuplicate_NotDuplicate() {
        UnifiedImportRow row = createValidPaperRow();
        row.setDoi("10.1234/new-paper");
        when(paperMapper.selectCount(any())).thenReturn(0L);

        boolean result = batchImportService.isDuplicate(row);
        assertThat(result).isFalse();
    }

    @Test
    void testImportExcel_EmptyFile() {
        assertThatThrownBy(() -> {
            MultipartFile emptyFile = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
            batchImportService.importExcel(emptyFile, 1L);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("为空");
    }

    @Test
    void testImportExcel_InvalidFileExtension() {
        assertThatThrownBy(() -> {
            MultipartFile invalidExtFile = new MockMultipartFile("file", "test.csv",
                    "text/csv", "data".getBytes());
            batchImportService.importExcel(invalidExtFile, 1L);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的文件格式");
    }

    @Test
    void testDownloadTemplate() {
        byte[] templateData = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(templateGeneratorService.generateTemplate()).thenReturn(templateData);

        byte[] result = batchImportService.downloadTemplate();

        assertThat(result).isEqualTo(templateData);
        verify(templateGeneratorService).generateTemplate();
    }

    // ── Test Helpers ───────────────────────────────────────────────────

    private UnifiedImportRow createValidPaperRow() {
        UnifiedImportRow row = new UnifiedImportRow();
        row.setType("paper");
        row.setTitle("Test Paper Title");
        row.setAuthors("Author A; Author B");
        row.setJournal("Test Journal");
        row.setDoi("10.1234/test-paper");
        row.setPublishYear(2023);
        row.setIndexStatus("SCI");
        row.setAbstractText("This is a test abstract");
        row.setVolume(10);
        row.setIssue(2);
        row.setPages("100-110");
        row.setImpactFactor(new java.math.BigDecimal("3.5"));
        row.setZone("一区");
        return row;
    }

    private UnifiedImportRow createValidPatentRow() {
        UnifiedImportRow row = new UnifiedImportRow();
        row.setType("patent");
        row.setTitle("Test Patent");
        row.setAuthors("Inventor A; Inventor B");
        row.setApplicationNo("CN20230001");
        row.setPatentType("发明");
        row.setCountry("中国");
        row.setApplicationDate("2023-01-15");
        row.setLegalStatus("授权");
        return row;
    }

    private UnifiedImportRow createValidCopyrightRow() {
        UnifiedImportRow row = new UnifiedImportRow();
        row.setType("copyright");
        row.setTitle("Test Software");
        row.setCopyrightHolder("Institute A");
        row.setRegistrationNo("REG20230001");
        row.setRegistrationDate("2023-06-01");
        row.setVersionNo("V1.0");
        row.setSoftwareCategory("应用软件");
        return row;
    }
}
