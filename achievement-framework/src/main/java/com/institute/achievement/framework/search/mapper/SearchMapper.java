package com.institute.achievement.framework.search.mapper;

import com.institute.achievement.framework.search.dto.SearchQueryDTO;
import com.institute.achievement.framework.search.dto.SearchResultVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis mapper for UNION ALL + FULLTEXT search queries across
 * paper, patent, and software_copyright tables.
 * <p>
 * <strong>Important:</strong> Do NOT add {@code @DataScope} annotation here.
 * The DataPermissionInterceptor cannot handle UNION ALL queries (Pitfall 1 from
 * RESEARCH.md). Department-level filtering is inlined in each UNION branch
 * explicitly via MyBatis dynamic SQL.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-03-02: Inline dept_id filter in each UNION branch prevents
 *       cross-department data leakage</li>
 *   <li>T-3-03-03: D-16 classified exclusion handled via forceExcludeClassified
 *       flag set by SearchServiceImpl</li>
 * </ul>
 */
@Mapper
public interface SearchMapper {

    /**
     * Execute a UNION ALL FULLTEXT search across paper, patent, and copyright tables.
     * Results are sorted by relevance score DESC with pagination.
     *
     * @param query search query parameters (keyword, filters, pagination)
     * @return list of matched results with relevance score
     */
    List<SearchResultVO> search(SearchQueryDTO query);

    /**
     * Count total matching results for the given query (no pagination).
     * Used for pagination total calculation.
     *
     * @param query search query parameters (same as search() but without pagination)
     * @return total match count
     */
    long countSearch(SearchQueryDTO query);
}
