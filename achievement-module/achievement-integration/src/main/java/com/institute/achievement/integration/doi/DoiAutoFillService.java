package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
    private final DoiSourcePriorityConfig priorityConfig;

    public DoiAutoFillService(CrossrefClient crossrefClient,
                              OpenAlexClient openAlexClient,
                              DoiSourcePriorityConfig priorityConfig) {
        this.crossrefClient = crossrefClient;
        this.openAlexClient = openAlexClient;
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

        log.warn("DOI {} could not be resolved from any source", doi);
        return DoiLookupResult.notFound(doi);
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
