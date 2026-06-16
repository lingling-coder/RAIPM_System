package com.institute.achievement.module.system.controller;

import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.ApprovalActionDTO;
import com.institute.achievement.module.system.dto.ApprovalRecordVO;
import com.institute.achievement.module.system.entity.ApprovalRecord;
import com.institute.achievement.module.system.service.ApprovalService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for approval workflow operations.
 * Provides endpoints for submit, approve, reject, withdraw, and queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * Submit an achievement for approval.
     * POST /api/approval/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody ApprovalActionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        approvalService.submit(dto.getAchievementId(), dto.getAchievementType(), userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "提交成功"));
    }

    /**
     * Approve an achievement (dept secretary or admin archive).
     * POST /api/approval/approve
     */
    @PostMapping("/approve")
    public ResponseEntity<Map<String, Object>> approve(@RequestBody ApprovalActionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        approvalService.approve(dto.getAchievementId(), dto.getAchievementType(), userId, dto.getArchiveNo());
        return ResponseEntity.ok(Map.of("code", 200, "message", "审批通过"));
    }

    /**
     * Reject an achievement during approval.
     * POST /api/approval/reject
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, Object>> reject(@RequestBody ApprovalActionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        approvalService.reject(dto.getAchievementId(), dto.getAchievementType(), userId, dto.getComment());
        return ResponseEntity.ok(Map.of("code", 200, "message", "已退回至提交人"));
    }

    /**
     * Withdraw a submitted achievement (submitter only).
     * POST /api/approval/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody ApprovalActionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        approvalService.withdraw(dto.getAchievementId(), dto.getAchievementType(), userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已撤回"));
    }

    /**
     * Get paginated pending approvals.
     * GET /api/approval/pending?page=1&size=20&type=&dateRange=
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateRange) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<ApprovalRecord> result = approvalService.getPendingApprovals(userId, type, dateRange, page, size);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of("records", result.getRecords(), "total", result.getTotal(),
                        "size", result.getSize(), "current", result.getCurrent())
        ));
    }

    /**
     * Get approval history timeline for an achievement.
     * GET /api/approval/history?type=paper&id=1
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam String type,
            @RequestParam Long id) {
        List<ApprovalRecordVO> history = approvalService.getApprovalHistory(type, id);
        return ResponseEntity.ok(Map.of("code", 200, "data", history));
    }
}
