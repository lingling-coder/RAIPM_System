package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates DOI lookups across multiple data sources in configured priority order.
 * <p>
 * Implements D-14 (auto-fallback): if the primary source returns no result or throws,
 * the next source in priority order is queried automatically.
 * <p>
 * Implements D-11: source priority is globally configured (admin-managed, user-unaware).
 */
@Slf4j
@Service
public class DoiAutoFillService {

    private final CrossrefClient crossrefClient;
    private final OpenAlexClient openAlexClient;
    private final ChineseDoiClient chineseDoiClient;
    private final DoiSourcePriorityConfig priorityConfig;

    public DoiAutoFillService(CrossrefClient crossrefClient,
                              OpenAlexClient openAlexClient,
                              ChineseDoiClient chineseDoiClient,
                              DoiSourcePriorityConfig priorityConfig) {
        this.crossrefClient = crossrefClient;
        this.openAlexClient = openAlexClient;
        this.chineseDoiClient = chineseDoiClient;
        this.priorityConfig = priorityConfig;
    }

    /**
     * Look up a DOI across all configured sources in priority order.
     * Returns the first successful result. Falls back to next source on failure.
     *
     * @param doi the DOI to look up
     * @return lookup result (found=true if any source resolved it)
     */
    public DoiLookupResult lookup(String doi) {
        List<DoiSourceEnum> sources = priorityConfig.getOrderedSources();

        for (DoiSourceEnum source : sources) {
            try {
                log.debug("Looking up DOI {} from {}", doi, source);
                Optional<DoiLookupResult> result = lookupFromSource(source, doi);
                if (result.isPresent() && result.get().isFound()) {
                    log.info("DOI {} resolved by {}", doi, source);
                    return result.get();
                }
            } catch (Exception e) {
                log.warn("DOI lookup failed from {}: {}", source, e.getMessage());
                // Continue to fallback source (D-14)
            }
        }

        log.warn("DOI {} could not be resolved from CrossRef/OpenAlex, trying Chinese DOI resolver", doi);

        // Final fallback: Chinese DOI infrastructure (CNKI/ISTIC via doi.org HTML)
        try {
            Optional<DoiLookupResult> chineseResult = chineseDoiClient.resolve(doi);
            if (chineseResult.isPresent() && chineseResult.get().isFound()) {
                log.info("DOI {} resolved by Chinese DOI resolver", doi);
                return chineseResult.get();
            }
        } catch (Exception e) {
            log.warn("Chinese DOI resolution also failed for {}: {}", doi, e.getMessage());
        }

        log.warn("DOI {} could not be resolved from any source (CrossRef, OpenAlex, Chinese)", doi);
        return DoiLookupResult.notFound(doi);
    }

    /**
     * Search for publications by title and optional authors.
     * Returns up to candidatesPerSource results from the primary source.
     * Falls back to secondary sources only if the primary returns zero results.
     *
     * @param title              paper title (required)
     * @param authors            optional author names for query refinement
     * @param candidatesPerSource max results per source (suggested: 5)
     * @return list of matched candidates from all sources
     */
    public List<DoiLookupResult> search(String title, String authors, int candidatesPerSource) {
        List<DoiLookupResult> allResults = new ArrayList<>();

        for (DoiSourceEnum source : priorityConfig.getOrderedSources()) {
            try {
                log.debug("Searching for title='{}' authors='{}' from {}", title, authors, source);
                List<DoiLookupResult> results = searchFromSource(source, title, authors, candidatesPerSource);
                if (!results.isEmpty()) {
                    allResults.addAll(results);
                    // If primary source returns enough, stop here
                    if (allResults.size() >= candidatesPerSource) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Title search failed from {}: {}", source, e.getMessage());
            }
        }

        // Limit to requested count and deduplicate by DOI
        return allResults.stream()
                .filter(r -> r.getDoi() != null)
                .collect(Collectors.toMap(
                        DoiLookupResult::getDoi,
                        r -> r,
                        (a, b) -> a))
                .values().stream()
                .limit(candidatesPerSource)
                .collect(Collectors.toList());
    }

    private List<DoiLookupResult> searchFromSource(DoiSourceEnum source, String title, String authors, int rows) {
        return switch (source) {
            case CROSSREF -> crossrefClient.searchByTitle(title, authors, rows);
            case OPENALEX -> openAlexClient.searchByTitle(title, authors, rows);
        };
    }

    /**
     * Route lookup to the appropriate client based on source.
     */
    private Optional<DoiLookupResult> lookupFromSource(DoiSourceEnum source, String doi) {
        return switch (source) {
            case CROSSREF -> crossrefClient.lookup(doi);
            case OPENALEX -> openAlexClient.lookup(doi);
        };
    }
}
