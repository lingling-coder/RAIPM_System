package com.institute.achievement.integration.doi;

import lombok.Getter;

/**
 * Enumeration of supported DOI data sources.
 * Priority ordering is configured externally via DoiSourcePriorityConfig.
 */
@Getter
public enum DoiSourceEnum {

    CROSSREF("Crossref"),
    OPENALEX("OpenAlex");

    private final String displayName;

    DoiSourceEnum(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Resolve from configuration key (case-insensitive).
     */
    public static DoiSourceEnum fromConfigKey(String key) {
        for (DoiSourceEnum source : values()) {
            if (source.name().equalsIgnoreCase(key)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown DOI source: " + key);
    }
}
