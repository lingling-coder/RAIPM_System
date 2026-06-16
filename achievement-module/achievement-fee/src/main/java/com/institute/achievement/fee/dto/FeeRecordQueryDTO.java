package com.institute.achievement.fee.dto;

import lombok.Data;

/**
 * Fee record query DTO — parameters for the paginated filtered listing endpoint.
 */
@Data
public class FeeRecordQueryDTO {

    /** Filter by fee status code */
    private String status;

    /** Filter by fee type code */
    private String feeType;

    /** Filter by funding source code */
    private String fundingSource;

    /** Keyword search (matches owner name / patent name / copyright name) */
    private String keyword;

    /** Due date range start (inclusive) */
    private String dueDateFrom;

    /** Due date range end (inclusive) */
    private String dueDateTo;

    /** Filter by owner type (patent / copyright) */
    private String ownerType;

    /** Filter by department ID (injected from SecurityUtils for data isolation) */
    private Long deptId;
}
