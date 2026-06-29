package com.institute.achievement.framework.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * VO for annual trend chart data.
 * <p>
 * Represents the count of achievements registered in a given year,
 * grouped by achievement type (paper, patent, software copyright).
 * Used by the annual trend line chart on the dashboard.
 */
@Data
@AllArgsConstructor
public class DashboardTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The year of achievement registration/publication */
    private Integer year;

    /** Achievement type discriminator: 'paper', 'patent', or 'software' */
    private String achievementType;

    /** Count of achievements matching this year and type */
    private Long count;
}
