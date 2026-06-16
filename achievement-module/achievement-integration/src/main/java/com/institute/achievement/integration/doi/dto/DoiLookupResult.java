package com.institute.achievement.integration.doi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a DOI lookup, wrapping metadata fetched from external sources
 * (Crossref, OpenAlex, etc.) and a found/not-found indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoiLookupResult {

    /** Whether the DOI was resolved to a publication */
    private boolean found;

    /** The DOI that was looked up */
    private String doi;

    /** Paper title */
    private String title;

    /** Authors as semicolon-separated string */
    private String authors;

    /** Journal or conference name */
    private String journal;

    /** Volume number */
    private Integer volume;

    /** Issue number */
    private Integer issue;

    /** Pages (e.g. "123-130") */
    private String pages;

    /** Publication year */
    private Integer publishYear;

    /** Abstract text */
    private String abstractText;

    /**
     * Factory method for a not-found result.
     */
    public static DoiLookupResult notFound(String doi) {
        DoiLookupResult result = new DoiLookupResult();
        result.setFound(false);
        result.setDoi(doi);
        return result;
    }
}
