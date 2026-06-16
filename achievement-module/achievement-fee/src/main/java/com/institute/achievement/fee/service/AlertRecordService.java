package com.institute.achievement.fee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.AlertQueryDTO;
import com.institute.achievement.fee.dto.AlertRecordVO;

import java.util.List;

/**
 * Service interface for alert record management and daily scan.
 * <p>
 * Provides the main {@link #scanAndGenerateAlerts()} method that classifies
 * pending fee records into 4 alert tiers (BLUE/YELLOW/ORANGE/RED), persists
 * alert records with dedup, and sends in-app notifications via
 * {@link com.institute.achievement.module.system.service.NotificationService}.
 */
public interface AlertRecordService {

    /**
     * Daily scan: classify pending fee records into 4 alert tiers and generate alerts.
     * <p>
     * <ol>
     *   <li>Query all fee_records where status='pending' AND due_date <= today + 30 days</li>
     *   <li>Classify each record using {@link com.institute.achievement.fee.enums.AlertLevelEnum#fromDueDate}</li>
     *   <li>Batch dedup: skip fee_record_ids that already have alerts for today</li>
     *   <li>Insert new alert records and send in-app notifications</li>
     * </ol>
     *
     * @return count of new alerts generated
     */
    int scanAndGenerateAlerts();

    /**
     * Paginated alert record listing with filtered search.
     *
     * @param page  page number (1-based)
     * @param size  page size
     * @param query filter parameters (status, alertLevel, deptId)
     * @return paginated alert record VOs
     */
    Page<AlertRecordVO> page(int page, int size, AlertQueryDTO query);

    /**
     * Get an alert record by ID with full details.
     *
     * @param id the alert record ID
     * @return the alert record view object
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    AlertRecordVO getById(Long id);

    /**
     * Mark an alert as resolved.
     * <p>
     * Sets status='resolved' and resolvedAt=now.
     * Only allowed when current status is 'pending'.
     *
     * @param id the alert record ID
     * @throws com.institute.achievement.common.exception.AchievementException if not found or not pending
     */
    void resolve(Long id);

    /**
     * Batch resolve multiple alerts.
     *
     * @param ids list of alert record IDs to resolve
     */
    void batchResolve(List<Long> ids);
}
