package com.institute.achievement.module.system;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.InvalidationDTO;
import com.institute.achievement.module.system.dto.InvalidationVO;
import com.institute.achievement.module.system.entity.InvalidationRecord;
import com.institute.achievement.module.system.mapper.InvalidationRecordMapper;
import com.institute.achievement.module.system.service.AuditLogService;
import com.institute.achievement.module.system.service.InvalidationService;
import com.institute.achievement.module.system.service.NotificationService;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvalidationService covering invalidation lifecycle,
 * permission enforcement, visibility control, and audit logging.
 * <p>
 * Implements D-34 (creator/secretary can invalidate), D-35 (hidden from
 * non-creator/admin), D-36 (irreversible), and T-01-16/20.
 */
@ExtendWith(MockitoExtension.class)
class InvalidationServiceTest {

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

    private InvalidationService invalidationService;

    @BeforeEach
    void setUp() {
        invalidationService = new InvalidationService(paperMapper, patentMapper, copyrightMapper,
                invalidationRecordMapper, auditLogService, notificationService);
    }

    // ── Helper Methods ─────────────────────────────────────────────────

    private Paper createArchivedPaper(Long id, Long deptId, Long createdBy) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setTitle("Test Paper");
        paper.setAuthors("Author A");
        paper.setStatus(AchievementStatusEnum.ARCHIVED.name());
        paper.setDeptId(deptId);
        paper.setCreatedBy(createdBy);
        paper.setVersion(1);
        paper.setCreatedTime(LocalDateTime.now());
        return paper;
    }

    private Paper createPaperWithStatus(Long id, String status, Long deptId, Long createdBy) {
        Paper paper = createArchivedPaper(id, deptId, createdBy);
        paper.setStatus(status);
        return paper;
    }

    private Patent createArchivedPatent(Long id, Long deptId, Long createdBy) {
        Patent patent = new Patent();
        patent.setId(id);
        patent.setPatentName("Test Patent");
        patent.setStatus(AchievementStatusEnum.ARCHIVED.name());
        patent.setDeptId(deptId);
        patent.setCreatedBy(createdBy);
        patent.setVersion(1);
        patent.setCreatedTime(LocalDateTime.now());
        return patent;
    }

    private Copyright createArchivedCopyright(Long id, Long deptId, Long createdBy) {
        Copyright copyright = new Copyright();
        copyright.setId(id);
        copyright.setName("Test Copyright");
        copyright.setStatus(AchievementStatusEnum.ARCHIVED.name());
        copyright.setDeptId(deptId);
        copyright.setCreatedBy(createdBy);
        copyright.setVersion(1);
        copyright.setCreatedTime(LocalDateTime.now());
        return copyright;
    }

    private InvalidationDTO createInvalidationDTO(String type, Long id, String reason) {
        InvalidationDTO dto = new InvalidationDTO();
        dto.setAchievementType(type);
        dto.setAchievementId(id);
        dto.setReason(reason);
        return dto;
    }

    // ── Invalidate Archived Paper (Full Cycle) ─────────────────────────

    @Test
    void testInvalidateArchivedPaper_shouldTransitionToInvalidated() {
        Paper paper = createArchivedPaper(1L, 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Creator invalidates own achievement
            invalidationService.invalidate(createInvalidationDTO("paper", 1L, "内容已过时"), 1L);

            // Verify status transition
            ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
            verify(paperMapper).updateById(paperCaptor.capture());
            assertThat(paperCaptor.getValue().getStatus()).isEqualTo(AchievementStatusEnum.INVALIDATED.name());

            // Verify invalidation record created
            ArgumentCaptor<InvalidationRecord> recordCaptor = ArgumentCaptor.forClass(InvalidationRecord.class);
            verify(invalidationRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getReason()).isEqualTo("内容已过时");
            assertThat(recordCaptor.getValue().getAchievementType()).isEqualTo("paper");
            assertThat(recordCaptor.getValue().getAchievementId()).isEqualTo(1L);

            // Verify audit log
            verify(auditLogService).log(eq("INVALIDATE"), eq(1L), eq(1L), contains("内容已过时"));

            // Verify admin notified
            verify(notificationService).notifyAdmin(eq("paper"), eq(1L), anyString());
        }
    }

    // ── Invalidate by Creator ─────────────────────────────────────────

    @Test
    void testInvalidateByCreator_shouldSucceed() {
        Paper paper = createArchivedPaper(1L, 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");

            // Creator (userId=1) invalidates own paper (createdBy=1)
            invalidationService.invalidate(createInvalidationDTO("paper", 1L, "不再需要"), 1L);

            verify(paperMapper).updateById(any(Paper.class));
        }
    }

    // ── Invalidate by Secretary ───────────────────────────────────────

    @Test
    void testInvalidateBySecretary_shouldSucceed() {
        Paper paper = createArchivedPaper(1L, 10L, 2L); // created by user 2
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            // Secretary (userId=3, same dept) invalidates
            invalidationService.invalidate(createInvalidationDTO("paper", 1L, "重复数据"), 3L);

            verify(paperMapper).updateById(any(Paper.class));
        }
    }

    // ── Non-Archived Achievements Cannot Be Invalidated ───────────────

    @Test
    void testInvalidateNonArchived_shouldThrowInvalidTransition() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.DRAFT.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "test"), 1L))
                .isInstanceOf(AchievementException.class);
    }

    @Test
    void testInvalidatePendingReview_shouldThrowInvalidTransition() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "test"), 1L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Unauthorized User Cannot Invalidate ───────────────────────────

    @Test
    void testInvalidateByUnauthorized_shouldThrowNotAuthorized() {
        Paper paper = createArchivedPaper(1L, 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            // Not the creator and not a secretary
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(false);

            assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "test"), 2L))
                    .isInstanceOf(AchievementException.class)
                    .hasMessageContaining("只有创建人或科研秘书可以作废成果");
        }
    }

    // ── Invalidation Is Irreversible ──────────────────────────────────

    @Test
    void testInvalidationIrreversible_shouldThrowForAlreadyInvalidated() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> invalidationService.invalidate(createInvalidationDTO("paper", 1L, "test"), 1L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Visibility After Invalidation (D-35) ──────────────────────────

    @Test
    void testVisibilityAfterInvalidation_creatorCanSee() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        // Creator can view
        boolean visible = invalidationService.isVisibleToUser("paper", 1L, 1L);
        assertThat(visible).isTrue();
    }

    @Test
    void testVisibilityAfterInvalidation_adminCanSee() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);

            boolean visible = invalidationService.isVisibleToUser("paper", 1L, 2L);
            assertThat(visible).isTrue();
        }
    }

    @Test
    void testVisibilityAfterInvalidation_otherUserCannotSee() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.INVALIDATED.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(false);

            boolean visible = invalidationService.isVisibleToUser("paper", 1L, 3L);
            assertThat(visible).isFalse();
        }
    }

    @Test
    void testVisibilityForNonInvalidated_alwaysVisible() {
        Paper paper = createPaperWithStatus(1L, AchievementStatusEnum.ARCHIVED.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        // Non-invalidated: always visible
        boolean visible = invalidationService.isVisibleToUser("paper", 1L, 99L);
        assertThat(visible).isTrue();
    }

    // ── Invalidate All Types ──────────────────────────────────────────

    @Test
    void testInvalidatePatent_shouldSucceed() {
        Patent patent = createArchivedPatent(2L, 10L, 1L);
        when(patentMapper.selectById(2L)).thenReturn(patent);
        when(patentMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");

            invalidationService.invalidate(createInvalidationDTO("patent", 2L, "专利过期"), 1L);

            verify(patentMapper).updateById(any(Patent.class));
            verify(invalidationRecordMapper).insert(any(InvalidationRecord.class));
        }
    }

    @Test
    void testInvalidateCopyright_shouldSucceed() {
        Copyright copyright = createArchivedCopyright(3L, 10L, 1L);
        when(copyrightMapper.selectById(3L)).thenReturn(copyright);
        when(copyrightMapper.update(any(), any())).thenReturn(1);
        when(invalidationRecordMapper.insert(any(InvalidationRecord.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");

            invalidationService.invalidate(createInvalidationDTO("copyright", 3L, "软著已升级"), 1L);

            verify(copyrightMapper).updateById(any(Copyright.class));
            verify(invalidationRecordMapper).insert(any(InvalidationRecord.class));
        }
    }

    // ── Get Invalidation Records ──────────────────────────────────────

    @Test
    void testGetInvalidationRecords_shouldReturnRecords() {
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
        assertThat(records.get(0).getInvalidatorName()).isEqualTo("张三");
    }
}
