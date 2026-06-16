package com.institute.achievement.framework.audit;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for audit log queries (read-only).
 * <p>
 * This controller provides paginated search, detail view, and hash chain
 * integrity verification. It enforces read-only access -- no endpoints
 * for creating, updating, or deleting audit log entries.
 * <p>
 * Access is restricted to SYSTEM_ADMIN and AUDITOR roles.
 */
@Tag(name = "审计日志", description = "审计日志查询与哈希链校验（只读）")
@RestController
@RequestMapping("/api/system/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志")
    @PostMapping("/page")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN') or hasRole('ROLE_AUDITOR')")
    public Result<PageResult<AuditLogVO>> page(@RequestBody AuditLogPageDTO dto) {
        PageResult<AuditLogVO> result = auditLogService.page(dto);
        return Result.success(result);
    }

    @Operation(summary = "获取审计日志详情（含哈希校验）")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN') or hasRole('ROLE_AUDITOR')")
    public Result<AuditLogVO> getDetail(@PathVariable Long id) {
        AuditLogVO vo = auditLogService.getDetail(id);
        if (vo == null) {
            return Result.notFound("审计日志不存在");
        }
        return Result.success(vo);
    }

    @Operation(summary = "校验哈希链完整性")
    @PostMapping("/verify-chain")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN') or hasRole('ROLE_AUDITOR')")
    public Result<ChainVerificationResult> verifyChain(@RequestBody ChainVerifyRequest request) {
        ChainVerificationResult result = auditLogService.verifyChain(
                request.getFromId(), request.getToId());
        return Result.success(result);
    }

    @Operation(summary = "获取操作类型统计")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN') or hasRole('ROLE_AUDITOR')")
    public Result<java.util.Map<String, Long>> getStats() {
        java.util.Map<String, Long> stats = auditLogService.getOperationTypeStats(null, null);
        return Result.success(stats);
    }

    /**
     * Request DTO for chain verification.
     */
    @io.swagger.v3.oas.annotations.media.Schema(description = "哈希链校验请求")
    public static class ChainVerifyRequest {
        private Long fromId;
        private Long toId;

        public Long getFromId() {
            return fromId;
        }

        public void setFromId(Long fromId) {
            this.fromId = fromId;
        }

        public Long getToId() {
            return toId;
        }

        public void setToId(Long toId) {
            this.toId = toId;
        }
    }
}
