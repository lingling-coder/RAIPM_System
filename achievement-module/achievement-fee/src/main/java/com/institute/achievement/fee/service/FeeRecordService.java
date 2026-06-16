package com.institute.achievement.fee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.FeeRecordDTO;
import com.institute.achievement.fee.dto.FeeRecordQueryDTO;
import com.institute.achievement.fee.dto.FeeRecordVO;

/**
 * Service interface for fee record management.
 * <p>
 * Provides CRUD operations, paginated filtered listing, and access control
 * enforcement per the threat model (T-02-01-01 through T-02-01-05).
 */
public interface FeeRecordService {

    /**
     * Create a new fee record.
     *
     * @param dto the fee record data
     * @return the new fee record ID
     */
    Long create(FeeRecordDTO dto);

    /**
     * Update an existing fee record.
     * <p>
     * Only whitelisted fields are modifiable per T-02-01-02:
     * amount, fundingSource, paidAmount, voucherNo, status.
     * dueDate, ownerType, ownerId are system-locked (D-17).
     *
     * @param id  the fee record ID
     * @param dto the updated data
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    void update(Long id, FeeRecordDTO dto);

    /**
     * Get a fee record by ID.
     *
     * @param id the fee record ID
     * @return the fee record view object with owner name populated
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    FeeRecordVO getById(Long id);

    /**
     * Paginated fee record listing with multi-dimensional filtering.
     *
     * @param page     page number (1-based)
     * @param size     page size
     * @param query    filter parameters
     * @return paginated fee record VO list with owner names
     */
    Page<FeeRecordVO> page(int page, int size, FeeRecordQueryDTO query);

    /**
     * Delete a fee record.
     * <p>
     * Only allowed when status is PAUSED (T-02-01-05 mitigation).
     * Only the creator can delete (created_by == currentUser).
     *
     * @param id the fee record ID
     * @throws com.institute.achievement.common.exception.AchievementException if not pausable or unauthorized
     */
    void delete(Long id);
}
