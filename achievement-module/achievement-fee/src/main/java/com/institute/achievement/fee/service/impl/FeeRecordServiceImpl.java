package com.institute.achievement.fee.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.event.AchievementArchivedEvent;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.fee.dto.FeeRecordDTO;
import com.institute.achievement.fee.dto.FeeRecordQueryDTO;
import com.institute.achievement.fee.dto.FeeRecordVO;
import com.institute.achievement.fee.entity.FeeRecord;
import com.institute.achievement.fee.enums.FeeStatusEnum;
import com.institute.achievement.fee.mapper.FeeRecordMapper;
import com.institute.achievement.fee.service.FeeRecordService;
import com.institute.achievement.fee.service.FeeSlipNumberGenerator;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fee record service implementation.
 * <p>
 * Implements CRUD with security checks per the Phase 2 threat model:
 * <ul>
 *   <li>T-02-01-01: creator/updater injected from SecurityUtils</li>
 *   <li>T-02-01-02: only whitelisted fields are updatable</li>
 *   <li>T-02-01-03: SQL-layer dept_id isolation via MyBatis-Plus interceptor</li>
 *   <li>T-02-01-04: paginated queries via MyBatis-Plus Page helper</li>
 *   <li>T-02-01-05: delete restricted to creator + paused status</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRecordServiceImpl implements FeeRecordService {

    private final FeeRecordMapper feeRecordMapper;
    private final FeeSlipNumberGenerator feeSlipNumberGenerator;

    @Override
    @Transactional
    public Long create(FeeRecordDTO dto) {
        FeeRecord record = new FeeRecord();
        record.setFeeType(dto.getFeeType());
        record.setAmount(dto.getAmount());
        record.setPaidAmount(dto.getPaidAmount());
        record.setDueDate(dto.getDueDate());
        record.setVoucherNo(dto.getVoucherNo());
        record.setStatus(dto.getStatus() != null ? dto.getStatus() : FeeStatusEnum.PENDING.getCode());
        record.setFundingSource(dto.getFundingSource());
        record.setOwnerType(dto.getOwnerType());
        record.setOwnerId(dto.getOwnerId());
        record.setSource(dto.getSource() != null ? dto.getSource() : "manual");
        record.setDeptId(SecurityUtils.getCurrentDeptId());
        record.setCreatedBy(SecurityUtils.getCurrentUserId());

        feeRecordMapper.insert(record);
        log.info("Fee record created: id={}, feeType={}, amount={}, dueDate={}, ownerType={}, ownerId={}",
                record.getId(), dto.getFeeType(), dto.getAmount(), dto.getDueDate(), dto.getOwnerType(), dto.getOwnerId());
        return record.getId();
    }

    @Override
    @Transactional
    public void update(Long id, FeeRecordDTO dto) {
        FeeRecord existing = feeRecordMapper.selectById(id);
        if (existing == null) {
            throw AchievementException.notFound("费用记录", id);
        }

        // T-02-01-02: Only whitelisted fields are updatable.
        // Use UpdateWrapper (not updateById) per Phase 01-03 decision.
        UpdateWrapper<FeeRecord> uw = new UpdateWrapper<FeeRecord>()
                .eq("id", id)
                .set("amount", dto.getAmount())
                .set("funding_source", dto.getFundingSource());

        // paidAmount, voucherNo, status are optional — only set if provided
        if (dto.getPaidAmount() != null) {
            uw.set("paid_amount", dto.getPaidAmount());
        }
        if (dto.getVoucherNo() != null) {
            uw.set("voucher_no", dto.getVoucherNo());
        }
        if (dto.getStatus() != null) {
            uw.set("status", dto.getStatus());
        }

        uw.set("updated_by", SecurityUtils.getCurrentUserId());
        uw.set("updated_time", LocalDateTime.now());

        feeRecordMapper.update(null, uw);
        log.info("Fee record updated: id={}", id);
    }

    @Override
    public FeeRecordVO getById(Long id) {
        FeeRecord record = feeRecordMapper.selectById(id);
        if (record == null) {
            throw AchievementException.notFound("费用记录", id);
        }
        return toVO(record);
    }

    @Override
    public Page<FeeRecordVO> page(int page, int size, FeeRecordQueryDTO query) {
        // Inject data isolation filter (T-02-01-03)
        if (query == null) {
            query = new FeeRecordQueryDTO();
        }
        if (query.getDeptId() == null) {
            query.setDeptId(SecurityUtils.getCurrentDeptId());
        }

        Page<FeeRecordVO> pageParam = new Page<>(page, size);
        return feeRecordMapper.selectFeeRecordPage(pageParam, query);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FeeRecord record = feeRecordMapper.selectById(id);
        if (record == null) {
            throw AchievementException.notFound("费用记录", id);
        }

        // T-02-01-05: Only the creator can delete, AND only when status is PAUSED
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!record.getCreatedBy().equals(currentUserId)) {
            throw AchievementException.notAuthorized("只能删除自己的费用记录");
        }
        if (!FeeStatusEnum.PAUSED.getCode().equals(record.getStatus())) {
            throw AchievementException.invalidTransition(record.getStatus(), "delete");
        }

        feeRecordMapper.deleteById(id);
        log.info("Fee record deleted: id={}", id);
    }

    /**
     * Event listener: when a patent is archived, auto-generate the first fee record
     * from the patent's nextFeeDate per D-11.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAchievementArchived(AchievementArchivedEvent event) {
        if (!"patent".equals(event.getOwnerType())) {
            return; // Only auto-generate fees for patents (copyright first fee is manual)
        }

        if (event.getNextFeeDate() == null) {
            log.info("No nextFeeDate for {}/{}, skipping auto-generation",
                    event.getOwnerType(), event.getOwnerId());
            return;
        }

        // Check if a fee record already exists for this achievement to avoid duplicates
        Long existingCount = feeRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FeeRecord>()
                        .eq(FeeRecord::getOwnerType, event.getOwnerType())
                        .eq(FeeRecord::getOwnerId, event.getOwnerId())
                        .eq(FeeRecord::getFeeType, "annual_fee"));
        if (existingCount > 0) {
            log.info("Fee record already exists for {}/{}, skipping auto-generation",
                    event.getOwnerType(), event.getOwnerId());
            return;
        }

        // Create the first annual fee record with amount=0 (placeholder, user edits later)
        FeeRecord record = new FeeRecord();
        record.setFeeType("annual_fee");
        record.setAmount(BigDecimal.ZERO);
        record.setDueDate(event.getNextFeeDate());
        record.setStatus(FeeStatusEnum.PENDING.getCode());
        record.setOwnerType(event.getOwnerType());
        record.setOwnerId(event.getOwnerId());
        record.setSource("auto_generated");
        record.setDeptId(SecurityUtils.getCurrentDeptId());
        record.setCreatedBy(SecurityUtils.getCurrentUserId());

        feeRecordMapper.insert(record);
        log.info("First fee auto-generated for {}/{}: id={}",
                event.getOwnerType(), event.getOwnerId(), record.getId());
    }

    @Override
    @Transactional
    public List<String> batchGenerateSlips(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new AchievementException(4003, "请选择要生成缴费单的费用记录");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<String> slipNumbers = new ArrayList<>(ids.size());

        for (Long id : ids) {
            FeeRecord record = feeRecordMapper.selectById(id);
            if (record == null) {
                throw AchievementException.notFound("费用记录", id);
            }
            if (!"pending".equals(record.getStatus())) {
                throw AchievementException.invalidTransition(record.getStatus(), "batchGenerateSlips");
            }

            // Generate a unique slip number for each record
            String slipNo = feeSlipNumberGenerator.generateSlipNo();

            // Persist the slip number
            int updated = feeRecordMapper.updateSlipNo(id, slipNo, currentUserId);
            if (updated == 0) {
                log.warn("Failed to update slip_no for feeRecord id={}", id);
                // Continue generating for remaining records — partial success is acceptable
            }

            slipNumbers.add(slipNo);
            log.debug("Slip generated for feeRecord id={}: slipNo={}", id, slipNo);
        }

        log.info("Batch slip generation: {} slips generated for {} records", slipNumbers.size(), ids.size());
        return slipNumbers;
    }

    @Override
    @Transactional
    public int batchPay(List<Long> ids, LocalDate paidDate, String voucherNo, String slipNo) {
        if (ids == null || ids.isEmpty()) {
            throw new AchievementException(4003, "请选择要标记为已缴费的费用记录");
        }
        if (!StringUtils.hasText(voucherNo)) {
            throw new AchievementException(4003, "凭证号不能为空");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();

        // batchMarkAsPaid has WHERE status='pending' guard (T-02-04-01)
        int affected = feeRecordMapper.batchMarkAsPaid(ids, paidDate, voucherNo, slipNo, currentUserId);

        log.info("Batch payment: {} records marked as paid, voucher={}", affected, voucherNo);
        return affected;
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    private FeeRecordVO toVO(FeeRecord record) {
        FeeRecordVO vo = new FeeRecordVO();
        vo.setId(record.getId());
        vo.setFeeType(record.getFeeType());
        vo.setAmount(record.getAmount());
        vo.setPaidAmount(record.getPaidAmount());
        vo.setDueDate(record.getDueDate());
        vo.setPaidDate(record.getPaidDate());
        vo.setVoucherNo(record.getVoucherNo());
        vo.setStatus(record.getStatus());
        vo.setFundingSource(record.getFundingSource());
        vo.setOwnerType(record.getOwnerType());
        vo.setOwnerId(record.getOwnerId());
        vo.setSource(record.getSource());
        vo.setSlipNo(record.getSlipNo());
        vo.setSlipGeneratedTime(record.getSlipGeneratedTime());
        vo.setSlipGeneratedBy(record.getSlipGeneratedBy());
        vo.setDeptId(record.getDeptId());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreatedTime(record.getCreatedTime());
        vo.setUpdatedBy(record.getUpdatedBy());
        vo.setUpdatedTime(record.getUpdatedTime());
        return vo;
    }
}
