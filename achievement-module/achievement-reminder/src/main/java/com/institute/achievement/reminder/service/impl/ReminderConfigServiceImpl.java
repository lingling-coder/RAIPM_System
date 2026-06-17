package com.institute.achievement.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.reminder.dto.ReminderConfigDTO;
import com.institute.achievement.reminder.dto.ReminderConfigVO;
import com.institute.achievement.reminder.entity.ReminderConfig;
import com.institute.achievement.reminder.enums.ReminderTypeEnum;
import com.institute.achievement.reminder.enums.UrgencyLevelEnum;
import com.institute.achievement.reminder.mapper.ReminderConfigMapper;
import com.institute.achievement.reminder.mapper.ReminderTaskMapper;
import com.institute.achievement.reminder.service.ReminderConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reminder configuration service implementation (D-02, D-03).
 * <p>
 * Implements full CRUD with validation, type name resolution from enum,
 * pending-task guard on delete, and query for enabled configs consumed
 * by the daily scheduler.
 *
 * <h3>Threat model mitigations</h3>
 * <ul>
 *   <li>T-4-02: jakarta.validation constraints on DTO; server-side validation before DB insert</li>
 *   <li>T-4-03: Delete checks for pending tasks before allowing deletion</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderConfigServiceImpl implements ReminderConfigService {

    private final ReminderConfigMapper reminderConfigMapper;
    private final ReminderTaskMapper reminderTaskMapper;

    @Override
    public Page<ReminderConfigVO> page(int page, int size, String typeCode) {
        Page<ReminderConfig> pageParam = new Page<>(page, size);
        Page<ReminderConfig> configPage = reminderConfigMapper.selectConfigPage(pageParam, typeCode);

        Page<ReminderConfigVO> voPage = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
        voPage.setRecords(configPage.getRecords().stream()
                .map(this::toVO)
                .toList());
        return voPage;
    }

    @Override
    public ReminderConfigVO getById(Long id) {
        ReminderConfig entity = reminderConfigMapper.selectById(id);
        if (entity == null) {
            throw AchievementException.notFound("提醒配置", id);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public void create(ReminderConfigDTO dto) {
        ReminderConfig entity = toEntity(dto);
        entity.setCreatedBy(SecurityUtils.getCurrentUserId());
        entity.setCreatedTime(LocalDateTime.now());
        entity.setDeptId(SecurityUtils.getCurrentDeptId());
        reminderConfigMapper.insert(entity);
        log.info("Reminder config created: typeCode={}, id={}", dto.getTypeCode(), entity.getId());
    }

    @Override
    @Transactional
    public void update(Long id, ReminderConfigDTO dto) {
        ReminderConfig entity = reminderConfigMapper.selectById(id);
        if (entity == null) {
            throw AchievementException.notFound("提醒配置", id);
        }

        // Use UpdateWrapper to selectively update non-null fields per existing project convention
        UpdateWrapper<ReminderConfig> uw = new UpdateWrapper<ReminderConfig>()
                .eq("id", id)
                .set(dto.getTypeCode() != null, "type_code", dto.getTypeCode())
                .set(dto.getAchievementName() != null, "achievement_name", dto.getAchievementName())
                .set(dto.getTitleTemplate() != null, "title_template", dto.getTitleTemplate())
                .set(dto.getBodyTemplate() != null, "body_template", dto.getBodyTemplate())
                .set(dto.getUrgency() != null, "urgency", dto.getUrgency())
                .set(dto.getAdvanceDays() != null, "advance_days", dto.getAdvanceDays())
                .set(dto.getDeadline() != null, "deadline", dto.getDeadline())
                .set(dto.getSchedulingRule() != null, "scheduling_rule", dto.getSchedulingRule())
                .set(dto.getResponsibleUserId() != null, "responsible_user_id", dto.getResponsibleUserId())
                .set(dto.getResponsibleRoleCode() != null, "responsible_role_code", dto.getResponsibleRoleCode())
                .set(dto.getStatus() != null, "status", dto.getStatus())
                .set("updated_by", SecurityUtils.getCurrentUserId());

        reminderConfigMapper.update(null, uw);
        log.info("Reminder config updated: id={}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ReminderConfig entity = reminderConfigMapper.selectById(id);
        if (entity == null) {
            throw AchievementException.notFound("提醒配置", id);
        }

        // Check for pending tasks before deletion (T-4-03)
        int pendingTaskCount = reminderTaskMapper.countByConfigId(id);
        if (pendingTaskCount > 0) {
            throw new AchievementException(AchievementException.MISSING_REQUIRED_FIELD,
                    "该提醒配置下存在" + pendingTaskCount + "条未处理的提醒任务，无法删除");
        }

        reminderConfigMapper.deleteById(id);
        log.info("Reminder config deleted: id={}", id);
    }

    @Override
    public List<ReminderConfig> findEnabledConfigs() {
        return reminderConfigMapper.findEnabledConfigs();
    }

    // ── Internal Mapping Helpers ──────────────────────────────────────────

    /**
     * Convert DTO to entity for create operation.
     */
    private ReminderConfig toEntity(ReminderConfigDTO dto) {
        ReminderConfig entity = new ReminderConfig();
        entity.setTypeCode(dto.getTypeCode());
        entity.setAchievementName(dto.getAchievementName());
        entity.setTitleTemplate(dto.getTitleTemplate());
        entity.setBodyTemplate(dto.getBodyTemplate());
        entity.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : "MEDIUM");
        entity.setAdvanceDays(dto.getAdvanceDays() != null ? dto.getAdvanceDays() : 30);
        entity.setDeadline(dto.getDeadline());
        entity.setSchedulingRule(dto.getSchedulingRule());
        entity.setResponsibleUserId(dto.getResponsibleUserId());
        entity.setResponsibleRoleCode(dto.getResponsibleRoleCode());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return entity;
    }

    /**
     * Convert entity to VO with resolved display fields.
     */
    private ReminderConfigVO toVO(ReminderConfig entity) {
        ReminderConfigVO vo = new ReminderConfigVO();
        vo.setId(entity.getId());
        vo.setTypeCode(entity.getTypeCode());

        // Resolve Chinese type name from enum
        ReminderTypeEnum typeEnum = ReminderTypeEnum.fromCode(entity.getTypeCode());
        vo.setTypeName(typeEnum != null ? typeEnum.getLabel() : entity.getTypeCode());

        vo.setAchievementName(entity.getAchievementName());
        vo.setTitleTemplate(entity.getTitleTemplate());
        vo.setBodyTemplate(entity.getBodyTemplate());
        vo.setUrgency(entity.getUrgency());
        vo.setAdvanceDays(entity.getAdvanceDays());
        vo.setDeadline(entity.getDeadline());

        // Compute deadline: explicit date or today + advanceDays
        if (entity.getDeadline() != null) {
            vo.setComputedDeadline(entity.getDeadline());
        } else if (entity.getAdvanceDays() != null) {
            vo.setComputedDeadline(LocalDate.now().plusDays(entity.getAdvanceDays()));
        }

        vo.setSchedulingRule(entity.getSchedulingRule());
        vo.setResponsibleUserId(entity.getResponsibleUserId());
        vo.setResponsibleRoleCode(entity.getResponsibleRoleCode());
        vo.setStatus(entity.getStatus());
        vo.setDeptId(entity.getDeptId());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
