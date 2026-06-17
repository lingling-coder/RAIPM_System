package com.institute.achievement.framework.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Search query parameters from the frontend.
 * <p>
 * The {@code keyword} field is required (@NotBlank). All other fields are
 * optional filters applied via MyBatis dynamic SQL. The {@code forceExcludeClassified}
 * field is set by the service layer based on the current user's role (D-16),
 * NOT from user input.
 */
@Data
public class SearchQueryDTO {

    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** Filter by achievement type: "paper", "patent", "software" */
    private String type;

    /** Filter by department ID (resolved from SecurityContext if null) */
    private Long deptId;

    /** Filter by minimum publish year */
    private Integer yearFrom;

    /** Filter by maximum publish year */
    private Integer yearTo;

    /** User-facing classification filter: "NORMAL" or "CLASSIFIED" */
    private String classification;

    /**
     * Internal flag set by service layer (D-16).
     * When true (non-CLASSIFIED_MANAGER users), forces SQL-level exclusion
     * of classified results regardless of user's classification filter.
     */
    private boolean forceExcludeClassified;

    /** Page number (1-based), default 1 */
    private int page = 1;

    /** Page size, default 10 */
    private int size = 10;

    /**
     * Calculate OFFSET for SQL LIMIT/OFFSET pagination.
     *
     * @return offset value
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
