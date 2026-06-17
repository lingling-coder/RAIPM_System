package com.institute.achievement.framework.search.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Static utility for sanitizing user search keywords for MySQL BOOLEAN MODE FULLTEXT search.
 * <p>
 * MySQL's {@code MATCH...AGAINST IN BOOLEAN MODE} interprets special characters
 * ({@code + - < > ( ) ~ * "}) as search operators. This class strips those
 * operators to prevent unexpected query behavior (Pitfall 3 from RESEARCH.md).
 * <p>
 * Additionally, single-character tokens are suffixed with {@code *} (wildcard) to
 * work around MySQL ngram's default {@code ngram_token_size=2} which does not
 * index single characters (Pitfall 2 from RESEARCH.md).
 * <p>
 * <h3>Usage</h3>
 * <pre>{@code
 * // In MyBatis XML:
 * // <bind name="sanitizedKeyword"
 * //   value="@com.institute.achievement.framework.search.util.SearchQuerySanitizer@toBooleanModeQuery(keyword)" />
 * }</pre>
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-03-01: BOOLEAN MODE operators stripped, preventing keyword tampering</li>
 *   <li>T-3-03-04: Deterministic static methods — no runtime code injection via OGNL</li>
 * </ul>
 */
public final class SearchQuerySanitizer {

    private static final Pattern BOOLEAN_OPERATORS =
            Pattern.compile("[+\\-<>()~*\"]");

    private SearchQuerySanitizer() {
        // Utility class, prevent instantiation
    }

    /**
     * Sanitize raw user input for safe use in BOOLEAN MODE search.
     * <p>
     * Processing steps:
     * <ol>
     *   <li>Strip BOOLEAN MODE special characters (replace with space)</li>
     *   <li>Normalize whitespace (collapse multiple spaces to one)</li>
     *   <li>Truncate to 100 characters max</li>
     *   <li>Append {@code *} wildcard suffix to single-character tokens</li>
     * </ol>
     *
     * @param keyword raw user input
     * @return sanitized keyword string, or empty string if null/blank
     */
    public static String sanitize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        // Remove boolean operators (T-3-03-01)
        String clean = BOOLEAN_OPERATORS.matcher(keyword.trim()).replaceAll(" ");
        // Normalize whitespace
        clean = clean.replaceAll("\\s+", " ");
        // Truncate to 100 chars
        if (clean.length() > 100) {
            clean = clean.substring(0, 100);
        }
        // Split into tokens and add wildcard suffix for single-char tokens (Pitfall 2)
        String[] tokens = clean.split(" ");
        return Arrays.stream(tokens)
                .filter(t -> !t.isEmpty())
                .map(t -> t.length() == 1 ? t + "*" : t)
                .collect(Collectors.joining(" "));
    }

    /**
     * Convert sanitized keyword to a BOOLEAN MODE query with AND semantics.
     * <p>
     * Each token is prefixed with {@code +} (must be present) for intersection
     * behavior. Returns empty string if sanitized input is empty.
     *
     * @param keyword raw user input
     * @return BOOLEAN MODE query string, or empty string if no valid tokens
     */
    public static String toBooleanModeQuery(String keyword) {
        String sanitized = sanitize(keyword);
        if (sanitized.isEmpty()) {
            return "";
        }
        return Arrays.stream(sanitized.split(" "))
                .map(t -> "+" + t)
                .collect(Collectors.joining(" "));
    }

    /**
     * Extract individual search terms from a sanitized keyword.
     * <p>
     * Used by the service layer to compute highlight positions in result text.
     *
     * @param sanitizedKeyword pre-sanitized keyword
     * @return array of individual terms, or empty array if null/blank
     */
    public static String[] extractKeywords(String sanitizedKeyword) {
        if (sanitizedKeyword == null || sanitizedKeyword.isBlank()) {
            return new String[0];
        }
        return sanitizedKeyword.split("\\s+");
    }
}
