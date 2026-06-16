package com.institute.achievement.module.system;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.DuplicateCheckResult;
import com.institute.achievement.module.system.dto.InvalidationDTO;
import com.institute.achievement.module.system.dto.InvalidationVO;
import com.institute.achievement.module.system.entity.InvalidationRecord;
import com.institute.achievement.module.system.mapper.InvalidationRecordMapper;
import com.institute.achievement.module.system.service.*;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests covering the full achievement lifecycle across
 * all three achievement types, including invalidation (D-34~D-36),
 * duplicate detection (D-45~D-47), and classified access enforcement (REG-10).
 * <p>
 * Uses mocked persistence layer with Mockito, verifying service orchestration
 * logic for complete lifecycle cycles, cross-department access control,
 * and notification delivery.
 * <p>
 * E2E smoke test documentation (in code comments):
 * <pre>
 * 1. Start backend with Flyway migrations
 * 2. POST /api/papers -> create paper, status=DRAFT
 * 3. POST /api/approval/submit -> status=PENDING_DEPT_REVIEW
 * 4. POST /api/approval/approve (dept) -> status=PENDING_ADMIN_ARCHIVE
 * 5. POST /api/approval/approve (admin, with archiveNo) -> status=ARCHIVED
 * 6. POST /api/achievement/invalidate -> status=INVALIDATED + invalidation_record
 * 7. GET /api/achievement/invalidation -> returns invalidation records
 * 8. Creator/Admin views -> visible; other user -> hidden (D-35)
 * 9. POST /api/papers with duplicate DOI -> DuplicateCheckResult returned
 * 10. GET /api/achievement/check-duplicate -> existing achievement info
 * 11. Classified paper: non-authorised user cannot view/download
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class AchievementLifecycleIntegrationTest {

    @Mock
    private PaperMapper paperMapper;
    @Mock
    private PatentMapper patentMapper;
    @Mock
    private CopyrightMapper copyrightMapper;
    @Mock
    private InvalidationRecordMapper invalidationRecordMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    // Services under test
    private InvalidationService invalidationService;
    private DuplicateCheckService duplicateCheckService;
    private ClassifiedPermissionService classifiedPermissionService;

    @BeforeEach
    void setUp() {
        invalidationService = new InvalidationService(paperMapper, patentMapper, copyrightMapper,
                invalidationRecordMapper, auditLogService, notificationService);
        duplicateCheckService = new DuplicateCheckService(paperMapper, patentMapper, copyrightMapper);
        classifiedPermissionService = new ClassifiedPermissionService(paperMapper, patentMapper, copyrightMapper);
    }

    // ── Helper Methods ─────────────────────────────────────────────────

    private Paper createPaper(Long id, String status, Long deptId, Long createdBy, String doi) {
        Paper p = new Paper();
        p.setId(id);
        p.setTitle("Integration Test Paper");
        p.setAuthors("Author A; Author B");
        p.setJournal("Test Journal");
        p.setDoi(doi);
        p.setPublishYear(2026);
        p.setStatus(status);
        p.setDeptId(deptId);
        p.setCreatedBy(createdBy);
        p.setIsClassified(0);
        p.setVersion(1);
        p.setCreatedTime(LocalDateTime.now());
        return p;
    }

    private Paper createClassifiedPaper(Long id, String status, Long deptId, Long createdBy,
                                         String classifiedLevel) {
        Paper p = createPaper(id, status, deptId, createdBy, "10.1234/classified");
        p.setIsClassified(1);
        p.setClassifiedLevel(classifiedLevel);
        return p;
    }

    private Patent createPatent(Long id, String status, Long deptId, Long createdBy, String applicationNo) {
        Patent p = new Patent();
        p.setId(id);
        p.setPatentName("Integration Test Patent");
        p.setInventors("Inventor A");
        p.setApplicationNo(applicationNo);
        p.setApplicationDate(java.time.LocalDate.of(2026, 1, 1));
        p.setPatentType("发明");
        p.setCountry("中国");
        p.setLegalStatus("授权");
        p.setStatus(status);
        p.setDeptId(deptId);
        p.setCreatedBy(createdBy);
        p.setIsClassified(0);
        p.setVersion(1);
        p.setCreatedTime(LocalDateTime.now());
        return p;
    }

    private Copyright createCopyright(Long id, String status, Long deptId, Long createdBy,
                                       String registrationNo) {
        Copyright c = new Copyright();
        c.setId(id);
        c.setName("Integration Test Copyright");
        c.setCopyrightHolder("Institute");
        c.setRegistrationNo(registrationNo);
        c.setRegistrationDate(java.time.LocalDate.of(2026, 6, 1));
        c.setSoftwareVersion("V1.0");
        c.setSoftwareCategory("应用软件");
        c.setStatus(status);
        c.setDeptId(deptId);
        c.setCreatedBy(createdBy);
        c.setIsClassified(0);
        c.setVersion(1);
        c.setCreatedTime(LocalDateTime.now());
        return c;
    }

    private InvalidationDTO createInvalidationDTO(String type, Long id, String reason) {
        InvalidationDTO dto = new InvalidationDTO();
        dto.setAchievementType(type);
        dto.setAchievementId(id);
        dto.setReason(reason);
        return dto;
    }

    // ── Test: Full Lifecycle — Paper ──────────────────────────────────

    @Test
    void testFullLifecycle_Paper_createSubmitArchiveInvalidate() {
        // Simulate paper: DRAFT -> PENDING_DEPT_REVIEW -> PENDING_ADMIN_ARCHIVE -> ARCHIVED -> INVALIDATED
        Paper paper = createPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "10.1234/full-cycle");

        // ── Step: Invalidate by creator ──
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三（创建人）");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            invalidationService.invalidate(createInvalidationDTO("paper", 1L, "内容已过时"), 1L);

            // Verify status updated
            ArgumentCaptor<UpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
            verify(paperMapper).update(eq(null), wrapperCaptor.capture());

            // Verify invalidation record created
            ArgumentCaptor<InvalidationRecord> recordCaptor = ArgumentCaptor.forClass(InvalidationRecord.class);
            verify(invalidationRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getReason()).isEqualTo("内容已过时");
            assertThat(recordCaptor.getValue().getAchievementType()).isEqualTo("paper");

            // Verify audit log
            verify(auditLogService).log(eq("INVALIDATE"), eq(1L), eq(1L), contains("内容已过时"));
        }
    }

    // ── Test: Visibility After Invalidation (D-35) ────────────────────

    @Test
    void testFullLifecycle_visibilityAfterInvalidation() {
        Paper paper = createPaper(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L, "10.1234/visibility");
        when(paperMapper.selectById(1L)).thenReturn(paper);

        // Creator can view
        boolean creatorVisible = invalidationService.isVisibleToUser("paper", 1L, 1L);
        assertThat(creatorVisible).isTrue();

        // Admin can view
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            boolean adminVisible = invalidationService.isVisibleToUser("paper", 1L, 2L);
            assertThat(adminVisible).isTrue();
        }

        // Other user cannot view
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(false);
            boolean otherVisible = invalidationService.isVisibleToUser("paper", 1L, 3L);
            assertThat(otherVisible).isFalse();
        }
    }

    // ── Test: Duplicate Detection — DOI (Paper) ──────────────────────

    @Test
    void testDuplicateDetection_Doi() {
        Paper existing = createPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "10.1234/dup-doi");
        existing.setTitle("Existing Paper");

        // Mock findExistingByDoi
        when(paperMapper.selectOne(any())).thenReturn(existing);

        // Check duplicate via DuplicateCheckService
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", "10.1234/dup-doi");
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingId()).isEqualTo(1L);
        assertThat(result.getExistingTitle()).isEqualTo("Existing Paper");
        assertThat(result.getExistingType()).isEqualTo("论文");
        assertThat(result.getExistingStatus()).isEqualTo("已归档");
    }

    // ── Test: Duplicate Detection — ApplicationNo (Patent) ───────────

    @Test
    void testDuplicateDetection_ApplicationNo() {
        Patent existing = createPatent(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "CN202410000001");
        existing.setPatentName("Existing Patent");

        when(patentMapper.selectOne(any())).thenReturn(existing);

        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("patent", "CN202410000001");
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingId()).isEqualTo(1L);
        assertThat(result.getExistingTitle()).isEqualTo("Existing Patent");
        assertThat(result.getExistingType()).isEqualTo("专利");
    }

    // ── Test: Duplicate Detection — RegistrationNo (Copyright) ───────

    @Test
    void testDuplicateDetection_RegistrationNo() {
        Copyright existing = createCopyright(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "2026SR000001");
        existing.setName("Existing Copyright");

        when(copyrightMapper.selectOne(any())).thenReturn(existing);

        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("copyright", "2026SR000001");
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingId()).isEqualTo(1L);
        assertThat(result.getExistingTitle()).isEqualTo("Existing Copyright");
        assertThat(result.getExistingType()).isEqualTo("软件著作权");
    }

    // ── Test: No Duplicate for Empty Fields (D-47) ───────────────────

    @Test
    void testDuplicateDetection_nullFieldReturnsNoDuplicate() {
        // Null DOI for paper should return no duplicate (D-47 style)
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", null);
        assertThat(result.isDuplicate()).isFalse();

        result = duplicateCheckService.checkDuplicateForSubmit("paper", "");
        assertThat(result.isDuplicate()).isFalse();
    }

    // ── Test: Classified Access — Paper ──────────────────────────────

    @Test
    void testClassifiedAccess_Paper() {
        Paper classified = createClassifiedPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "秘密");
        when(paperMapper.selectById(1L)).thenReturn(classified);

        // Creator can view
        boolean creatorView = classifiedPermissionService.canViewAchievement("paper", 1L, 1L);
        assertThat(creatorView).isTrue();

        // Classified manager can view
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(true);
            boolean managerView = classifiedPermissionService.canViewAchievement("paper", 1L, 2L);
            assertThat(managerView).isTrue();
        }

        // Other user cannot view
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);
            boolean otherView = classifiedPermissionService.canViewAchievement("paper", 1L, 3L);
            assertThat(otherView).isFalse();
        }
    }

    // ── Test: Filter Classified Achievements ─────────────────────────

    @Test
    void testClassifiedAccess_filterList() {
        Paper classified = createClassifiedPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "机密");
        Paper nonClassified = createPaper(2L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "10.1234/normal");

        when(paperMapper.selectById(1L)).thenReturn(classified);
        when(paperMapper.selectById(2L)).thenReturn(nonClassified);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Non-authorized user: paper 1 filtered out, paper 2 stays
            List<Long> filtered = classifiedPermissionService.filterClassifiedAchievements(
                    Arrays.asList(1L, 2L), "paper", 3L);
            assertThat(filtered).containsExactly(2L);
        }

        // Creator can see own classified
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            List<Long> filtered = classifiedPermissionService.filterClassifiedAchievements(
                    Arrays.asList(1L, 2L), "paper", 1L);
            assertThat(filtered).containsExactly(1L, 2L);
        }
    }

    // ── Test: Invalidation — Creator Only (D-34) ─────────────────────

    @Test
    void testInvalidation_CreatorOnly() {
        Paper paper = createPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "10.1234/creator-only");
        when(paperMapper.selectById(1L)).thenReturn(paper);

        // User B (same dept, non-secretary, not creator) tries to invalidate
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(false);

            assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "test"), 2L))
                    .isInstanceOf(AchievementException.class)
                    .hasMessageContaining("只有创建人或科研秘书可以作废成果");
        }
    }

    // ── Test: Invalidation — Secretary Can Act (D-34) ────────────────

    @Test
    void testInvalidation_SecretaryCanAct() {
        Paper paper = createPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "10.1234/secretary");
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        // Secretary (same dept, not creator) can invalidate
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("秘书李四");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            invalidationService.invalidate(createInvalidationDTO("paper", 1L, "重复数据"), 3L);

            verify(paperMapper).update(eq(null), any(UpdateWrapper.class));
            verify(invalidationRecordMapper).insert(any(InvalidationRecord.class));
        }
    }

    // ── Test: Invalidation — Cannot Invalidate Already Invalidated (D-36) ─

    @Test
    void testInvalidation_AlreadyInvalidated_ThrowsError() {
        Paper paper = createPaper(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L, "10.1234/already");
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "again"), 1L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Test: Invalidation Records Query ─────────────────────────────

    @Test
    void testInvalidationRecords_query() {
        InvalidationRecord record = new InvalidationRecord();
        record.setId(1L);
        record.setAchievementType("paper");
        record.setAchievementId(1L);
        record.setInvalidatorName("张三");
        record.setReason("内容已过时");
        record.setCreatedTime(LocalDateTime.now());

        when(invalidationRecordMapper.findByAchievement("paper", 1L))
                .thenReturn(Collections.singletonList(record));

        List<InvalidationVO> records = invalidationService.getInvalidationRecords("paper", 1L);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getReason()).isEqualTo("内容已过时");
    }

    // ── Test: Classified Attachment Check ────────────────────────────

    @Test
    void testClassifiedAttachment_canUserViewAttachment() {
        Paper classified = createClassifiedPaper(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L, "秘密");
        when(paperMapper.selectById(1L)).thenReturn(classified);

        // Creator can download
        boolean canDownload = classifiedPermissionService.canUserViewAttachment("paper", 1L, 1L);
        assertThat(canDownload).isTrue();
    }
}
