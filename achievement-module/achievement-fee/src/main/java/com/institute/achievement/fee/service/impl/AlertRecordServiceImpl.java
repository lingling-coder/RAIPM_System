package com.institute.achievement.fee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.fee.dto.AlertQueryDTO;
import com.institute.achievement.fee.dto.AlertRecordVO;
import com.institute.achievement.fee.entity.AlertRecord;
import com.institute.achievement.fee.entity.FeeRecord;
import com.institute.achievement.fee.enums.AlertLevelEnum;
import com.institute.achievement.fee.enums.EscalationLevel;
import com.institute.achievement.fee.enums.FeeTypeEnum;
import com.institute.achievement.fee.mapper.AlertRecordMapper;
import com.institute.achievement.fee.mapper.FeeRecordMapper;
import com.institute.achievement.fee.service.AlertRecordService;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.service.NotificationService;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alert record service implementation.
 * <p>
 * Implements the 4-tier alert engine: daily scan classifies pending fee
 * records by due date proximity (BLUE=30d, YELLOW=15d, ORANGE=7d, RED=overdue),
 * persists alert records with dedup, and sends in-app notifications.
 *
 * <h3>Threat model mitigations</h3>
 * <ul>
 *   <li>T-02-03-01: UNIQUE INDEX idx_unique_alert + batch dedup check prevents duplicates</li>
 *   <li>T-02-03-02: Redis idempotency key + distributed lock (handled in AlertScanTask)</li>
 *   <li>T-02-03-03: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordMapper alertRecordMapper;
    private final FeeRecordMapper feeRecordMapper;
    private final PatentMapper patentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public int scanAndGenerateAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate scanWindowEnd = today.plusDays(30);

        // Step 1: Query all pending fee_records within the 30-day scan window
        List<FeeRecord> pendingRecords = feeRecordMapper.selectList(
                new LambdaQueryWrapper<FeeRecord>()
                        .eq(FeeRecord::getStatus, "pending")
                        .le(FeeRecord::getDueDate, scanWindowEnd));

        if (pendingRecords.isEmpty()) {
            log.debug("Alert scan: no pending fee records found for date {}", today);
            return 0;
        }

        // Step 2: Batch dedup — find fee_record_ids that already have alerts for today
        Set<Long> alreadyAlertedIds = new HashSet<>(alertRecordMapper.findAlreadyAlertedFeeRecordIds(today));

        // Step 3: Filter and classify fee records that need new alerts
        List<FeeRecord> recordsToAlert = pendingRecords.stream()
                .filter(fr -> !alreadyAlertedIds.contains(fr.getId()))
                .filter(fr -> AlertLevelEnum.fromDueDate(fr.getDueDate()) != null)
                .collect(Collectors.toList());

        if (recordsToAlert.isEmpty()) {
            log.info("Alert scan for {}: {} pending records, all already have alerts for today",
                    today, pendingRecords.size());
            return 0;
        }

        // Step 4: Generate alerts with notifications
        int newAlertCount = 0;
        int skippedCount = pendingRecords.size() - recordsToAlert.size();

        for (FeeRecord feeRecord : recordsToAlert) {
            try {
                AlertLevelEnum level = AlertLevelEnum.fromDueDate(feeRecord.getDueDate());
                if (level == null) {
                    continue; // Should not happen due to filter above
                }

                // Double-check dedup at DB level (T-02-03-01)
                int existingCount = alertRecordMapper.countByFeeRecordAndLevelAndDate(
                        feeRecord.getId(), level.getCode(), today);
                if (existingCount > 0) {
                    skippedCount++;
                    continue;
                }

                // Insert alert record
                AlertRecord alert = new AlertRecord();
                alert.setFeeRecordId(feeRecord.getId());
                alert.setAlertLevel(level.getCode());
                alert.setTriggeredDate(today);
                alert.setTriggeredAt(LocalDateTime.now());
                alert.setStatus("pending");
                alert.setEscalationLevel("NONE");
                alert.setDeptId(feeRecord.getDeptId());
                alertRecordMapper.insert(alert);

                // Determine the user to notify
                Long notifyUserId = resolveNotifyUserId(feeRecord);

                // Build notification title and content
                String feeTypeLabel = resolveFeeTypeLabel(feeRecord.getFeeType());
                long daysUntilDue = ChronoUnit.DAYS.between(today, feeRecord.getDueDate());
                String ownerName = resolveOwnerName(feeRecord);

                String title = "费用预警 — " + ownerName;
                String content;
                if (AlertLevelEnum.RED.equals(level)) {
                    // Overdue case
                    content = String.format(
                            "「%s」已于 %s 到期（已逾期%d天），金额 ¥%s。请立即处理。",
                            feeTypeLabel, feeRecord.getDueDate(), Math.abs(daysUntilDue),
                            feeRecord.getAmount() != null ? feeRecord.getAmount().toPlainString() : "0");
                } else {
                    content = String.format(
                            "「%s」将于 %s 到期（%d天后），金额 ¥%s。请尽快安排缴费。",
                            feeTypeLabel, feeRecord.getDueDate(), daysUntilDue,
                            feeRecord.getAmount() != null ? feeRecord.getAmount().toPlainString() : "0");
                }

                // Send in-app notification (D-25)
                notificationService.send(notifyUserId, "ALERT", title, content,
                        "fee", feeRecord.getId());

                newAlertCount++;
                log.debug("Alert generated: feeRecordId={}, level={}, userId={}",
                        feeRecord.getId(), level.getCode(), notifyUserId);

            } catch (Exception e) {
                log.error("Failed to generate alert for feeRecord id={}: {}",
                        feeRecord.getId(), e.getMessage());
                // Continue with next fee record — partial failures are acceptable
                skippedCount++;
            }
        }

        log.info("Alert scan completed for {}: {} alerts generated, {} skipped (already alerted or failed)",
                today, newAlertCount, skippedCount);
        return newAlertCount;
    }

    @Override
    public Page<AlertRecordVO> page(int page, int size, AlertQueryDTO query) {
        // Inject data isolation filter (T-02-03-03)
        if (query == null) {
            query = new AlertQueryDTO();
        }
        if (query.getDeptId() == null) {
            query.setDeptId(SecurityUtils.getCurrentDeptId());
        }

        Page<AlertRecordVO> pageParam = new Page<>(page, size);
        return alertRecordMapper.selectAlertPage(pageParam, query);
    }

    @Override
    public AlertRecordVO getById(Long id) {
        // Direct ID lookup -- do NOT use paginated query which only returns
        // the single most recent alert (LIMIT 1 OFFSET 0) and would fail
        // for any record not on the first page.
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw AchievementException.notFound("预警记录", id);
        }

        // Load JOIN fields via fee_record
        FeeRecord feeRecord = feeRecordMapper.selectById(record.getFeeRecordId());

        AlertRecordVO vo = new AlertRecordVO();
        vo.setId(record.getId());
        vo.setFeeRecordId(record.getFeeRecordId());
        vo.setAlertLevel(record.getAlertLevel());
        vo.setTriggeredDate(record.getTriggeredDate());
        vo.setTriggeredAt(record.getTriggeredAt());
        vo.setResolvedAt(record.getResolvedAt());
        vo.setStatus(record.getStatus());
        vo.setEscalationLevel(record.getEscalationLevel());
        if (feeRecord != null) {
            vo.setFeeAmount(feeRecord.getAmount());
            vo.setDueDate(feeRecord.getDueDate());
            vo.setOwnerType(feeRecord.getOwnerType());
            vo.setOwnerId(feeRecord.getOwnerId());
        }
        return vo;
    }
    public void resolve(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw AchievementException.notFound("预警记录", id);
        }
        if (!"pending".equals(alert.getStatus())) {
            throw AchievementException.invalidTransition(alert.getStatus(), "resolve");
        }

        alert.setStatus("resolved");
        alert.setResolvedAt(LocalDateTime.now());
        alertRecordMapper.updateById(alert);
        log.info("Alert resolved: id={}", id);
    }

    @Override
    @Transactional
    public void batchResolve(List<Long> ids) {
        for (Long id : ids) {
            try {
                resolve(id);
            } catch (AchievementException e) {
                log.warn("Batch resolve skipped alert id={}: {}", id, e.getMessage());
                // Continue with remaining IDs
            }
        }
    }

    @Override
    @Transactional
    public int processEscalations() {
        // Query all pending alerts that haven't reached max escalation level
        List<AlertRecord> pendingAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStatus, "pending")
                        .ne(AlertRecord::getEscalationLevel, "LEADERSHIP"));

        if (pendingAlerts.isEmpty()) {
            log.debug("Escalation scan: no pending alerts to escalate");
            return 0;
        }

        int escalatedDeptHead = 0;
        int escalatedLeadership = 0;

        for (AlertRecord alert : pendingAlerts) {
            try {
                long hoursSinceTrigger = Duration.between(alert.getTriggeredAt(), LocalDateTime.now()).toHours();
                EscalationLevel targetLevel = EscalationLevel.determineNextLevel(
                        alert.getEscalationLevel(), hoursSinceTrigger);
                if (targetLevel == null) {
                    continue; // No escalation needed yet
                }

                // Update the alert record
                alert.setEscalationLevel(targetLevel.getCode());
                alert.setEscalatedAt(LocalDateTime.now());
                alertRecordMapper.updateById(alert);

                // Load the associated fee record for notification content
                FeeRecord feeRecord = feeRecordMapper.selectById(alert.getFeeRecordId());
                if (feeRecord == null) {
                    log.warn("Fee record not found for alert id={}, feeRecordId={}",
                            alert.getId(), alert.getFeeRecordId());
                    continue;
                }

                // Determine target users based on escalation level
                String feeTypeLabel = resolveFeeTypeLabel(feeRecord.getFeeType());
                String ownerName = resolveOwnerName(feeRecord);

                if (EscalationLevel.DEPT_HEAD.equals(targetLevel)) {
                    // Notify department heads / secretaries
                    List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                            alert.getDeptId(), "ROLE_SECRETARY");
                    // Also notify department admins
                    List<Long> adminIds = notificationService.findUserIdsByDeptAndRole(
                            alert.getDeptId(), "ROLE_DEPT_ADMIN");
                    userIds.addAll(adminIds);

                    String title = "费用预警升级 — 请部门负责人处理";
                    String content = String.format(
                            "费用「%s」已于 %s 到期，首次预警已发送3天仍未处理。请部门负责人督促处理。",
                            feeTypeLabel, feeRecord.getDueDate());

                    for (Long userId : userIds) {
                        notificationService.send(userId, "ALERT", title, content,
                                "fee", feeRecord.getId());
                    }
                    escalatedDeptHead++;
                    log.debug("Alert escalated to DEPT_HEAD: alertId={}, deptId={}, users={}",
                            alert.getId(), alert.getDeptId(), userIds);

                } else if (EscalationLevel.LEADERSHIP.equals(targetLevel)) {
                    // Notify leaders across all departments
                    List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                            alert.getDeptId(), "ROLE_LEADER");

                    String title = "费用预警升级 — 请院领导关注";
                    String content = String.format(
                            "费用「%s」已于 %s 到期，部门负责人通知已发送5天仍未处理。请院领导关注处理。",
                            feeTypeLabel, feeRecord.getDueDate());

                    for (Long userId : userIds) {
                        notificationService.send(userId, "ALERT", title, content,
                                "fee", feeRecord.getId());
                    }
                    escalatedLeadership++;
                    log.debug("Alert escalated to LEADERSHIP: alertId={}, deptId={}, users={}",
                            alert.getId(), alert.getDeptId(), userIds);
                }

            } catch (Exception e) {
                log.error("Failed to escalate alert id={}: {}", alert.getId(), e.getMessage());
                // Continue with next alert — partial failures are acceptable
            }
        }

        log.info("Alert escalation completed: DEPT_HEAD={}, LEADERSHIP={}",
                escalatedDeptHead, escalatedLeadership);
        return escalatedDeptHead + escalatedLeadership;
    }

    @Override
    @Transactional
    public void processSingleEscalation(Long alertRecordId) {
        AlertRecord alert = alertRecordMapper.selectById(alertRecordId);
        if (alert == null) {
            throw AchievementException.notFound("预警记录", alertRecordId);
        }
        if (!"pending".equals(alert.getStatus())) {
            throw AchievementException.invalidTransition(alert.getStatus(), "escalate");
        }

        // Use the same logic by treating this as the escalation scan's threshold
        long hoursSinceTrigger = Duration.between(alert.getTriggeredAt(), LocalDateTime.now()).toHours();
        EscalationLevel targetLevel = EscalationLevel.determineNextLevel(
                alert.getEscalationLevel(), hoursSinceTrigger);
        if (targetLevel == null) {
            log.info("Alert id={} does not yet meet escalation threshold (hoursSinceTrigger={})",
                    alertRecordId, hoursSinceTrigger);
            return; // Not yet time to escalate, but not an error
        }

        // Update the alert record
        alert.setEscalationLevel(targetLevel.getCode());
        alert.setEscalatedAt(LocalDateTime.now());
        alertRecordMapper.updateById(alert);

        // Load the associated fee record for notification content
        FeeRecord feeRecord = feeRecordMapper.selectById(alert.getFeeRecordId());
        if (feeRecord == null) {
            log.warn("Fee record not found for alert id={}", alert.getId());
            return;
        }

        String feeTypeLabel = resolveFeeTypeLabel(feeRecord.getFeeType());

        if (EscalationLevel.DEPT_HEAD.equals(targetLevel)) {
            List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                    alert.getDeptId(), "ROLE_SECRETARY");
            List<Long> adminIds = notificationService.findUserIdsByDeptAndRole(
                    alert.getDeptId(), "ROLE_DEPT_ADMIN");
            userIds.addAll(adminIds);

            String title = "费用预警升级 — 请部门负责人处理";
            String content = String.format(
                    "费用「%s」已于 %s 到期，首次预警已发送3天仍未处理。请部门负责人督促处理。",
                    feeTypeLabel, feeRecord.getDueDate());

            for (Long userId : userIds) {
                notificationService.send(userId, "ALERT", title, content,
                        "fee", feeRecord.getId());
            }
            log.info("Manual escalation to DEPT_HEAD: alertId={}", alertRecordId);

        } else if (EscalationLevel.LEADERSHIP.equals(targetLevel)) {
            List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                    alert.getDeptId(), "ROLE_LEADER");

            String title = "费用预警升级 — 请院领导关注";
            String content = String.format(
                    "费用「%s」已于 %s 到期，部门负责人通知已发送5天仍未处理。请院领导关注处理。",
                    feeTypeLabel, feeRecord.getDueDate());

            for (Long userId : userIds) {
                notificationService.send(userId, "ALERT", title, content,
                        "fee", feeRecord.getId());
            }
            log.info("Manual escalation to LEADERSHIP: alertId={}", alertRecordId);
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    /**
     * Determine which user to notify for a given fee record.
     * <p>
     * For patents: load the patent's created_by as the responsible person.
     * For copyrights: fall back to fee_record's created_by.
     *
     * @param feeRecord the fee record
     * @return the user ID to notify
     */
    private Long resolveNotifyUserId(FeeRecord feeRecord) {
        if ("patent".equals(feeRecord.getOwnerType()) && feeRecord.getOwnerId() != null) {
            try {
                Patent patent = patentMapper.selectById(feeRecord.getOwnerId());
                if (patent != null && patent.getCreatedBy() != null && patent.getCreatedBy() > 0) {
                    return patent.getCreatedBy();
                }
            } catch (Exception e) {
                log.warn("Failed to resolve patent creator for feeRecord id={}: {}",
                        feeRecord.getId(), e.getMessage());
            }
        }

        // Fallback: use fee_record's creator
        if (feeRecord.getCreatedBy() != null && feeRecord.getCreatedBy() > 0) {
            return feeRecord.getCreatedBy();
        }

        // Last resort: send to department (sentinel userId=0, same pattern as NotificationService)
        return 0L;
    }

    /**
     * Resolve a human-readable fee type label from the fee type code.
     */
    private String resolveFeeTypeLabel(String feeTypeCode) {
        if (feeTypeCode == null) return "未知费用";
        FeeTypeEnum e = FeeTypeEnum.fromCode(feeTypeCode);
        return e != null ? e.getLabel() : feeTypeCode;
    }

    /**
     * Resolve the owner name from a fee record by querying the appropriate table.
     * <p>
     * This is used for notification content — the query-based approach in
     * AlertRecordMapper is preferred for the paginated listing.
     */
    private String resolveOwnerName(FeeRecord feeRecord) {
        if ("patent".equals(feeRecord.getOwnerType()) && feeRecord.getOwnerId() != null) {
            try {
                Patent patent = patentMapper.selectById(feeRecord.getOwnerId());
                if (patent != null && patent.getPatentName() != null) {
                    return patent.getPatentName();
                }
            } catch (Exception e) {
                log.trace("Failed to resolve patent name for feeRecord id={}: {}",
                        feeRecord.getId(), e.getMessage());
            }
        }
        // For copyrights and fallback, return a generic identifier
        return feeRecord.getOwnerType() + "#" + feeRecord.getOwnerId();
    }
}
