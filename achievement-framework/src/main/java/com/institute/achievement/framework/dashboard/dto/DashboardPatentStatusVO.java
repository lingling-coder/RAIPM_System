package com.institute.achievement.framework.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * VO for patent status donut chart data.
 * <p>
 * Represents the count and percentage of patents grouped by legal
 * status category: VALID, INVALID, or UNKNOWN. Used by the patent
 * valid/invalid donut chart on the dashboard.
 */
@Data
public class DashboardPatentStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status category: 'VALID', 'INVALID', or 'UNKNOWN' */
    private String status;

    /** Human-readable label for the status category */
    private String label;

    /** Count of patents in this status category */
    private Long count;

    /** Percentage of total patents (0.0 ~ 100.0) */
    private Double percentage;
}
