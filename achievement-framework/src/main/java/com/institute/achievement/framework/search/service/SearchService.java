package com.institute.achievement.framework.search.service;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.framework.search.dto.SearchQueryDTO;
import com.institute.achievement.framework.search.dto.SearchResultVO;
import com.institute.achievement.framework.search.dto.SearchResultVO.HighlightRange;

import java.util.List;

/**
 * Search service for full-text search across paper, patent, and copyright tables.
 * <p>
 * Handles keyword sanitization, classified data exclusion (D-16), department
 * ID resolution from security context, and highlight computation.
 */
public interface SearchService {

    /**
     * Execute a full-text search and return paginated results.
     *
     * @param query search parameters including keyword, filters, and pagination
     * @return paginated search results with highlight ranges
     */
    PageResult<SearchResultVO> search(SearchQueryDTO query);

    /**
     * Compute highlight ranges for a given text and keyword.
     * <p>
     * Finds all occurrences of individual search terms in the title and authors
     * text, returning their character positions for frontend highlighting (D-13).
     *
     * @param title   the achievement title
     * @param authors the achievement authors/inventors
     * @param keyword the original search keyword (before sanitization)
     * @return list of highlight ranges with field, start, and end positions
     */
    List<HighlightRange> computeHighlights(String title, String authors, String keyword);
}
