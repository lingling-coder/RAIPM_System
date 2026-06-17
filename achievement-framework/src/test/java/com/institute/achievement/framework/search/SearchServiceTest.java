package com.institute.achievement.framework.search;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.framework.search.dto.SearchQueryDTO;
import com.institute.achievement.framework.search.dto.SearchResultVO;
import com.institute.achievement.framework.search.dto.SearchResultVO.HighlightRange;
import com.institute.achievement.framework.search.mapper.SearchMapper;
import com.institute.achievement.framework.search.service.SearchService;
import com.institute.achievement.framework.search.service.impl.SearchServiceImpl;
import com.institute.achievement.framework.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SearchServiceImpl}.
 * <p>
 * Covers search execution, empty results, deptId auto-resolution from
 * security context, D-16 classified exclusion for non-managers,
 * classified non-exclusion for managers, and highlight computation.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchMapper searchMapper;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchServiceImpl(searchMapper);
    }

    private SearchQueryDTO createDefaultQuery() {
        SearchQueryDTO query = new SearchQueryDTO();
        query.setKeyword("机器学习");
        query.setDeptId(100L);
        query.setPage(1);
        query.setSize(10);
        return query;
    }

    private SearchResultVO createSampleResult(Long id, String title, String authors, Double score) {
        return new SearchResultVO(
                id, title, "paper", "APPROVED",
                "计算机学院", authors, 2026,
                score, new ArrayList<>(), false
        );
    }

    // ── search() tests ─────────────────────────────────────────────────

    @Test
    void search_withResults_returnsPageResult() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();
        List<SearchResultVO> sampleResults = Arrays.asList(
                createSampleResult(1L, "机器学习入门", "张三; 李四", 10.5),
                createSampleResult(2L, "深度学习应用", "王五", 8.3)
        );

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(5L);
        when(searchMapper.search(any(SearchQueryDTO.class))).thenReturn(sampleResults);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Act
            PageResult<SearchResultVO> result = searchService.search(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(5);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getRecords()).hasSize(2);

            // Verify both mapper methods were called
            verify(searchMapper).countSearch(any(SearchQueryDTO.class));
            verify(searchMapper).search(any(SearchQueryDTO.class));
        }
    }

    @Test
    void search_withEmptyResults_returnsEmptyPage() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(0L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Act
            PageResult<SearchResultVO> result = searchService.search(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getRecords()).isEmpty();

            // Verify search mapper was NOT called (short circuit)
            verify(searchMapper).countSearch(any(SearchQueryDTO.class));
            verify(searchMapper, never()).search(any(SearchQueryDTO.class));
        }
    }

    @Test
    void search_withDeptIdNull_resolvesFromSecurityContext() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();
        query.setDeptId(null); // No deptId provided

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(1L);
        when(searchMapper.search(any(SearchQueryDTO.class)))
                .thenReturn(Collections.singletonList(createSampleResult(1L, "Test", "Author", 5.0)));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(200L);

            // Act
            searchService.search(query);

            // Assert: deptId should have been resolved to 200
            assertThat(query.getDeptId()).isEqualTo(200L);
        }
    }

    @Test
    void search_withExistingDeptId_doesNotOverride() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();
        query.setDeptId(300L); // Already set

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(1L);
        when(searchMapper.search(any(SearchQueryDTO.class)))
                .thenReturn(Collections.singletonList(createSampleResult(1L, "Test", "Author", 5.0)));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Act
            searchService.search(query);

            // Assert: deptId should remain 300 (not overridden from security context)
            assertThat(query.getDeptId()).isEqualTo(300L);
        }
    }

    // ── D-16 classified exclusion tests ────────────────────────────────

    @Test
    void search_nonClassifiedManager_forceExcludeClassifiedIsTrue() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(1L);
        when(searchMapper.search(any(SearchQueryDTO.class)))
                .thenReturn(Collections.singletonList(createSampleResult(1L, "Test", "Author", 5.0)));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Act
            searchService.search(query);

            // Assert: forceExcludeClassified should be true for non-manager
            assertThat(query.isForceExcludeClassified()).isTrue();
        }
    }

    @Test
    void search_classifiedManager_forceExcludeClassifiedIsFalse() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(1L);
        when(searchMapper.search(any(SearchQueryDTO.class)))
                .thenReturn(Collections.singletonList(createSampleResult(1L, "Test", "Author", 5.0)));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(true);

            // Act
            searchService.search(query);

            // Assert: forceExcludeClassified should be false for classified manager
            assertThat(query.isForceExcludeClassified()).isFalse();
        }
    }

    // ── computeHighlights() tests ──────────────────────────────────────

    @Test
    void computeHighlights_singleMatch_findsCorrectRange() {
        // Arrange
        String title = "机器学习入门";
        String authors = "张三";
        String keyword = "机器";

        // Act
        List<HighlightRange> ranges = searchService.computeHighlights(title, authors, keyword);

        // Assert
        assertThat(ranges).isNotEmpty();
        // "机器" starts at position 0 in "机器学习入门"
        assertThat(ranges).anyMatch(r ->
                "title".equals(r.getField()) && r.getStart() == 0 && r.getEnd() == 2
        );
    }

    @Test
    void computeHighlights_multipleMatches_allFound() {
        // Arrange
        String title = "机器学习与机器视觉";
        String authors = "张三";
        String keyword = "机器";

        // Act
        List<HighlightRange> ranges = searchService.computeHighlights(title, authors, keyword);

        // Assert: "机器" appears at positions (0,2) and (5,7) in "机器学习与机器视觉"
        assertThat(ranges).filteredOn(r -> "title".equals(r.getField())).hasSize(2);

        assertThat(ranges).anyMatch(r ->
                "title".equals(r.getField()) && r.getStart() == 0 && r.getEnd() == 2
        );
        assertThat(ranges).anyMatch(r ->
                "title".equals(r.getField()) && r.getStart() == 5 && r.getEnd() == 7
        );
    }

    @Test
    void computeHighlights_caseInsensitive_findsMatches() {
        // Arrange
        String title = "Machine Learning";
        String authors = "";
        String keyword = "machine";

        // Act
        List<HighlightRange> ranges = searchService.computeHighlights(title, authors, keyword);

        // Assert: case-insensitive match
        assertThat(ranges).anyMatch(r ->
                "title".equals(r.getField()) && r.getStart() == 0 && r.getEnd() == 7
        );
    }

    @Test
    void computeHighlights_noMatch_returnsEmpty() {
        // Arrange
        String title = "深度学习";
        String authors = "";
        String keyword = "机器";

        // Act
        List<HighlightRange> ranges = searchService.computeHighlights(title, authors, keyword);

        // Assert
        assertThat(ranges).isEmpty();
    }

    @Test
    void computeHighlights_nullTitleOrAuthors_doesNotThrow() {
        // Act & Assert: should not throw NPE
        assertThat(searchService.computeHighlights(null, null, "test")).isEmpty();
        assertThat(searchService.computeHighlights("title", null, "test")).isEmpty(); // "test" not in "title"
        assertThat(searchService.computeHighlights(null, "author", "test")).isEmpty(); // "test" not in "author"
        // With matching content
        assertThat(searchService.computeHighlights("机器学习", null, "机器")).isNotEmpty();
        assertThat(searchService.computeHighlights(null, "张三", "张三")).isNotEmpty();
    }

    @Test
    void computeHighlights_emptyKeyword_returnsEmpty() {
        assertThat(searchService.computeHighlights("title", "author", "")).isEmpty();
        assertThat(searchService.computeHighlights("title", "author", null)).isEmpty();
    }

    // ── page size enforcement test ─────────────────────────────────────

    @Test
    void search_exceedsMaxPageSize_enforcesLimit() {
        // Arrange
        SearchQueryDTO query = createDefaultQuery();
        query.setSize(1000); // Exceeds MAX_PAGE_SIZE (500)

        when(searchMapper.countSearch(any(SearchQueryDTO.class))).thenReturn(0L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole("CLASSIFIED_MANAGER")).thenReturn(false);

            // Act
            searchService.search(query);

            // Assert: page size should be capped at 500
            assertThat(query.getSize()).isEqualTo(500);
        }
    }
}
