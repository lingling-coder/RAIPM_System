package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.DoiSourceEnum;
import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DoiAutoFillService covering multi-source priority lookup,
 * automatic fallback, and all-sources-failure scenarios.
 */
@ExtendWith(MockitoExtension.class)
class DoiAutoFillServiceTest {

    @Mock
    private CrossrefClient crossrefClient;

    @Mock
    private OpenAlexClient openAlexClient;

    @Mock
    private ChineseDoiClient chineseDoiClient;

    @Mock
    private DoiSourcePriorityConfig priorityConfig;

    private DoiAutoFillService doiAutoFillService;

    @BeforeEach
    void setUp() {
        doiAutoFillService = new DoiAutoFillService(crossrefClient, openAlexClient, chineseDoiClient, priorityConfig);
    }

    @Test
    void testLookup_Success_FromPrimary() {
        // Arrange
        String doi = "10.1038/nature12373";
        DoiLookupResult primaryResult = new DoiLookupResult();
        primaryResult.setFound(true);
        primaryResult.setDoi(doi);
        primaryResult.setTitle("Nature Article");

        when(priorityConfig.getOrderedSources()).thenReturn(List.of(DoiSourceEnum.CROSSREF, DoiSourceEnum.OPENALEX));
        when(crossrefClient.lookup(doi)).thenReturn(Optional.of(primaryResult));

        // Act
        DoiLookupResult result = doiAutoFillService.lookup(doi);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getTitle()).isEqualTo("Nature Article");
        verify(crossrefClient).lookup(doi);
        verify(openAlexClient, never()).lookup(anyString());
    }

    @Test
    void testLookup_Fallback_WhenPrimaryThrows() {
        // Arrange
        String doi = "10.1038/nature12373";
        DoiLookupResult fallbackResult = new DoiLookupResult();
        fallbackResult.setFound(true);
        fallbackResult.setDoi(doi);
        fallbackResult.setTitle("Fallback Article");

        when(priorityConfig.getOrderedSources()).thenReturn(List.of(DoiSourceEnum.CROSSREF, DoiSourceEnum.OPENALEX));
        when(crossrefClient.lookup(doi)).thenThrow(new RuntimeException("Primary source timeout"));
        when(openAlexClient.lookup(doi)).thenReturn(Optional.of(fallbackResult));

        // Act
        DoiLookupResult result = doiAutoFillService.lookup(doi);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getTitle()).isEqualTo("Fallback Article");
        verify(crossrefClient).lookup(doi);
        verify(openAlexClient).lookup(doi);
    }

    @Test
    void testLookup_AllFail_ReturnsNotFound() {
        // Arrange
        String doi = "10.1234/nonexistent";
        when(priorityConfig.getOrderedSources()).thenReturn(List.of(DoiSourceEnum.CROSSREF, DoiSourceEnum.OPENALEX));
        when(crossrefClient.lookup(doi)).thenReturn(Optional.empty());
        when(openAlexClient.lookup(doi)).thenReturn(Optional.empty());

        // Act
        DoiLookupResult result = doiAutoFillService.lookup(doi);

        // Assert
        assertThat(result.isFound()).isFalse();
        assertThat(result.getDoi()).isEqualTo(doi);
    }

    @Test
    void testLookup_PriorityOrder_PrimaryCheckedFirst() {
        // Arrange
        String doi = "10.1038/nature12373";

        when(priorityConfig.getOrderedSources()).thenReturn(List.of(DoiSourceEnum.OPENALEX, DoiSourceEnum.CROSSREF));

        DoiLookupResult primaryResult = new DoiLookupResult();
        primaryResult.setFound(true);
        primaryResult.setDoi(doi);

        when(openAlexClient.lookup(doi)).thenReturn(Optional.of(primaryResult));

        // Act
        DoiAutoFillService service = new DoiAutoFillService(crossrefClient, openAlexClient, chineseDoiClient, priorityConfig);
        DoiLookupResult result = service.lookup(doi);

        // Assert
        assertThat(result.isFound()).isTrue();
        verify(openAlexClient).lookup(doi);
        verify(crossrefClient, never()).lookup(anyString());
    }
}
