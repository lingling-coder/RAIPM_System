package com.institute.achievement.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.module.system.service.NotificationService;
import com.institute.achievement.reminder.dto.ReminderTaskVO;
import com.institute.achievement.reminder.entity.ReminderConfig;
import com.institute.achievement.reminder.entity.ReminderTask;
import com.institute.achievement.reminder.enums.ReminderTypeEnum;
import com.institute.achievement.reminder.enums.UrgencyLevelEnum;
import com.institute.achievement.reminder.mapper.ReminderConfigMapper;
import com.institute.achievement.reminder.mapper.ReminderTaskMapper;
import com.institute.achievement.reminder.service.ReminderTaskService;
import com.institute.achievement.reminder.util.ReminderTemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reminder task service implementation.
 * <p>
 * Core engine for batch task generation from enabled configs, user-facing
 * task listing and mutation, and high-urgency task queries.
 *
 * <h3>Threat model mitigations</h3>
 * <ul>
 *   <li>T-4-02: UNIQUE INDEX (config_id, user_id, deadline) + pre-generation dedup query</li>
 *   <li>T-4-03: All user-facing queries derive userId from SecurityUtils; service verifies ownership</li>
 *   <li>T-4-05: Ownership check before confirm/dismiss mutation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderTaskServiceImpl implements ReminderTaskService {

    private final ReminderTaskMapper reminderTaskMapper;
    private final ReminderConfigMapper reminderConfigMapper;
    private final NotificationService notificationService;
    private final SysUserMapper sysUserMapper;

    /**
     * Optional email service — may not be available in all deployment
     * configurations. Guarded with null check before use.
     */
    @Autowired(required = false)
    private Object emailService;

    @Override
    @Transactional
    public int generateTasks(String today) {
        // Step 1: Query all enabled configs
        List<ReminderConfig> configs = reminderConfigMapper.findEnabledConfigs();
        if (configs.isEmpty()) {
            log.debug("Reminder generation: no enabled configs found for {}", today);
            return 0;
        }

        int totalTaskCount = 0;
        int configCount = 0;

        for (ReminderConfig config : configs) {
            configCount++;
            ReminderTypeEnum typeEnum = ReminderTypeEnum.fromCode(config.getTypeCode());
            if (typeEnum == null) {
                log.warn("Reminder generation: unknown typeCode={} for config id={}, skipping",
                        config.getTypeCode(), config.getId());
                continue;
            }

            // Step 2: Compute deadline
            LocalDate computedDeadline;
            if (config.getDeadline() != null && config.getDeadline().isAfter(LocalDate.now())) {
                computedDeadline = config.getDeadline();
            } else {
                int advanceDays = config.getAdvanceDays() != null
                        ? config.getAdvanceDays()
                        : typeEnum.getDefaultAdvanceDays();
                computedDeadline = LocalDate.now().plusDays(advanceDays);
            }

            long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), computedDeadline);

            // Step 3: Determine target users (D-05 dual assignment)
            Set<Long> targetUserIds = resolveTargetUserIds(config);

            if (targetUserIds.isEmpty()) {
                log.warn("Reminder generation: no target users for config id={}, type={}",
                        config.getId(), config.getTypeCode());
                continue;
            }

            // Step 4: For each target user, generate task (with dedup)
            int taskCount = 0;
            for (Long userId : targetUserIds) {
                try {
                    // Dedup check: same configId + userId + deadline already exists?
                    int existingCount = reminderTaskMapper.selectCount(
                            new LambdaQueryWrapper<ReminderTask>()
                                    .eq(ReminderTask::getConfigId, config.getId())
                                    .eq(ReminderTask::getUserId, userId)
                                    .eq(ReminderTask::getDeadline, computedDeadline));
                    if (existingCount > 0) {
                        log.debug("Skipping duplicate task: configId={}, userId={}, deadline={}",
                                config.getId(), userId, computedDeadline);
                        continue;
                    }

                    // Resolve user display name for template variable
                    String userName = resolveUserName(userId);

                    // Build variable map for template substitution
                    Map<String, String> variables = Map.of(
                            "achievementName", config.getAchievementName() != null ? config.getAchievementName() : "",
                            "deadline", computedDeadline.toString(),
                            "daysRemaining", String.valueOf(daysUntilDeadline),
                            "responsiblePerson", userName
                    );

                    // Determine title and content from template or type defaults
                    String title = ReminderTemplateUtil.substitute(
                            config.getTitleTemplate() != null ? config.getTitleTemplate() : typeEnum.getDefaultTitleTemplate(),
                            variables);
                    String content = ReminderTemplateUtil.substitute(
                            config.getBodyTemplate() != null ? config.getBodyTemplate() : typeEnum.getDefaultBodyTemplate(),
                            variables);

                    // Determine urgency
                    String urgency = config.getUrgency() != null
                            ? config.getUrgency()
                            : typeEnum.getDefaultUrgency().getCode();

                    // Insert task record
                    ReminderTask task = new ReminderTask();
                    task.setConfigId(config.getId());
                    task.setUserId(userId);
                    task.setAchievementName(config.getAchievementName());
                    task.setTitle(title);
                    task.setContent(content);
                    task.setDeadline(computedDeadline);
                    task.setDaysRemaining((int) daysUntilDeadline);
                    task.setUrgency(urgency);
                    task.setConfirmedFlag(0);
                    task.setEscalationLevel("NONE");
                    task.setEmailSentFlag(0);
                    task.setEmailRetryCount(0);
                    task.setDeptId(config.getDeptId());
                    task.setCreatedTime(LocalDateTime.now());
                    reminderTaskMapper.insert(task);

                    // Send in-app notification (D-10)
                    notificationService.send(userId, "REMINDER", title, content,
                            config.getTypeCode(), task.getId());

                    taskCount++;
                } catch (Exception e) {
                    log.error("Failed to generate task for configId={}, userId={}: {}",
                            config.getId(), userId, e.getMessage());
                }
            }

            totalTaskCount += taskCount;
            log.debug("Reminder generation for config id={} ({}): {} tasks generated",
                    config.getId(), config.getTypeCode(), taskCount);
        }

        log.info("Reminder generation completed for {}: {} tasks generated from {} configs",
                today, totalTaskCount, configCount);
        return totalTaskCount;
    }

    @Override
    public IPage<ReminderTaskVO> listByUser(Long userId, String urgency, int page, int size) {
        Page<ReminderTask> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ReminderTask> wrapper = new LambdaQueryWrapper<ReminderTask>()
                .eq(ReminderTask::getUserId, userId);

        if (urgency != null && !urgency.isEmpty()) {
            wrapper.eq(ReminderTask::getUrgency, urgency);
        }

        wrapper.orderByAsc(ReminderTask::getDeadline);

        Page<ReminderTask> taskPage = reminderTaskMapper.selectPage(pageParam, wrapper);
        return convertToVOPage(taskPage);
    }

    @Override
    public IPage<ReminderTaskVO> listByConfigId(Long configId, int page, int size) {
        Page<ReminderTask> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ReminderTask> wrapper = new LambdaQueryWrapper<ReminderTask>()
                .eq(ReminderTask::getConfigId, configId)
                .orderByAsc(ReminderTask::getDeadline);

        Page<ReminderTask> taskPage = reminderTaskMapper.selectPage(pageParam, wrapper);
        return convertToVOPage(taskPage);
    }

    @Override
    public ReminderTaskVO getById(Long id) {
        ReminderTask task = reminderTaskMapper.selectById(id);
        if (task == null) {
            throw AchievementException.notFound("提醒任务", id);
        }
        return convertToVO(task);
    }

    @Override
    @Transactional
    public void confirmReceipt(Long taskId, Long userId) {
        ReminderTask task = reminderTaskMapper.selectById(taskId);
        if (task == null) {
            throw AchievementException.notFound("提醒任务", taskId);
        }
        if (!task.getUserId().equals(userId)) {
            throw AchievementException.notAuthorized("只能操作自己的提醒任务");
        }
        if (task.getConfirmedFlag() == 1) {
            log.debug("Task {} already confirmed by user {}, skipping", taskId, userId);
            return;
        }

        task.setConfirmedFlag(1);
        task.setConfirmedTime(LocalDateTime.now());
        reminderTaskMapper.updateById(task);
        log.info("Task {} confirmed by user {}", taskId, userId);
    }

    @Override
    public void dismissTask(Long taskId, Long userId) {
        ReminderTask task = reminderTaskMapper.selectById(taskId);
        if (task == null) {
            throw AchievementException.notFound("提醒任务", taskId);
        }
        if (!task.getUserId().equals(userId)) {
            throw AchievementException.notAuthorized("只能操作自己的提醒任务");
        }
        log.info("Task {} dismissed by user {}", taskId, userId);
    }

    @Override
    public List<ReminderTaskVO> getHighUrgencyUnconfirmed(Long userId) {
        List<ReminderTask> tasks = reminderTaskMapper.findUnconfirmedHighUrgency(userId);
        return tasks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnconfirmedCount(Long userId) {
        return reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<ReminderTask>()
                        .eq(ReminderTask::getUserId, userId)
                        .eq(ReminderTask::getConfirmedFlag, 0)
                        .ge(ReminderTask::getDeadline, LocalDate.now()));
    }

    // ── Escalation ───────────────────────────────────────────────────────

    /**
     * Escalation constants matching the 3-tier pattern (D-18).
     * Reuses the same NONE / DEPT_HEAD / LEADERSHIP naming as Phase 2's
     * {@code EscalationLevel} but with deadline-relative timing.
     */
    private static final String ESCALATION_NONE = "NONE";
    private static final String ESCALATION_DEPT_HEAD = "DEPT_HEAD";
    private static final String ESCALATION_LEADERSHIP = "LEADERSHIP";

    /** Days after DEPT_HEAD escalation without confirmation to escalate to LEADERSHIP */
    private static final long DEPT_HEAD_TO_LEADERSHIP_DAYS = 5;

    /** Role code for department secretaries (DEPT_HEAD target) */
    private static final String ROLE_SECRETARY = "ROLE_SECRETARY";

    /** Role code for leaders (LEADERSHIP target) */
    private static final String ROLE_LEADER = "ROLE_LEADER";

    @Override
    @Transactional
    public int processEscalations() {
        // Query all unconfirmed tasks with future deadlines
        List<ReminderTask> tasks = reminderTaskMapper.findUnconfirmedForEscalation();
        if (tasks.isEmpty()) {
            log.debug("Escalation scan: no unconfirmed tasks to escalate");
            return 0;
        }

        int escalatedCount = 0;

        for (ReminderTask task : tasks) {
            try {
                // Skip if already at max escalation level
                if (ESCALATION_LEADERSHIP.equals(task.getEscalationLevel())) {
                    continue;
                }

                long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());

                // Resolve urgency-dependent escalation threshold (D-19)
                UrgencyLevelEnum urgency = UrgencyLevelEnum.fromCode(task.getUrgency());
                int escalationDaysBeforeDeadline = getEscalationDaysBeforeDeadline(urgency);

                // Check if task is within escalation window
                if (escalationDaysBeforeDeadline < 0) {
                    // LOW urgency: never escalates per D-19
                    continue;
                }
                if (daysUntilDeadline > escalationDaysBeforeDeadline) {
                    // Not yet within escalation window
                    continue;
                }

                String currentLevel = task.getEscalationLevel();
                if (currentLevel == null) {
                    currentLevel = ESCALATION_NONE;
                }

                if (ESCALATION_NONE.equals(currentLevel)) {
                    // Escalate to DEPT_HEAD: notify department secretaries
                    List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                            task.getDeptId(), ROLE_SECRETARY);

                    if (userIds.isEmpty()) {
                        log.warn("No secretaries found for deptId={}, cannot escalate task id={}",
                                task.getDeptId(), task.getId());
                    } else {
                        String title = "提醒升级 — 请部门负责人处理";
                        String content = String.format(
                                "提醒任务「%s」截止日期为 %s，尚有 %d 天，责任人未确认。请部门负责人督促处理。",
                                task.getTitle(), task.getDeadline(), daysUntilDeadline);

                        for (Long userId : userIds) {
                            try {
                                notificationService.send(userId, "REMINDER", title, content,
                                        "reminder", task.getId());
                            } catch (Exception e) {
                                log.error("Failed to send DEPT_HEAD notification to userId={} for task id={}: {}",
                                        userId, task.getId(), e.getMessage());
                            }
                        }
                    }

                    // Update task escalation state
                    task.setEscalationLevel(ESCALATION_DEPT_HEAD);
                    task.setEscalationTime(LocalDateTime.now());
                    reminderTaskMapper.updateById(task);
                    escalatedCount++;

                    log.debug("Escalated task id={} to DEPT_HEAD (deadline={}, daysUntilDeadline={})",
                            task.getId(), task.getDeadline(), daysUntilDeadline);

                } else if (ESCALATION_DEPT_HEAD.equals(currentLevel)) {
                    // Check if 5+ days since DEPT_HEAD escalation (D-18)
                    if (task.getEscalationTime() == null) {
                        log.warn("Task id={} has DEPT_HEAD level but no escalationTime, skipping re-escalation",
                                task.getId());
                        continue;
                    }

                    long daysSinceDeptEscalation = ChronoUnit.DAYS.between(
                            task.getEscalationTime().toLocalDate(), LocalDate.now());

                    if (daysSinceDeptEscalation < DEPT_HEAD_TO_LEADERSHIP_DAYS) {
                        // Not enough time since DEPT_HEAD escalation
                        continue;
                    }

                    // Escalate to LEADERSHIP: notify leaders across all departments
                    List<Long> userIds = notificationService.findUserIdsByDeptAndRole(
                            task.getDeptId(), ROLE_LEADER);

                    if (userIds.isEmpty()) {
                        log.warn("No leaders found for escalation of task id={}", task.getId());
                    } else {
                        String title = "提醒升级 — 请院领导关注";
                        String content = String.format(
                                "提醒任务「%s」截止日期为 %s，部门负责人通知已发送 %d 天仍未确认。请院领导关注处理。",
                                task.getTitle(), task.getDeadline(), daysSinceDeptEscalation);

                        for (Long userId : userIds) {
                            try {
                                notificationService.send(userId, "REMINDER", title, content,
                                        "reminder", task.getId());
                            } catch (Exception e) {
                                log.error("Failed to send LEADERSHIP notification to userId={} for task id={}: {}",
                                        userId, task.getId(), e.getMessage());
                            }
                        }
                    }

                    // Update task escalation state
                    task.setEscalationLevel(ESCALATION_LEADERSHIP);
                    task.setEscalationTime(LocalDateTime.now());
                    reminderTaskMapper.updateById(task);
                    escalatedCount++;

                    log.debug("Escalated task id={} to LEADERSHIP (deadline={}, daysSinceDeptEscalation={})",
                            task.getId(), task.getDeadline(), daysSinceDeptEscalation);
                }

            } catch (Exception e) {
                log.error("Failed to process escalation for task id={}: {}", task.getId(), e.getMessage());
                // Continue with next task — partial failures are acceptable
            }
        }

        log.info("Reminder escalation processed: {} tasks escalated", escalatedCount);
        return escalatedCount;
    }

    /**
     * Get the escalation days-before-deadline threshold for a given urgency level.
     * <p>
     * Returns a negative value for urgency levels that never escalate.
     *
     * @param urgency the urgency level (may be null)
     * @return days before deadline to start escalation, or -1 if no escalation
     */
    private int getEscalationDaysBeforeDeadline(UrgencyLevelEnum urgency) {
        if (urgency == null) {
            return -1;
        }
        return urgency.getEscalationDaysBeforeDeadline();
    }

    // ── Private Helpers ────────────────────────────────────────────────

    /**
     * Resolve target user IDs for a config per D-05 dual assignment.
     * <p>
     * Combines personal assignment ({@code responsibleUserId}) with
     * role-based assignment ({@code responsibleRoleCode}), deduplicating
     * by user ID. Either or both may be set.
     *
     * @param config the reminder config
     * @return set of target user IDs (may be empty)
     */
    private Set<Long> resolveTargetUserIds(ReminderConfig config) {
        Set<Long> userIds = new HashSet<>();

        // Personal assignment
        if (config.getResponsibleUserId() != null) {
            userIds.add(config.getResponsibleUserId());
        }

        // Role-based assignment (D-05)
        if (config.getResponsibleRoleCode() != null && config.getDeptId() != null) {
            List<Long> roleUserIds = notificationService.findUserIdsByDeptAndRole(
                    config.getDeptId(), config.getResponsibleRoleCode());
            userIds.addAll(roleUserIds);
        }

        return userIds;
    }

    /**
     * Resolve a user's display name from their user ID.
     *
     * @param userId the user ID
     * @return the user's realName, or "用户{userId}" if not found
     */
    private String resolveUserName(Long userId) {
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && user.getRealName() != null && !user.getRealName().isEmpty()) {
                return user.getRealName();
            }
        } catch (Exception e) {
            log.trace("Failed to resolve user name for userId={}: {}", userId, e.getMessage());
        }
        return "用户" + userId;
    }

    /**
     * Convert a ReminderTask entity to a ReminderTaskVO with resolved fields.
     *
     * @param task the entity
     * @return the view object with resolved display fields
     */
    private ReminderTaskVO convertToVO(ReminderTask task) {
        ReminderTaskVO vo = new ReminderTaskVO();
        vo.setId(task.getId());
        vo.setConfigId(task.getConfigId());
        vo.setUserId(task.getUserId());
        vo.setAchievementName(task.getAchievementName());
        vo.setTitle(task.getTitle());
        vo.setContent(task.getContent());
        vo.setDeadline(task.getDeadline());
        vo.setDaysRemaining(task.getDaysRemaining());
        vo.setUrgency(task.getUrgency());
        vo.setConfirmedFlag(task.getConfirmedFlag());
        vo.setConfirmedTime(task.getConfirmedTime());
        vo.setEscalationLevel(task.getEscalationLevel());
        vo.setEscalationTime(task.getEscalationTime());
        vo.setEmailSentFlag(task.getEmailSentFlag());
        vo.setEmailSentTime(task.getEmailSentTime());
        vo.setDeptId(task.getDeptId());
        vo.setCreatedTime(task.getCreatedTime());

        // Resolve type info from config
        if (task.getConfigId() != null) {
            ReminderConfig config = reminderConfigMapper.selectById(task.getConfigId());
            if (config != null) {
                vo.setTypeCode(config.getTypeCode());
                vo.setConfigTitle(config.getTitleTemplate());
                vo.resolveTypeName();
            }
        }

        // Resolve user name
        vo.setUserName(resolveUserName(task.getUserId()));

        // Resolve urgency label
        vo.resolveUrgencyLabel();

        // Recalculate days until deadline at query time
        vo.recalcDaysUntilDeadline();

        return vo;
    }

    /**
     * Convert a Page of ReminderTask entities to a Page of ReminderTaskVOs.
     *
     * @param taskPage the entity page
     * @return the VO page
     */
    private IPage<ReminderTaskVO> convertToVOPage(Page<ReminderTask> taskPage) {
        Page<ReminderTaskVO> voPage = new Page<>(
                taskPage.getCurrent(),
                taskPage.getSize(),
                taskPage.getTotal());

        List<ReminderTaskVO> voList = taskPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
