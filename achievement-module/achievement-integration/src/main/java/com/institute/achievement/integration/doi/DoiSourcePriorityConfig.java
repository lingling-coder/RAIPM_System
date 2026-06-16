package com.institute.achievement.integration.doi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for DOI data source priority order.
 * <p>
 * Admin-configurable via application properties under "doi.source".
 * Default priority: crossref, openalex (Scopus deferred — requires paid API Key).
 * <p>
 * Usage: doi.source.priority=crossref,openalex
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "doi.source")
public class DoiSourcePriorityConfig {

    /** Ordered list of source keys (enum names in priority order) */
    private List<String> priority = new ArrayList<>(List.of("crossref", "openalex"));

    /**
     * Returns the list of DoiSourceEnum in configured priority order.
     */
    public List<DoiSourceEnum> getOrderedSources() {
        return priority.stream()
                .map(DoiSourceEnum::fromConfigKey)
                .toList();
    }
}
