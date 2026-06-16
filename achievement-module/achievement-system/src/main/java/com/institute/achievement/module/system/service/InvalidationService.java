package com.institute.achievement.module.system.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.InvalidationDTO;
import com.institute.achievement.module.system.dto.InvalidationVO;
import com.institute.achievement.module.system.entity.InvalidationRecord;
import com.institute.achievement.module.system.mapper.InvalidationRecordMapper;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Achievement invalidation service implementing D-34~D-36.
 * <p>
 * Handles the end-of-life transition for archived achievements:
 * <ul>
 *   <li>D-34: Creator or dept secretary can directly invalidate (no approval needed)</li>
 *   <li>D-35: After invalidation, only creator and admin can see the achievement</li>
 *   <li>D-36: Invalidation is irreversible</li>
 * </ul>
 * <p>
 * Threat mitigations:
 * <ul>
 *   <li>T-01-16 (DoS): Only creator/dept secretary can invalidate; audit logged</li>
 *   <li>T-01-20 (Repudiation): InvalidationRecord + AuditLog in same @Transactional</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvalidationService {

    private final PaperMapper paperMapper;
    private final PatentMapper patentMapper;
    private final CopyrightMapper copyrightMapper;
    private final InvalidationRecordMapper invalidationRecordMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    // ── Invalidate (D-34) ──────────────────────────────────────────────

    /**
     * Invalidate an archived achievement.
     * <p>
     * Transitions status from ARCHIVED to INVALIDATED, creates an
     * InvalidationRecord with reason, logs to audit, and notifies admin.
     * <p>
     * Enforcement:
     * <ul>
     *   <li>Achievement must be in ARCHIVED status (D-36: irreversible)</li>
     *   <li>Only creator or same-department secretary can invalidate (D-34)</li>
     *   <li>Reason is required (audit trail per T-01-20)</li>
     * </ul>
     *
     * @param dto    the invalidation request
     * @param userId the requesting user's ID
     */
    @Transactional
    public void invalidate(InvalidationDTO dto, Long userId) {
        String type = dto.getAchievementType();
        Long achievementId = dto.getAchievementId();
        String reason = dto.getReason();

        // Load achievement and validate status
        String currentStatus = getStatus(type, achievementId);

        // D-36: Only ARCHIVED can be invalidated
        if (!AchievementStatusEnum.ARCHIVED.name().equals(currentStatus)) {
            throw AchievementException.invalidTransition(currentStatus, "invalidate");
        }

        // D-34: Only creator or same-department secretary can invalidate
        Long createdBy = getCreatedBy(type, achievementId);
        Long deptId = getDeptId(type, achievementId);
        if (!createdBy.equals(userId)) {
            // Not the creator — check if user is a dept secretary in the same department
            validateDeptSecretary(userId, deptId);
        }

        // D-36: Transition to INVALIDATED
        updateStatus(type, achievementId, AchievementStatusEnum.INVALIDATED.name());

        // Create invalidation record (T-01-20)
        InvalidationRecord record = new InvalidationRecord();
        record.setAchievementType(type);
        record.setAchievementId(achievementId);
        record.setInvalidatorId(userId);
        record.setInvalidatorName(SecurityUtils.getCurrentUsername());
        record.setReason(reason);
        record.setCreatedTime(LocalDateTime.now());
        invalidationRecordMapper.insert(record);

        // Audit log (T-01-20)
        auditLogService.log("INVALIDATE", achievementId, userId,
                "作废: " + type + " id=" + achievementId + ", reason=" + reason);

        // Notify system admin
        String title = getAchievementTitle(type, achievementId);
        notificationService.notifyAdmin(type, achievementId, title);

        log.info("Achievement invalidated: type={}, id={}, userId={}, reason='{}'",
                type, achievementId, userId, reason);
    }

    // ── Visibility (D-35) ──────────────────────────────────────────────

    /**
     * Check if an achievement is visible to a specific user.
     * <p>
     * D-35: After invalidation, only creator and system admin can see.
     * Non-invalidated achievements are always visible (dept-level filtering
     * handled by SQL permission interceptor).
     *
     * @param achievementType paper/patent/copyright
     * @param achievementId   achievement ID
     * @param userId          user ID to check
     * @return true if the user can view this achievement
     */
    public boolean isVisibleToUser(String achievementType, Long achievementId, Long userId) {
        String status = getStatus(achievementType, achievementId);

        // Non-invalidated: always visible at service level
        if (!AchievementStatusEnum.INVALIDATED.name().equals(status)) {
            return true;
        }

        // D-35: INVALIDATED — only creator or admin can see
        Long createdBy = getCreatedBy(achievementType, achievementId);
        if (createdBy != null && createdBy.equals(userId)) {
            return true;
        }

        // Check admin role
        return SecurityUtils.hasRole("admin");
    }

    // ── Query Records ────────────────────────────────────────────────

    /**
     * Get invalidation records for a specific achievement.
     */
    public List<InvalidationVO> getInvalidationRecords(String achievementType, Long achievementId) {
        List<InvalidationRecord> records = invalidationRecordMapper.findByAchievement(
                achievementType, achievementId);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::mapToVO).collect(Collectors.toList());
    }

    /**
     * Get all invalidation records for the current user or all (admin).
     */
    public List<InvalidationVO> getAllInvalidations(Long userId, boolean isAdmin) {
        List<InvalidationRecord> records;
        if (isAdmin) {
            // Admin sees all — use selectList with no conditions
            records = invalidationRecordMapper.selectList(null);
        } else {
            // Others see their own
            records = invalidationRecordMapper.findByInvalidator(userId);
        }
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::mapToVO).collect(Collectors.toList());
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    private String getStatus(String type, Long id) {
        return switch (type.toLowerCase()) {
            case "paper" -> {
                Paper p = paperMapper.selectById(id);
                if (p == null) throw AchievementException.notFound("论文", id);
                yield p.getStatus();
            }
            case "patent" -> {
                Patent p = patentMapper.selectById(id);
                if (p == null) throw AchievementException.notFound("专利", id);
                yield p.getStatus();
            }
            case "copyright" -> {
                Copyright c = copyrightMapper.selectById(id);
                if (c == null) throw AchievementException.notFound("软件著作权", id);
                yield c.getStatus();
            }
            default -> throw new IllegalArgumentException("未知成果类型: " + type);
        };
    }

    private Long getCreatedBy(String type, Long id) {
        return switch (type.toLowerCase()) {
            case "paper" -> {
                Paper p = paperMapper.selectById(id);
                yield p != null ? p.getCreatedBy() : null;
            }
            case "patent" -> {
                Patent p = patentMapper.selectById(id);
                yield p != null ? p.getCreatedBy() : null;
            }
            case "copyright" -> {
                Copyright c = copyrightMapper.selectById(id);
                yield c != null ? c.getCreatedBy() : null;
            }
            default -> null;
        };
    }

    private Long getDeptId(String type, Long id) {
        return switch (type.toLowerCase()) {
            case "paper" -> {
                Paper p = paperMapper.selectById(id);
                yield p != null ? p.getDeptId() : null;
            }
            case "patent" -> {
                Patent p = patentMapper.selectById(id);
                yield p != null ? p.getDeptId() : null;
            }
            case "copyright" -> {
                Copyright c = copyrightMapper.selectById(id);
                yield c != null ? c.getDeptId() : null;
            }
            default -> null;
        };
    }

    private String getAchievementTitle(String type, Long id) {
        return switch (type.toLowerCase()) {
            case "paper" -> {
                Paper p = paperMapper.selectById(id);
                yield p != null ? p.getTitle() : "未知";
            }
            case "patent" -> {
                Patent p = patentMapper.selectById(id);
                yield p != null ? p.getPatentName() : "未知";
            }
            case "copyright" -> {
                Copyright c = copyrightMapper.selectById(id);
                yield c != null ? c.getName() : "未知";
            }
            default -> "未知";
        };
    }

    private void updateStatus(String type, Long id, String newStatus) {
        int updated = switch (type.toLowerCase()) {
            case "paper" -> paperMapper.update(null,
                    new UpdateWrapper<Paper>()
                            .eq("id", id)
                            .set("status", newStatus)
                            .set("updated_by", SecurityUtils.getCurrentUserId())
                            .set("updated_time", LocalDateTime.now()));
            case "patent" -> patentMapper.update(null,
                    new UpdateWrapper<Patent>()
                            .eq("id", id)
                            .set("status", newStatus)
                            .set("updated_by", SecurityUtils.getCurrentUserId())
                            .set("updated_time", LocalDateTime.now()));
            case "copyright" -> copyrightMapper.update(null,
                    new UpdateWrapper<Copyright>()
                            .eq("id", id)
                            .set("status", newStatus)
                            .set("updated_by", SecurityUtils.getCurrentUserId())
                            .set("updated_time", LocalDateTime.now()));
            default -> throw new IllegalArgumentException("未知成果类型: " + type);
        };

        if (updated == 0) {
            throw new IllegalStateException("状态更新失败，记录可能已被修改（乐观锁冲突）");
        }
    }

    private void validateDeptSecretary(Long userId, Long deptId) {
        if (!SecurityUtils.hasRole("secretary")) {
            throw AchievementException.notAuthorized("只有创建人或科研秘书可以作废成果（D-34）");
        }
        Long userDeptId = SecurityUtils.getCurrentDeptId();
        if (userDeptId == null || !userDeptId.equals(deptId)) {
            throw AchievementException.notAuthorized("只能作废本部门的成果（D-34）");
        }
    }

    private InvalidationVO mapToVO(InvalidationRecord record) {
        InvalidationVO vo = new InvalidationVO();
        vo.setId(record.getId());
        vo.setAchievementType(record.getAchievementType());
        vo.setAchievementId(record.getAchievementId());
        vo.setInvalidatorName(record.getInvalidatorName());
        vo.setReason(record.getReason());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }
}
