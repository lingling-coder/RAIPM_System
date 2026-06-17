package com.institute.achievement.framework.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Unified search result VO across paper, patent, and software_copyright tables.
 * <p>
 * Each result represents one achievement matched by the FULLTEXT search query.
 * The {@code highlightRanges} field is computed by the service layer after
 * the SQL query returns, indicating character ranges for frontend highlighting.
 */
@Data
@AllArgsConstructor
public class SearchResultVO {

    private Long id;
    private String title;
    private String achievementType;
    private String status;
    private String deptName;
    private String authors;
    private Integer publishYear;
    private Double relevanceScore;
    private List<HighlightRange> highlightRanges;
    private Boolean isClassified;

    /**
     * Represents a range of characters in a text field that matched the search keyword.
     * Used by the frontend to render highlighted text spans.
     */
    @Data
    @AllArgsConstructor
    public static class HighlightRange {
        /** The field name where the match occurred: "title" or "authors" */
        private String field;
        /** Start character position (0-indexed) */
        private Integer start;
        /** End character position (exclusive) */
        private Integer end;
    }
}
