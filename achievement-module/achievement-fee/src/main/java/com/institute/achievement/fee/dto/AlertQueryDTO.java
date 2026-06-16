package com.institute.achievement.fee.dto;

import lombok.Data;

/**
 * Query parameters for alert record paginated listing.
 * <p>
 * Supports filtering by status, alert level, and department.
 * deptId is injected from SecurityUtils for data isolation.
 */
@Data
public class AlertQueryDTO {

    /** Filter by alert status: pending, resolved, ignored */
    private String status;

    /** Filter by alert level: BLUE, YELLOW, ORANGE, RED */
    private String alertLevel;

    /** Filter by department ID (injected from SecurityUtils for data isolation) */
    private Long deptId;
}
