package com.institute.achievement.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for invalidation request.
 * <p>
 * Used by InvalidationController to receive invalidation actions.
 * Validates required fields per UI-SPEC §5.2.
 */
@Data
public class InvalidationDTO {

    /** Achievement type: paper, patent, or copyright */
    @NotBlank(message = "成果类型不能为空")
    private String achievementType;

    /** Achievement ID */
    @NotNull(message = "成果ID不能为空")
    private Long achievementId;

    /** Reason for invalidation (D-34 requires reason recording) */
    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因不超过500字")
    private String reason;
}
