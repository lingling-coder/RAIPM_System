package com.institute.achievement.fee.dto;

import lombok.Data;

/**
 * Fee plan query DTO — parameters for the paginated filtered listing endpoint.
 */
@Data
public class FeePlanQueryDTO {

    /** Filter by plan status (active / paused) */
    private String status;

    /** Filter by fee type code */
    private String feeType;

    /** Keyword search (matches patent name) */
    private String keyword;

    /** Filter by patent ID */
    private Long patentId;

    /** Filter by department ID (injected from SecurityUtils for data isolation) */
    private Long deptId;
}
