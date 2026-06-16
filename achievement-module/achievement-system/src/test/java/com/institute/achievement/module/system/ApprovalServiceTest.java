package com.institute.achievement.module.system;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.ApprovalRecordVO;
import com.institute.achievement.module.system.entity.ApprovalRecord;
import com.institute.achievement.module.system.mapper.ApprovalRecordMapper;
import com.institute.achievement.module.system.service.ApprovalService;
import com.institute.achievement.module.system.service.AuditLogService;
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
 * Unit tests for ApprovalService covering state machine transitions,
 * authorization checks, and notification triggers.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private PaperMapper paperMapper;
    @Mock
    private PatentMapper patentMapper;
    @Mock
    private CopyrightMapper copyrightMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(paperMapper, patentMapper, copyrightMapper,
                approvalRecordMapper, notificationService, auditLogService);
    }

    private Paper createDraftPaper(Long id, Long deptId, Long createdBy) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setTitle("Test Paper");
        paper.setAuthors("Author A");
        paper.setStatus(AchievementStatusEnum.DRAFT.name());
        paper.setDeptId(deptId);
        paper.setCreatedBy(createdBy);
        paper.setVersion(1);
        paper.setCreatedTime(LocalDateTime.now());
        return paper;
    }

    // ── Submit Tests ──────────────────────────────────────────────────────

    @Test
    void testSubmitPaper_shouldTransitionToPendingDeptReview() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            approvalService.submit(1L, "paper", 1L);

            ArgumentCaptor<ApprovalRecord> recordCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getAction()).isEqualTo("SUBMIT");
            assertThat(recordCaptor.getValue().getFromStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
            assertThat(recordCaptor.getValue().getToStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());

            verify(auditLogService).log(eq("SUBMIT"), eq(1L), eq(1L), anyString());
            verify(notificationService).notifyDeptSecretaries(eq(10L), eq("paper"), eq(1L), anyString());
        }
    }

    @Test
    void testSubmitNonDraft_shouldThrowInvalidTransition() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.submit(1L, "paper", 1L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Approve Tests (Dept Secretary) ────────────────────────────────────

    @Test
    void testDeptApprove_shouldTransitionToPendingAdminArchive() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            approvalService.approve(1L, "paper", 2L, null);

            ArgumentCaptor<ApprovalRecord> recordCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getAction()).isEqualTo("PASS_DEPT");
            assertThat(recordCaptor.getValue().getFromStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            assertThat(recordCaptor.getValue().getToStatus()).isEqualTo(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());

            verify(notificationService).notifyAdmin(eq("paper"), eq(1L), anyString());
        }
    }

    @Test
    void testDeptApprove_wrongDepartment_shouldThrowNotAuthorized() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(20L);

            assertThatThrownBy(() -> approvalService.approve(1L, "paper", 2L, null))
                    .isInstanceOf(AchievementException.class);
        }
    }

    // ── Approve Tests (Admin Archive) ────────────────────────────────────

    @Test
    void testAdminArchive_shouldTransitionToArchived() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);

            approvalService.approve(1L, "paper", 3L, "PER-2026-0001");

            ArgumentCaptor<ApprovalRecord> recordCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getAction()).isEqualTo("PASS_ADMIN");
            assertThat(recordCaptor.getValue().getFromStatus()).isEqualTo(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            assertThat(recordCaptor.getValue().getToStatus()).isEqualTo(AchievementStatusEnum.ARCHIVED.name());

            verify(notificationService).send(anyLong(), eq("APPROVAL"), anyString(), anyString(), anyString(), anyLong());
        }
    }

    @Test
    void testNonAdminArchive_shouldThrowNotAuthorized() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(false);

            assertThatThrownBy(() -> approvalService.approve(1L, "paper", 2L, null))
                    .isInstanceOf(AchievementException.class);
        }
    }

    // ── Reject Tests ─────────────────────────────────────────────────────

    @Test
    void testRejectPendingDeptReview_shouldTransitionToRejected() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");

            approvalService.reject(1L, "paper", 2L, "信息填写不完整");

            ArgumentCaptor<ApprovalRecord> recordCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getAction()).isEqualTo("REJECT_DEPT");
            assertThat(recordCaptor.getValue().getFromStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            assertThat(recordCaptor.getValue().getToStatus()).isEqualTo(AchievementStatusEnum.REJECTED.name());
            assertThat(recordCaptor.getValue().getComment()).isEqualTo("信息填写不完整");

            verify(notificationService).send(anyLong(), eq("APPROVAL"), eq("您的成果被退回"),
                    contains("信息填写不完整"), anyString(), anyLong());
        }
    }

    @Test
    void testRejectWithoutReason_shouldThrow() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.reject(1L, "paper", 2L, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> approvalService.reject(1L, "paper", 2L, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Withdraw Tests ───────────────────────────────────────────────────

    @Test
    void testWithdrawAsSubmitter_shouldTransitionToWithdrawn() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");

            approvalService.withdraw(1L, "paper", 1L);

            ArgumentCaptor<ApprovalRecord> recordCaptor = ArgumentCaptor.forClass(ApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getAction()).isEqualTo("WITHDRAW");
            assertThat(recordCaptor.getValue().getToStatus()).isEqualTo(AchievementStatusEnum.WITHDRAWN.name());
        }
    }

    @Test
    void testWithdrawAsNonSubmitter_shouldThrowNotAuthorized() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.withdraw(1L, "paper", 2L))
                .isInstanceOf(AchievementException.class)
                .hasMessageContaining("只有提交人可以撤回");
    }

    @Test
    void testWithdrawFromArchived_shouldThrowInvalidTransition() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.ARCHIVED.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.withdraw(1L, "paper", 1L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Full Workflow Tests ──────────────────────────────────────────────

    @Test
    void testFullWorkflow_paperSubmitDeptApproveAdminArchive() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            approvalService.submit(1L, "paper", 1L);
            verify(approvalRecordMapper, times(1)).insert(any(ApprovalRecord.class));

            // Dept approve
            paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            approvalService.approve(1L, "paper", 2L, null);
            verify(approvalRecordMapper, times(2)).insert(any(ApprovalRecord.class));

            // Admin archive
            paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);

            approvalService.approve(1L, "paper", 3L, "PER-2026-0001");
            verify(approvalRecordMapper, times(3)).insert(any(ApprovalRecord.class));
        }
    }

    @Test
    void testRejectThenResubmit_paperFull3StepFlow() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");

            // Reject
            approvalService.reject(1L, "paper", 2L, "信息填写不完整");
            verify(approvalRecordMapper, times(1)).insert(any(ApprovalRecord.class));

            // Resubmit
            paper.setStatus(AchievementStatusEnum.DRAFT.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            approvalService.submit(1L, "paper", 1L);

            // Dept approve again
            paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            approvalService.approve(1L, "paper", 2L, null);

            // Admin archive
            paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);

            approvalService.approve(1L, "paper", 3L, "PER-2026-0002");
            verify(approvalRecordMapper, times(4)).insert(any(ApprovalRecord.class));
        }
    }

    // ── History Tests ────────────────────────────────────────────────────

    @Test
    void testGetApprovalHistory_shouldReturnOrderedRecords() {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(1L);
        record.setAction("SUBMIT");
        record.setOperatorName("张三");
        record.setFromStatus(AchievementStatusEnum.DRAFT.name());
        record.setToStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        record.setCreatedTime(LocalDateTime.now());

        when(approvalRecordMapper.findByAchievement("paper", 1L))
                .thenReturn(Collections.singletonList(record));

        List<ApprovalRecordVO> history = approvalService.getApprovalHistory("paper", 1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo("SUBMIT");
        assertThat(history.get(0).getActionLabel()).isEqualTo("提交审批");
    }

    // ── Invalid State Transition Tests ──────────────────────────────────

    @Test
    void testApproveDraft_shouldThrowInvalidTransition() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.DRAFT.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.approve(1L, "paper", 2L, null))
                .isInstanceOf(AchievementException.class);
    }

    @Test
    void testRejectArchived_shouldThrowInvalidTransition() {
        Paper paper = createDraftPaper(1L, 10L, 1L);
        paper.setStatus(AchievementStatusEnum.ARCHIVED.name());
        when(paperMapper.selectById(1L)).thenReturn(paper);

        assertThatThrownBy(() -> approvalService.reject(1L, "paper", 2L, "reason"))
                .isInstanceOf(AchievementException.class);
    }

    // ── Patent Workflow ─────────────────────────────────────────────────

    @Test
    void testPatentFullWorkflow_shouldComplete() {
        Patent patent = new Patent();
        patent.setId(2L);
        patent.setPatentName("Test Patent");
        patent.setStatus(AchievementStatusEnum.DRAFT.name());
        patent.setCreatedBy(1L);
        patent.setDeptId(10L);
        patent.setVersion(1);

        when(patentMapper.selectById(2L)).thenReturn(patent);
        when(patentMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            approvalService.submit(2L, "patent", 1L);
            verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
        }
    }

    // ── Copyright Workflow ──────────────────────────────────────────────

    @Test
    void testCopyrightFullWorkflow_shouldComplete() {
        Copyright copyright = new Copyright();
        copyright.setId(3L);
        copyright.setName("Test Copyright");
        copyright.setStatus(AchievementStatusEnum.DRAFT.name());
        copyright.setCreatedBy(1L);
        copyright.setDeptId(10L);
        copyright.setVersion(1);

        when(copyrightMapper.selectById(3L)).thenReturn(copyright);
        when(copyrightMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            approvalService.submit(3L, "copyright", 1L);
            verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
        }
    }
}
