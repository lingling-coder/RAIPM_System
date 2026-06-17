package com.institute.achievement.framework.search.controller;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.search.dto.SearchQueryDTO;
import com.institute.achievement.framework.search.dto.SearchResultVO;
import com.institute.achievement.framework.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for global full-text achievement search.
 * <p>
 * Provides a single GET endpoint at {@code /api/search} that accepts a required
 * keyword and optional filters. Returns paginated results with relevance
 * scoring and highlight positions for frontend rendering.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-03-01: Keyword sanitized by SearchQuerySanitizer before reaching SQL</li>
 *   <li>T-3-03-03: D-16 classified exclusion enforced by SearchServiceImpl</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Execute a full-text search across paper, patent, and copyright tables.
     * <p>
     * The keyword is required and sanitized by the service layer for safe
     * BOOLEAN MODE usage. Results are sorted by relevance score DESC.
     *
     * @param keyword     search keyword (required, sanitized by backend)
     * @param type        filter by achievement type (optional): "paper", "patent", "software"
     * @param deptId      filter by department ID (optional, resolved from security context if null)
     * @param yearFrom    filter by minimum publish year (optional)
     * @param yearTo      filter by maximum publish year (optional)
     * @param classification filter by classification level (optional): "NORMAL" or "CLASSIFIED"
     * @param page        page number (1-based, default 1)
     * @param size        page size (default 10, max 500)
     * @return paginated search results wrapped in Result
     */
    @GetMapping
    public Result<PageResult<SearchResultVO>> search(
            @RequestParam @Valid String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) String classification,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Build query DTO
        SearchQueryDTO query = new SearchQueryDTO();
        query.setKeyword(keyword);
        query.setType(type);
        query.setDeptId(deptId);
        query.setYearFrom(yearFrom);
        query.setYearTo(yearTo);
        query.setClassification(classification);
        query.setPage(page);
        query.setSize(size);

        // Execute search (classified exclusion handled in service layer)
        PageResult<SearchResultVO> pageResult = searchService.search(query);

        log.debug("Search '{}' returned {} total results (page={}, size={})",
                keyword, pageResult.getTotal(), page, size);

        return Result.success(pageResult);
    }
}
