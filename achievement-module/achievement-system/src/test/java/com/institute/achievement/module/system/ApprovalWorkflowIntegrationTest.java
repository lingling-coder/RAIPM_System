package com.institute.achievement.module.system;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests for the complete approval workflow across all three
 * achievement types (paper, patent, copyright).
 * <p>
 * Uses mocked persistence layer with Mockito, verifying service orchestration
 * logic for full approval cycles, reject/resubmit, withdrawal, cross-department
 * access control, and notification delivery.
 * <p>
 * E2E smoke test documentation (in code comments):
 * <pre>
 * 1. Start backend with Flyway migrations
 * 2. POST /api/papers -> 201 + id
 * 3. POST /api/approval/submit -> 200, status = PENDING_DEPT_REVIEW
 * 4. POST /api/approval/approve (dept) -> 200, status = PENDING_ADMIN_ARCHIVE
 * 5. POST /api/approval/approve (admin, with archiveNo) -> 200, status = ARCHIVED
 * 6. GET /api/approval/history -> 200, 3 records
 * 7. GET /api/notifications/unread-count -> 200, count > 0
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowIntegrationTest {

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

    // ── Helper Methods ─────────────────────────────────────────────────

    private Paper createPaper(Long id, String status, Long deptId, Long createdBy) {
        Paper p = new Paper();
        p.setId(id);
        p.setTitle("Integration Test Paper");
        p.setAuthors("Author A; Author B");
        p.setJournal("Test Journal");
        p.setDoi("10.1234/test." + id);
        p.setPublishYear(2026);
        p.setStatus(status);
        p.setDeptId(deptId);
        p.setCreatedBy(createdBy);
        p.setVersion(1);
        p.setCreatedTime(LocalDateTime.now());
        return p;
    }

    private Patent createPatent(Long id, String status, Long deptId, Long createdBy) {
        Patent p = new Patent();
        p.setId(id);
        p.setPatentName("Integration Test Patent");
        p.setInventors("Inventor A");
        p.setApplicationNo("CN2026" + id);
        p.setPatentType("发明");
        p.setCountry("中国");
        p.setLegalStatus("授权");
        p.setApplicationDate(java.time.LocalDate.of(2026, 1, 1));
        p.setStatus(status);
        p.setDeptId(deptId);
        p.setCreatedBy(createdBy);
        p.setVersion(1);
        p.setCreatedTime(LocalDateTime.now());
        return p;
    }

    private Copyright createCopyright(Long id, String status, Long deptId, Long createdBy) {
        Copyright c = new Copyright();
        c.setId(id);
        c.setName("Integration Test Copyright");
        c.setCopyrightHolder("Institute");
        c.setRegistrationNo("2026SR" + id);
        c.setSoftwareVersion("V1.0");
        c.setSoftwareCategory("应用软件");
        c.setRegistrationDate(java.time.LocalDate.of(2026, 6, 1));
        c.setStatus(status);
        c.setDeptId(deptId);
        c.setCreatedBy(createdBy);
        c.setVersion(1);
        c.setCreatedTime(LocalDateTime.now());
        return c;
    }

    // ── Paper Full Approval Workflow ───────────────────────────────────

    @Test
    void testPaperFullApprovalWorkflow() {
        // Step 1: Create paper (DRAFT)
        Paper paper = createPaper(1L, AchievementStatusEnum.DRAFT.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Step 2: Submit -> PENDING_DEPT_REVIEW
            approvalService.submit(1L, "paper", 1L);
            verify(approvalRecordMapper).insert(argThat((ApprovalRecord r) -> "SUBMIT".equals(r.getAction())));

            // Step 3: Dept secretary approves -> PENDING_ADMIN_ARCHIVE
            paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);

            approvalService.approve(1L, "paper", 2L, null);
            verify(approvalRecordMapper, times(2)).insert(argThat((ApprovalRecord r) ->
                    "PASS_DEPT".equals(r.getAction()) || "SUBMIT".equals(r.getAction())));

            // Step 4: Admin archives -> ARCHIVED
            paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);

            approvalService.approve(1L, "paper", 3L, "PER-2026-0001");
            verify(approvalRecordMapper, atLeast(3)).insert(any(ApprovalRecord.class));
        }
    }

    // ── Patent Full Approval Workflow ──────────────────────────────────

    @Test
    void testPatentFullApprovalWorkflow() {
        Patent patent = createPatent(2L, AchievementStatusEnum.DRAFT.name(), 10L, 1L);
        when(patentMapper.selectById(2L)).thenReturn(patent);
        when(patentMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Submit
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            approvalService.submit(2L, "patent", 1L);

            // Dept approve
            patent.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            approvalService.approve(2L, "patent", 2L, null);

            // Admin archive
            patent.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            approvalService.approve(2L, "patent", 3L, "PAT-2026-0001");

            verify(approvalRecordMapper, atLeast(3)).insert(any(ApprovalRecord.class));
        }
    }

    // ── Copyright Full Approval Workflow ───────────────────────────────

    @Test
    void testCopyrightFullApprovalWorkflow() {
        Copyright copyright = createCopyright(3L, AchievementStatusEnum.DRAFT.name(), 10L, 1L);
        when(copyrightMapper.selectById(3L)).thenReturn(copyright);
        when(copyrightMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Submit
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            approvalService.submit(3L, "copyright", 1L);

            // Dept approve
            copyright.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            approvalService.approve(3L, "copyright", 2L, null);

            // Admin archive
            copyright.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            approvalService.approve(3L, "copyright", 3L, "CTR-2026-0001");

            verify(approvalRecordMapper, atLeast(3)).insert(any(ApprovalRecord.class));
        }
    }

    // ── Reject Then Resubmit ───────────────────────────────────────────

    @Test
    void testRejectThenResubmit() {
        Paper paper = createPaper(1L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Step 1: Dept rejects
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            approvalService.reject(1L, "paper", 2L, "信息填写不完整");

            verify(notificationService).send(anyLong(), eq("APPROVAL"), eq("您的成果被退回"),
                    contains("信息填写不完整"), anyString(), anyLong());

            // Step 2: Resubmit (full 3-step per D-28)
            paper.setStatus(AchievementStatusEnum.REJECTED.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            paper.setStatus(AchievementStatusEnum.DRAFT.name());
            approvalService.submit(1L, "paper", 1L);

            // Step 3: Dept approves
            paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            approvalService.approve(1L, "paper", 2L, null);

            // Step 4: Admin archives
            paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            approvalService.approve(1L, "paper", 3L, "PER-2026-0002");

            // Verify full flow went through all steps
            verify(approvalRecordMapper, times(4)).insert(any(ApprovalRecord.class));
        }
    }

    // ── Withdraw During Approval ───────────────────────────────────────

    @Test
    void testWithdrawDuringApproval() {
        Paper paper = createPaper(1L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Submitter withdraws
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            approvalService.withdraw(1L, "paper", 1L);

            verify(approvalRecordMapper).insert(argThat((ApprovalRecord r) -> "WITHDRAW".equals(r.getAction())));
        }

        // Non-submitter cannot withdraw
        Paper paper2 = createPaper(2L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(2L)).thenReturn(paper2);
        assertThatThrownBy(() -> approvalService.withdraw(2L, "paper", 2L))
                .isInstanceOf(AchievementException.class);
    }

    // ── Cross-Department Access ────────────────────────────────────────

    @Test
    void testCrossDepartmentAccess() {
        Paper paper = createPaper(1L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // User from dept 20 tries to approve dept 10's achievement
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(20L);

            assertThatThrownBy(() -> approvalService.approve(1L, "paper", 2L, null))
                    .isInstanceOf(AchievementException.class);
        }
    }

    // ── Notification Delivery ──────────────────────────────────────────

    @Test
    void testNotificationDelivery() {
        Paper paper = createPaper(1L, AchievementStatusEnum.DRAFT.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Submit -> dept secretary notified
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);
            approvalService.submit(1L, "paper", 1L);
            verify(notificationService).notifyDeptSecretaries(eq(10L), eq("paper"), eq(1L), anyString());

            // Dept approve -> admin notified
            paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            securityUtils.when(() -> SecurityUtils.hasRole("secretary")).thenReturn(true);
            approvalService.approve(1L, "paper", 2L, null);
            verify(notificationService).notifyAdmin(eq("paper"), eq(1L), anyString());

            // Admin archive -> submitter notified
            paper.setStatus(AchievementStatusEnum.PENDING_ADMIN_ARCHIVE.name());
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("管理员");
            securityUtils.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            approvalService.approve(1L, "paper", 3L, "PER-2026-0001");
            verify(notificationService).send(eq(1L), eq("APPROVAL"), anyString(), anyString(), anyString(), anyLong());
        }
    }

    // ── Approval History Records ───────────────────────────────────────

    @Test
    void testApprovalHistoryRecords() {
        Paper paper = createPaper(1L, AchievementStatusEnum.PENDING_DEPT_REVIEW.name(), 10L, 1L);
        when(paperMapper.selectById(1L)).thenReturn(paper);
        when(paperMapper.update(any(), any())).thenReturn(1);

        when(approvalRecordMapper.findByAchievement("paper", 1L))
                .thenReturn(java.util.Collections.emptyList());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("李四");
            approvalService.reject(1L, "paper", 2L, "格式不符合");

            List<ApprovalRecordVO> history = approvalService.getApprovalHistory("paper", 1L);
            assertThat(history).isEmpty();
        }
    }
}
