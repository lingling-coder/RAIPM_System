package com.institute.achievement.framework.search.service.impl;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.framework.search.dto.SearchQueryDTO;
import com.institute.achievement.framework.search.dto.SearchResultVO;
import com.institute.achievement.framework.search.dto.SearchResultVO.HighlightRange;
import com.institute.achievement.framework.search.mapper.SearchMapper;
import com.institute.achievement.framework.search.service.SearchService;
import com.institute.achievement.framework.search.util.SearchQuerySanitizer;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service implementation for full-text search across achievement tables.
 * <p>
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-03-01: keyword sanitized via SearchQuerySanitizer before SQL</li>
 *   <li>T-3-03-03: D-16 classified exclusion — non-CLASSIFIED_MANAGER users
 *       get forceExcludeClassified=true</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchMapper searchMapper;

    /**
     * Maximum allowed page size to prevent excessive data retrieval.
     */
    private static final int MAX_PAGE_SIZE = 500;

    @Override
    public PageResult<SearchResultVO> search(SearchQueryDTO query) {
        // Resolve deptId from security context if not provided
        if (query.getDeptId() == null) {
            Long deptId = SecurityUtils.getCurrentDeptId();
            query.setDeptId(deptId);
            log.debug("Search deptId resolved from security context: {}", deptId);
        }

        // D-16 classified exclusion: non-CLASSIFIED_MANAGER users cannot see classified results
        boolean isClassifiedManager = SecurityUtils.hasRole("classified_admin");
        if (!isClassifiedManager) {
            query.setForceExcludeClassified(true);
            log.debug("User is not CLASSIFIED_MANAGER — classified results excluded (D-16)");
        }

        // Enforce page size limit
        if (query.getSize() > MAX_PAGE_SIZE) {
            query.setSize(MAX_PAGE_SIZE);
        }

        // Get total count
        long total = searchMapper.countSearch(query);
        log.debug("Search count: {} results for keyword '{}'", total, query.getKeyword());

        // If no results, return empty page
        if (total == 0) {
            return PageResult.empty();
        }

        // Get paginated results
        List<SearchResultVO> records = searchMapper.search(query);
        log.debug("Search returned {} records (page={}, size={})", records.size(), query.getPage(), query.getSize());

        // Compute highlight ranges for each result
        for (SearchResultVO record : records) {
            List<HighlightRange> highlights = computeHighlights(
                    record.getTitle(), record.getAuthors(), query.getKeyword());
            record.setHighlightRanges(highlights);
        }

        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    @Override
    public List<HighlightRange> computeHighlights(String title, String authors, String keyword) {
        List<HighlightRange> ranges = new ArrayList<>();

        // Extract individual search terms
        String[] terms = SearchQuerySanitizer.extractKeywords(keyword);
        if (terms.length == 0) {
            return ranges;
        }

        // Find matches in title
        if (title != null && !title.isEmpty()) {
            for (String term : terms) {
                if (term.isEmpty()) continue;
                // Build case-insensitive pattern, removing trailing * wildcard from sanitizer
                String searchTerm = term.endsWith("*") ? term.substring(0, term.length() - 1) : term;
                if (searchTerm.isEmpty()) continue;

                Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(title);
                while (matcher.find()) {
                    ranges.add(new HighlightRange("title", matcher.start(), matcher.end()));
                }
            }
        }

        // Find matches in authors
        if (authors != null && !authors.isEmpty()) {
            for (String term : terms) {
                if (term.isEmpty()) continue;
                String searchTerm = term.endsWith("*") ? term.substring(0, term.length() - 1) : term;
                if (searchTerm.isEmpty()) continue;

                Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(authors);
                while (matcher.find()) {
                    ranges.add(new HighlightRange("authors", matcher.start(), matcher.end()));
                }
            }
        }

        return ranges;
    }
}
