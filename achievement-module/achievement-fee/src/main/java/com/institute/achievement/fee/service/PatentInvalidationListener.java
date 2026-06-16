package com.institute.achievement.fee.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.institute.achievement.common.event.AchievementInvalidatedEvent;
import com.institute.achievement.fee.entity.FeePlan;
import com.institute.achievement.fee.entity.FeeRecord;
import com.institute.achievement.fee.enums.FeePlanStatusEnum;
import com.institute.achievement.fee.enums.FeeStatusEnum;
import com.institute.achievement.fee.mapper.FeePlanMapper;
import com.institute.achievement.fee.mapper.FeeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Event listener that auto-pauses fee plans when a patent is invalidated.
 * <p>
 * Listens for {@link AchievementInvalidatedEvent} published by the
 * achievement system's InvalidationService (D-18).
 * <p>
 * When a patent is invalidated:
 * <ol>
 *   <li>All active fee plans for that patent are set to PAUSED status</li>
 *   <li>All pending fee records for that patent are set to PAUSED status</li>
 *   <li>Historical paid records are preserved (not modified)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatentInvalidationListener {

    private final FeePlanMapper feePlanMapper;
    private final FeeRecordMapper feeRecordMapper;

    /**
     * Handle achievement invalidation event.
     * <p>
     * Only processes events where {@code ownerType == "patent"}.
     * Fee plans are patent-specific (not Arc polymorphic), so copyright
     * invalidation events are ignored.
     *
     * @param event the invalidation event
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void onPatentInvalidated(AchievementInvalidatedEvent event) {
        if (!"patent".equals(event.getOwnerType())) {
            return; // Only process patent invalidation events
        }

        Long patentId = event.getOwnerId();
        log.info("Processing patent invalidation: patentId={}, reason={}", patentId, event.getReason());

        // Pause all active fee plans for this patent
        int pausedPlans = feePlanMapper.batchPauseByPatentId(patentId);

        // Pause associated fee_records where status=pending
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
    }
}
