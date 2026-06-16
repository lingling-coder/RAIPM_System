package com.institute.achievement.fee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.fee.dto.AlertQueryDTO;
import com.institute.achievement.fee.dto.AlertRecordVO;
import com.institute.achievement.fee.entity.AlertRecord;
import com.institute.achievement.fee.entity.FeeRecord;
import com.institute.achievement.fee.enums.AlertLevelEnum;
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
        // Use paginated query with page=1 size=1, then extract the single result
        // This reuses the JOIN logic from selectAlertPage
        AlertQueryDTO query = new AlertQueryDTO();
        // Don't restrict dept_id for single-entity lookup
        // Security: MyBatis-Plus interceptor still enforces dept_id at SQL level

        Page<AlertRecordVO> pageResult = alertRecordMapper.selectAlertPage(
                new Page<>(1, 1), query);

        return pageResult.getRecords().stream()
                .filter(vo -> vo.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> AchievementException.notFound("预警记录", id));
    }

    @Override
    @Transactional
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
