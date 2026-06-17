package com.institute.achievement.reminder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.reminder.dto.ReminderConfigDTO;
import com.institute.achievement.reminder.dto.ReminderConfigVO;
import com.institute.achievement.reminder.entity.ReminderConfig;

import java.util.List;

/**
 * Service interface for reminder configuration CRUD.
 * <p>
 * Provides administrative CRUD operations plus query for enabled configs
 * consumed by the daily task generation scheduler.
 */
public interface ReminderConfigService {

    /**
     * Paginated listing with optional type code filter.
     *
     * @param page     page number (1-based)
     * @param size     page size
     * @param typeCode optional type code filter
     * @return paginated config VO list
     */
    Page<ReminderConfigVO> page(int page, int size, String typeCode);

    /**
     * Get a single config by ID with resolved display fields.
     *
     * @param id the config ID
     * @return the config VO
     */
    ReminderConfigVO getById(Long id);

    /**
     * Create a new reminder configuration.
     *
     * @param dto the config DTO
     */
    void create(ReminderConfigDTO dto);

    /**
     * Update an existing reminder configuration.
     *
     * @param id  the config ID
     * @param dto the config DTO with updated fields
     */
    void update(Long id, ReminderConfigDTO dto);

    /**
     * Delete a reminder configuration.
     * <p>
     * Checks for pending tasks referencing this config before allowing deletion.
     *
     * @param id the config ID
     * @throws com.institute.achievement.common.exception.AchievementException if pending tasks exist
     */
    void delete(Long id);

    /**
     * Find all enabled configs for the daily task scheduler.
     *
     * @return list of enabled reminder configs
     */
    List<ReminderConfig> findEnabledConfigs();
}
