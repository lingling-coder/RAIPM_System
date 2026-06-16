package com.institute.achievement.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.module.system.dto.DuplicateCheckResult;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Unified duplicate detection service for all achievement types.
 * <p>
 * Provides submit-time duplicate checking (D-45) per achievement type:
 * <ul>
 *   <li>Paper: DOI uniqueness (allows null DOI, check only non-null)</li>
 *   <li>Patent: application_no uniqueness (required field)</li>
 *   <li>Copyright: registration_no uniqueness (required field)</li>
 * </ul>
 * <p>
 * D-47: Draft submissions skip duplicate check entirely.
 * T-01-19: Duplicate detection enforced server-side in submit method,
 * not just in frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateCheckService {

    private final PaperMapper paperMapper;
    private final PatentMapper patentMapper;
    private final CopyrightMapper copyrightMapper;

    // ── Paper DOI ──────────────────────────────────────────────────────

    /**
     * Find an existing paper with the same DOI (excluding INVALIDATED status).
     *
     * @param doi the DOI to check
     * @return Optional containing the existing paper, or empty if none found
     */
    public Optional<Paper> findExistingByDoi(String doi) {
        if (!StringUtils.hasText(doi)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getDoi, doi)
                .ne(Paper::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return Optional.ofNullable(paperMapper.selectOne(wrapper));
    }

    // ── Patent ApplicationNo ───────────────────────────────────────────

    /**
     * Find an existing patent with the same application number (excluding INVALIDATED).
     *
     * @param applicationNo the application number to check
     * @return Optional containing the existing patent, or empty if none found
     */
    public Optional<Patent> findExistingByApplicationNo(String applicationNo) {
        if (!StringUtils.hasText(applicationNo)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<Patent>()
                .eq(Patent::getApplicationNo, applicationNo)
                .ne(Patent::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return Optional.ofNullable(patentMapper.selectOne(wrapper));
    }

    // ── Copyright RegistrationNo ───────────────────────────────────────

    /**
     * Find an existing copyright with the same registration number (excluding INVALIDATED).
     *
     * @param registrationNo the registration number to check
     * @return Optional containing the existing copyright, or empty if none found
     */
    public Optional<Copyright> findExistingByRegistrationNo(String registrationNo) {
        if (!StringUtils.hasText(registrationNo)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<Copyright> wrapper = new LambdaQueryWrapper<Copyright>()
                .eq(Copyright::getRegistrationNo, registrationNo)
                .ne(Copyright::getStatus, AchievementStatusEnum.INVALIDATED.name());
        return Optional.ofNullable(copyrightMapper.selectOne(wrapper));
    }

    // ── Unified Submit-Time Check (D-45/D-47) ──────────────────────────

    /**
     * Check for duplicate achievement at submit time.
     * <p>
     * Returns a DuplicateCheckResult with existing achievement info if a
     * duplicate is found, or a no-duplicate result otherwise.
     * <p>
     * D-47: Drafts skip duplicate check (handled by caller — this method
     * checks the unique field value; if null/empty, no duplicate is returned).
     *
     * @param achievementType paper/patent/copyright
     * @param uniqueField     the unique field value (DOI/applicationNo/registrationNo)
     * @return DuplicateCheckResult with existing achievement details if duplicate found
     */
    public DuplicateCheckResult checkDuplicateForSubmit(String achievementType, String uniqueField) {
        // D-47: No unique field means no duplicate check possible
        if (!StringUtils.hasText(uniqueField)) {
            return DuplicateCheckResult.noDuplicate();
        }

        return switch (achievementType.toLowerCase()) {
            case "paper" -> checkPaperDuplicate(uniqueField);
            case "patent" -> checkPatentDuplicate(uniqueField);
            case "copyright" -> checkCopyrightDuplicate(uniqueField);
            default -> DuplicateCheckResult.noDuplicate();
        };
    }

    // ── Internal ───────────────────────────────────────────────────────

    private DuplicateCheckResult checkPaperDuplicate(String doi) {
        Optional<Paper> existing = findExistingByDoi(doi);
        if (existing.isEmpty()) {
            return DuplicateCheckResult.noDuplicate();
        }
        Paper paper = existing.get();
        return DuplicateCheckResult.builder()
                .duplicate(true)
                .existingId(paper.getId())
                .existingTitle(paper.getTitle())
                .existingType("论文")
                .existingStatus(getStatusLabel(paper.getStatus()))
                .existingSubmitTime(paper.getCreatedTime() != null
                        ? paper.getCreatedTime().toString() : null)
                .build();
    }

    private DuplicateCheckResult checkPatentDuplicate(String applicationNo) {
        Optional<Patent> existing = findExistingByApplicationNo(applicationNo);
        if (existing.isEmpty()) {
            return DuplicateCheckResult.noDuplicate();
        }
        Patent patent = existing.get();
        return DuplicateCheckResult.builder()
                .duplicate(true)
                .existingId(patent.getId())
                .existingTitle(patent.getPatentName())
                .existingType("专利")
                .existingStatus(getStatusLabel(patent.getStatus()))
                .existingSubmitTime(patent.getCreatedTime() != null
                        ? patent.getCreatedTime().toString() : null)
                .build();
    }

    private DuplicateCheckResult checkCopyrightDuplicate(String registrationNo) {
        Optional<Copyright> existing = findExistingByRegistrationNo(registrationNo);
        if (existing.isEmpty()) {
            return DuplicateCheckResult.noDuplicate();
        }
        Copyright copyright = existing.get();
        return DuplicateCheckResult.builder()
                .duplicate(true)
                .existingId(copyright.getId())
                .existingTitle(copyright.getName())
                .existingType("软件著作权")
                .existingStatus(getStatusLabel(copyright.getStatus()))
                .existingSubmitTime(copyright.getCreatedTime() != null
                        ? copyright.getCreatedTime().toString() : null)
                .build();
    }

    /**
     * Resolve status enum name to Chinese label.
     */
    private String getStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        try {
            return AchievementStatusEnum.fromName(status).getLabel();
        } catch (Exception e) {
            return status;
        }
    }
}
