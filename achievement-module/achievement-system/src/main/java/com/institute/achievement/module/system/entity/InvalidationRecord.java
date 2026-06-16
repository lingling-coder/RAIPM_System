package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Invalidation record entity — maps to the {@code invalidation_record} table.
 * <p>
 * Stores every invalidation action with the reason, the invalidator's identity,
 * and the achievement reference. Invalidation is irreversible (D-36), so this
 * record serves as a permanent audit trail.
 * <p>
 * Implements D-34~D-36 (irreversible invalidation by creator or dept secretary),
 * and T-01-20 (repudiation prevention — every invalidation has a traceable record).
 */
@Data
@TableName("invalidation_record")
public class InvalidationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Achievement type: paper, patent, or copyright */
    private String achievementType;

    /** Achievement ID */
    private Long achievementId;

    /** User ID of the person who performed the invalidation */
    private Long invalidatorId;

    /** Display name of the invalidator */
    private String invalidatorName;

    /** Reason for invalidation (free text, D-34) */
    private String reason;

    /** When the invalidation occurred */
    private LocalDateTime createdTime;
}
