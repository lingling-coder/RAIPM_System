package com.institute.achievement.fee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.FeePlanDTO;
import com.institute.achievement.fee.dto.FeePlanQueryDTO;
import com.institute.achievement.fee.dto.FeePlanVO;

/**
 * Service interface for fee plan management.
 * <p>
 * Provides CRUD with special rules (edit amount only, pause/restore),
 * recurring annual fee generation via scheduled task, and patent
 * invalidation auto-pause.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-02-01: update only copies amount, fundingSource from DTO — ignores dueDate, patentId</li>
 *   <li>T-02-02-02: Java-level dedup check + UNIQUE INDEX on (patent_id, fee_type, due_date)</li>
 *   <li>T-02-02-04: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 * </ul>
 */
public interface FeePlanService {

    /**
     * Create a new fee plan (manual one-time plan).
     *
     * @param dto the fee plan data
     * @return the new fee plan ID
     */
    Long create(FeePlanDTO dto);

    /**
     * Update an existing fee plan.
     * <p>
     * Only whitelisted fields are modifiable per T-02-02-01:
     * amount and fundingSource. dueDate and patentId are system-locked (D-17).
     *
     * @param id  the fee plan ID
     * @param dto the updated data
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    void update(Long id, FeePlanDTO dto);

    /**
     * Get a fee plan by ID.
     *
     * @param id the fee plan ID
     * @return the fee plan view object with patent info populated
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    FeePlanVO getById(Long id);

    /**
     * Paginated fee plan listing with multi-dimensional filtering.
     *
     * @param page  page number (1-based)
     * @param size  page size
     * @param query filter parameters
     * @return paginated fee plan list with patent info
     */
    Page<FeePlanVO> page(int page, int size, FeePlanQueryDTO query);

    /**
     * Pause a fee plan.
     * <p>
     * Only allowed when status is ACTIVE. Also pauses associated
     * fee_records for the same patent/type/dueDate.
     *
     * @param id the fee plan ID
     * @throws com.institute.achievement.common.exception.AchievementException if not active
     */
    void pause(Long id);

    /**
     * Restore a paused fee plan.
     * <p>
     * Only allowed when status is PAUSED.
     *
     * @param id the fee plan ID
     * @throws com.institute.achievement.common.exception.AchievementException if not paused
     */
    void restore(Long id);

    /**
     * Delete a fee plan.
     * <p>
     * Only allowed when status is PAUSED. Physically deletes the plan record.
     *
     * @param id the fee plan ID
     * @throws com.institute.achievement.common.exception.AchievementException if not paused
     */
    void delete(Long id);

    /**
     * Generate recurring annual fee plans for all authorized patents.
     * <p>
     * Called by the daily scheduled task (FeePlanGenerationTask).
     * Scans patents with active status, calculates next due date from
     * authorizationDate, checks for duplicates, and inserts both a
     * fee_plan and corresponding fee_record atomically.
     *
     * @return the number of plans created
     */
    int generateRecurringPlans();

    /**
     * Pause all active plans for a given patent.
     * <p>
     * Called by {@link PatentInvalidationListener} when a patent is invalidated.
     *
     * @param patentId the patent ID
     * @return the number of plans paused
     */
    int pauseByPatentId(Long patentId);
}
