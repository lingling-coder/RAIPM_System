package com.institute.achievement.fee.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.fee.dto.FeePlanDTO;
import com.institute.achievement.fee.dto.FeePlanQueryDTO;
import com.institute.achievement.fee.dto.FeePlanVO;
import com.institute.achievement.fee.entity.FeePlan;
import com.institute.achievement.fee.entity.FeeRecord;
import com.institute.achievement.fee.enums.FeePlanStatusEnum;
import com.institute.achievement.fee.enums.FeeStatusEnum;
import com.institute.achievement.fee.mapper.FeePlanMapper;
import com.institute.achievement.fee.mapper.FeeRecordMapper;
import com.institute.achievement.fee.service.FeePlanService;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Fee plan service implementation.
 * <p>
 * Implements CRUD with special edit rules (amount/fundingSource only),
 * recurring annual fee generation via scheduled task, and patent
 * invalidation auto-pause.
 *
 * <h3>Threat model mitigations</h3>
 * <ul>
 *   <li>T-02-02-01: update() only copies amount, fundingSource from DTO — ignores dueDate, patentId</li>
 *   <li>T-02-02-02: countByPatentAndType() check BEFORE insert + UNIQUE INDEX on (patent_id, fee_type, due_date)</li>
 *   <li>T-02-02-04: dept_id injected from SecurityUtils for data isolation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeePlanServiceImpl implements FeePlanService {

    private final FeePlanMapper feePlanMapper;
    private final FeeRecordMapper feeRecordMapper;
    private final PatentMapper patentMapper;

    @Override
    @Transactional
    public Long create(FeePlanDTO dto) {
        // Validate patent exists
        Patent patent = patentMapper.selectById(dto.getPatentId());
        if (patent == null) {
            throw AchievementException.notFound("专利", dto.getPatentId());
        }

        FeePlan plan = new FeePlan();
        plan.setPatentId(dto.getPatentId());
        plan.setFeeType(dto.getFeeType());
        plan.setAmount(dto.getAmount());
        plan.setDueDate(dto.getDueDate());
        plan.setStatus(FeePlanStatusEnum.ACTIVE.getCode());
        plan.setSource(dto.getSource() != null ? dto.getSource() : "manual");
        plan.setFundingSource(dto.getFundingSource());
        plan.setDeptId(SecurityUtils.getCurrentDeptId());
        plan.setCreatedBy(SecurityUtils.getCurrentUserId());

        feePlanMapper.insert(plan);
        log.info("Fee plan created: id={}, patentId={}, feeType={}, amount={}, dueDate={}",
                plan.getId(), dto.getPatentId(), dto.getFeeType(), dto.getAmount(), dto.getDueDate());
        return plan.getId();
    }

    @Override
    @Transactional
    public void update(Long id, FeePlanDTO dto) {
        FeePlan existing = feePlanMapper.selectById(id);
        if (existing == null) {
            throw AchievementException.notFound("缴费计划", id);
        }

        // T-02-02-01: Only whitelisted fields are updatable.
        // dueDate and patentId are NEVER updated (D-17).
        // Use UpdateWrapper per Phase 01-03 decision.
        UpdateWrapper<FeePlan> uw = new UpdateWrapper<FeePlan>()
                .eq("id", id)
                .set("amount", dto.getAmount());

        // fundingSource is optional — only set if provided
        if (dto.getFundingSource() != null) {
            uw.set("funding_source", dto.getFundingSource());
        }

        uw.set("updated_by", SecurityUtils.getCurrentUserId());
        uw.set("updated_time", LocalDateTime.now());

        feePlanMapper.update(null, uw);
        log.info("Fee plan updated: id={}, newAmount={}", id, dto.getAmount());
    }

    @Override
    public FeePlanVO getById(Long id) {
        FeePlan plan = feePlanMapper.selectById(id);
        if (plan == null) {
            throw AchievementException.notFound("缴费计划", id);
        }
        return toVO(plan);
    }

    @Override
    public Page<FeePlanVO> page(int page, int size, FeePlanQueryDTO query) {
        // Inject data isolation filter (T-02-02-04)
        if (query == null) {
            query = new FeePlanQueryDTO();
        }
        if (query.getDeptId() == null) {
            query.setDeptId(SecurityUtils.getCurrentDeptId());
        }

        Page<FeePlanVO> pageParam = new Page<>(page, size);
        return feePlanMapper.selectFeePlanPage(pageParam, query);
    }

    @Override
    @Transactional
    public void pause(Long id) {
        FeePlan plan = feePlanMapper.selectById(id);
        if (plan == null) {
            throw AchievementException.notFound("缴费计划", id);
        }
        if (!FeePlanStatusEnum.ACTIVE.getCode().equals(plan.getStatus())) {
            throw AchievementException.invalidTransition(plan.getStatus(), "pause");
        }

        // Update plan status
        UpdateWrapper<FeePlan> uw = new UpdateWrapper<FeePlan>()
                .eq("id", id)
                .set("status", FeePlanStatusEnum.PAUSED.getCode())
                .set("updated_by", SecurityUtils.getCurrentUserId())
                .set("updated_time", LocalDateTime.now());
        feePlanMapper.update(null, uw);

        // Also pause associated fee_records for same patent/type/dueDate
        UpdateWrapper<FeeRecord> feeUw = new UpdateWrapper<FeeRecord>()
                .eq("owner_type", "patent")
                .eq("owner_id", plan.getPatentId())
                .eq("fee_type", plan.getFeeType())
                .eq("due_date", plan.getDueDate())
                .eq("status", FeeStatusEnum.PENDING.getCode())
                .set("status", FeeStatusEnum.PAUSED.getCode())
                .set("updated_by", SecurityUtils.getCurrentUserId())
                .set("updated_time", LocalDateTime.now());
        int affectedRecords = feeRecordMapper.update(null, feeUw);

        log.info("Fee plan paused: id={}, associated fee_records paused={}", id, affectedRecords);
    }

    @Override
    @Transactional
    public void restore(Long id) {
        FeePlan plan = feePlanMapper.selectById(id);
        if (plan == null) {
            throw AchievementException.notFound("缴费计划", id);
        }
        if (!FeePlanStatusEnum.PAUSED.getCode().equals(plan.getStatus())) {
            throw AchievementException.invalidTransition(plan.getStatus(), "restore");
        }

        // Update plan status
        UpdateWrapper<FeePlan> uw = new UpdateWrapper<FeePlan>()
                .eq("id", id)
                .set("status", FeePlanStatusEnum.ACTIVE.getCode())
                .set("updated_by", SecurityUtils.getCurrentUserId())
                .set("updated_time", LocalDateTime.now());
        feePlanMapper.update(null, uw);

        // Also restore associated fee_records (only paused ones, not paid ones)
        UpdateWrapper<FeeRecord> feeUw = new UpdateWrapper<FeeRecord>()
                .eq("owner_type", "patent")
                .eq("owner_id", plan.getPatentId())
                .eq("fee_type", plan.getFeeType())
                .eq("due_date", plan.getDueDate())
                .eq("status", FeeStatusEnum.PAUSED.getCode())
                .set("status", FeeStatusEnum.PENDING.getCode())
                .set("updated_by", SecurityUtils.getCurrentUserId())
                .set("updated_time", LocalDateTime.now());
        int affectedRecords = feeRecordMapper.update(null, feeUw);

        log.info("Fee plan restored: id={}, associated fee_records restored={}", id, affectedRecords);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FeePlan plan = feePlanMapper.selectById(id);
        if (plan == null) {
            throw AchievementException.notFound("缴费计划", id);
        }
        if (!FeePlanStatusEnum.PAUSED.getCode().equals(plan.getStatus())) {
            throw AchievementException.invalidTransition(plan.getStatus(), "delete");
        }

        feePlanMapper.deleteById(id);
        log.info("Fee plan deleted: id={}", id);
    }

    @Override
    @Transactional
    public int generateRecurringPlans() {
        // Query all patents eligible for recurring fee generation
        // legalStatus: '授权' (Chinese) or 'authorized' (English)
        // status: 'ARCHIVED' means the patent was approved and archived
        List<Patent> patents = patentMapper.selectList(
                new LambdaQueryWrapper<Patent>()
                        .in(Patent::getLegalStatus, "授权", "authorized")
                        .eq(Patent::getStatus, "ARCHIVED")
                        .isNotNull(Patent::getAuthorizationDate));

        if (patents.isEmpty()) {
            log.info("No eligible patents found for recurring fee generation");
            return 0;
        }

        int createdCount = 0;
        LocalDate today = LocalDate.now();

        for (Patent patent : patents) {
            try {
                // Calculate the next due date based on authorizationDate
                LocalDate authorizationDate = patent.getAuthorizationDate();
                if (authorizationDate == null) {
                    continue; // Skip patents without authorizationDate
                }

                // Calculate next year's due date: authorizationDate month/day + N years
                // Use Hutool DateUtil for safe month arithmetic
                Date authDate = java.sql.Date.valueOf(authorizationDate);
                int yearsSinceAuth = today.getYear() - authorizationDate.getYear();
                if (yearsSinceAuth <= 0) {
                    // First year fee — use the authorizationDate anniversary
                    // But only generate if the due date is in the future or current month
                    LocalDate firstDueDate = authorizationDate;
                    if (firstDueDate.isBefore(today)) {
                        // Already past — generate for next year
                        Date nextDate = DateUtil.offsetMonth(authDate, 12);
                        LocalDate calculatedDueDate = DateUtil.toLocalDateTime(nextDate).toLocalDate();

                        // Generate if no existing plan for this period
                        if (feePlanMapper.countByPatentAndType(patent.getId(), "annual_fee", calculatedDueDate) == 0) {
                            insertRecurringPlan(patent, calculatedDueDate);
                            createdCount++;
                        }
                    }
                } else {
                    // Calculate the due date for the next upcoming period
                    // Based on authorizationDate anniversary
                    LocalDate anniversaryThisYear = authorizationDate.withYear(today.getYear());

                    if (!anniversaryThisYear.isBefore(today)) {
                        // This year's anniversary hasn't passed yet — generate for this year
                        if (feePlanMapper.countByPatentAndType(patent.getId(), "annual_fee", anniversaryThisYear) == 0) {
                            insertRecurringPlan(patent, anniversaryThisYear);
                            createdCount++;
                        }
                    } else {
                        // This year's anniversary already passed — generate for next year
                        LocalDate nextYearAnniversary = anniversaryThisYear.plusYears(1);
                        if (feePlanMapper.countByPatentAndType(patent.getId(), "annual_fee", nextYearAnniversary) == 0) {
                            insertRecurringPlan(patent, nextYearAnniversary);
                            createdCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to generate recurring plan for patent id={}: {}", patent.getId(), e.getMessage());
                // Continue with next patent — partial failures are acceptable
            }
        }

        log.info("Recurring fee plan generation completed: {} plans created", createdCount);
        return createdCount;
    }

    /**
     * Insert a recurring fee plan and its corresponding fee record.
     */
    private void insertRecurringPlan(Patent patent, LocalDate dueDate) {
        // Create fee_plan
        FeePlan plan = new FeePlan();
        plan.setPatentId(patent.getId());
        plan.setFeeType("annual_fee");
        plan.setAmount(BigDecimal.ZERO); // Amount is placeholder — user edits later
        plan.setDueDate(dueDate);
        plan.setStatus(FeePlanStatusEnum.ACTIVE.getCode());
        plan.setSource("auto_generated");
        plan.setDeptId(patent.getDeptId());
        plan.setCreatedBy(0L); // System-generated
        feePlanMapper.insert(plan);

        // Create corresponding fee_record
        FeeRecord record = new FeeRecord();
        record.setFeeType("annual_fee");
        record.setAmount(BigDecimal.ZERO); // Amount is placeholder — user edits later
        record.setDueDate(dueDate);
        record.setStatus(FeeStatusEnum.PENDING.getCode());
        record.setOwnerType("patent");
        record.setOwnerId(patent.getId());
        record.setSource("auto_generated");
        record.setDeptId(patent.getDeptId());
        record.setCreatedBy(0L); // System-generated
        feeRecordMapper.insert(record);

        log.debug("Generated recurring plan: patentId={}, dueDate={}, planId={}, recordId={}",
                patent.getId(), dueDate, plan.getId(), record.getId());
    }

    @Override
    @Transactional
    public int pauseByPatentId(Long patentId) {
        // Pause all active fee plans for this patent
        int pausedPlans = feePlanMapper.batchPauseByPatentId(patentId);

        // Also pause associated fee_records where status=pending
        UpdateWrapper<FeeRecord> feeUw = new UpdateWrapper<FeeRecord>()
                .eq("owner_type", "patent")
                .eq("owner_id", patentId)
                .eq("status", FeeStatusEnum.PENDING.getCode())
                .set("status", FeeStatusEnum.PAUSED.getCode())
                .set("updated_by", 0L) // System action
                .set("updated_time", LocalDateTime.now());
        int pausedRecords = feeRecordMapper.update(null, feeUw);

        log.info("Auto-paused {} fee plans and {} fee records for invalidated patent id={}",
                pausedPlans, pausedRecords, patentId);
        return pausedPlans;
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    private FeePlanVO toVO(FeePlan plan) {
        FeePlanVO vo = new FeePlanVO();
        vo.setId(plan.getId());
        vo.setPatentId(plan.getPatentId());
        vo.setFeeType(plan.getFeeType());
        vo.setAmount(plan.getAmount());
        vo.setDueDate(plan.getDueDate());
        vo.setStatus(plan.getStatus());
        vo.setSource(plan.getSource());
        vo.setFundingSource(plan.getFundingSource());
        vo.setDeptId(plan.getDeptId());
        vo.setCreatedBy(plan.getCreatedBy());
        vo.setCreatedTime(plan.getCreatedTime());
        vo.setUpdatedBy(plan.getUpdatedBy());
        vo.setUpdatedTime(plan.getUpdatedTime());
        return vo;
    }
}
