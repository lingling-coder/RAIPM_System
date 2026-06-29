package com.institute.achievement.framework.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * VO for type distribution pie chart data.
 * <p>
 * Represents the count and percentage of achievements by type
 * (paper, patent, software copyright). Used by the type distribution
 * pie chart on the dashboard.
 */
@Data
public class DashboardTypeDistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Achievement type discriminator: 'paper', 'patent', or 'software' */
    private String achievementType;

    /** Count of achievements of this type */
    private Long count;

    /** Percentage of total achievements (0.0 ~ 100.0) */
    private Double percentage;
}
