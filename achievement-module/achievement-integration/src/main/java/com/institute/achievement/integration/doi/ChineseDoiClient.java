package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for resolving Chinese DOIs via doi.org HTML landing pages.
 * <p>
 * Chinese journals registered through CNKI/ISTIC do not support the standard
 * content-negotiation (application/vnd.citationstyles.csl+json). Instead,
 * doi.org returns a "多重解析地址选择页面" HTML page containing structured
 * metadata in a predictable table format.
 * <p>
 * This client parses that HTML to extract title, authors, journal, year, etc.
 * Used as a last-resort fallback after CrossRef and OpenAlex fail.
 */
@Slf4j
@Component
public class ChineseDoiClient {

    private static final String DOI_RESOLVER = "https://doi.org/";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    /** Pattern to detect Chinese DOI landing page */
    private static final Pattern CHINESE_PAGE_PATTERN =
            Pattern.compile("多重解析地址选择页面");

    private final HttpClient httpClient;

    public ChineseDoiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /**
     * Attempt to resolve a DOI via the Chinese DOI infrastructure.
     * Returns empty if the page is not a recognizable Chinese DOI page
     * or if parsing fails.
     *
     * @param doi the DOI to resolve
     * @return Optional DoiLookupResult with extracted metadata
     */
    public Optional<DoiLookupResult> resolve(String doi) {
        try {
            String url = DOI_RESOLVER + doi;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("User-Agent", "Mozilla/5.0 (compatible; AchievementSystem/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.debug("Chinese DOI resolver returned status {} for DOI: {}", response.statusCode(), doi);
                return Optional.empty();
            }

            String html = response.body();

            // Only parse if this looks like a Chinese DOI page
            if (!CHINESE_PAGE_PATTERN.matcher(html).find()) {
                log.debug("DOI {} did not resolve to a Chinese DOI page", doi);
                return Optional.empty();
            }

            return Optional.of(parseChinesePage(html, doi));

        } catch (Exception e) {
            log.warn("Chinese DOI resolution failed for {}: {}", doi, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse the Chinese DOI HTML landing page to extract metadata.
     * <p>
     * The page structure is predictable: a table with &lt;label&gt;key&lt;/label&gt;value rows.
     * We extract: 题名→title, 作者→authors, 来源→journal,
     * 出版年→year, DOI码→doi.
     */
    private DoiLookupResult parseChinesePage(String html, String doi) {
        DoiLookupResult result = new DoiLookupResult();
        result.setFound(true);
        result.setDoi(doi);

        // Title: <label>题名：</label>VALUE
        String title = extractField(html, "题名[：:]\\s*</label>\\s*([^<]+)");
        result.setTitle(title);

        // Authors: <label>作者：</label>VALUE
        String authors = extractField(html, "作者[：:]\\s*</label>\\s*([^<]+)");
        if (authors != null && !authors.isEmpty()) {
            // CNKI uses semicolons, normalize to semicolons
            authors = authors.replace("；", ";");
            result.setAuthors(authors);
        }

        // Journal/Source: <label>来源：</label>VALUE
        String source = extractField(html, "来源[：:]\\s*</label>\\s*([^<]*)");
        if (source != null && !source.isEmpty()) {
            result.setJournal(source);
        }

        // Publication year: <label>出版年:</label>VALUE
        String pubYear = extractField(html, "出版年[：:]\\s*</label>\\s*([^<]*)");
        if (pubYear != null && !pubYear.isEmpty()) {
            try {
                result.setPublishYear(Integer.parseInt(pubYear.trim()));
            } catch (NumberFormatException e) {
                log.debug("Could not parse publication year: {}", pubYear);
            }
        }

        // Also try to extract year from CNKI link URL: .../detail/.../YYYY-...
        if (result.getPublishYear() == null) {
            result.setPublishYear(extractYearFromLinks(html));
        }

        // Registration time can serve as a proxy for publication date
        // <label>注册时间：</label>2024-06-21 11:31:17
        if (result.getPublishYear() == null) {
            String regTime = extractField(html, "注册时间[：:]\\s*</label>\\s*([^<]+)");
            if (regTime != null && regTime.length() >= 4) {
                try {
                    result.setPublishYear(Integer.parseInt(regTime.substring(0, 4)));
                } catch (NumberFormatException e) {
                    log.debug("Could not parse year from registration time: {}", regTime);
                }
            }
        }

        log.debug("Chinese DOI parsed: title={}, authors={}, year={}",
                result.getTitle(), result.getAuthors(), result.getPublishYear());

        return result;
    }

    /**
     * Extract a field value using a regex pattern from HTML.
     * Returns null if no match found or the matched group is empty/whitespace only.
     */
    private String extractField(String html, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value != null) {
                value = value.trim();
                // Decode HTML entities
                value = value.replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace("&nbsp;", " ");
                return !value.isEmpty() ? value : null;
            }
        }
        return null;
    }

    /**
     * Try to extract publication year from CNKI link URLs in the page.
     * CNKI detail links often contain year info in the URL path.
     */
    private Integer extractYearFromLinks(String html) {
        // Look for year patterns in CNKI URLs like /detail/.../2024-...
        Pattern yearPattern = Pattern.compile("/detail/[^/]+/(\\d{4})-");
        Matcher matcher = yearPattern.matcher(html);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
