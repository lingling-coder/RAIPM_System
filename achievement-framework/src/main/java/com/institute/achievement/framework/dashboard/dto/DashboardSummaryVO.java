package com.institute.achievement.framework.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Dashboard summary statistics VO.
 * <p>
 * Carries total counts for users, departments, and roles
 * displayed on the homepage stat cards.
 */
@Data
public class DashboardSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total active user count (deleted=0). */
    private long totalUsers;

    /** Total active department count (deleted=0). */
    private long totalDepts;

    /** Total active role count (deleted=0). */
    private long totalRoles;
}
