package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client for the Crossref REST API (https://api.crossref.org/works/{doi}).
 * <p>
 * Fetches publication metadata by DOI. Parses JSON response into DoiLookupResult.
 * 5-second timeout per request.
 */
@Slf4j
@Component
public class CrossrefClient {

    private static final String API_BASE = "https://api.crossref.org/works/";
    private static final String MAILTO = "?mailto=admin@institute.cn";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrossrefClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Look up a DOI on Crossref.
     *
     * @param doi the DOI to look up
     * @return Optional containing the result if found, empty if not
     */
    public Optional<DoiLookupResult> lookup(String doi) {
        try {
            String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8);
            String url = API_BASE + encodedDoi + MAILTO;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Crossref returned status {} for DOI: {}", response.statusCode(), doi);
                return Optional.empty();
            }

            return Optional.of(parseResponse(response.body(), doi));

        } catch (Exception e) {
            log.warn("Crossref lookup failed for DOI {}: {}", doi, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Search by Title + Author (D-REQ: intelligent matching) ──────────

    private static final String SEARCH_API = "https://api.crossref.org/works";

    /**
     * Search Crossref by title and optional authors, returning up to {@code rows} candidates.
     * <p>
     * The query is constructed as: {@code query=title + " " + authors}.
     * Crossref scores results by relevance; the top N are returned.
     *
     * @param title   paper title (required, used as primary query)
     * @param authors optional author names to refine the query
     * @param rows    max results to return (suggested: 5)
     * @return list of DoiLookupResult candidates (empty if none found)
     */
    public List<DoiLookupResult> searchByTitle(String title, String authors, int rows) {
        try {
            StringBuilder query = new StringBuilder(title.trim());
            if (authors != null && !authors.trim().isEmpty()) {
                query.append(" ").append(authors.trim());
            }
            String encodedQuery = URLEncoder.encode(query.toString(), StandardCharsets.UTF_8);
            String url = SEARCH_API + "?query=" + encodedQuery + "&rows=" + rows + "&mailto=admin@institute.cn";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Crossref search returned status {} for query: {}", response.statusCode(), query);
                return List.of();
            }

            return parseSearchResponse(response.body());

        } catch (Exception e) {
            log.warn("Crossref search failed for query '{}': {}", title, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse Crossref search response (list of works) into DoiLookupResult list.
     */
    private List<DoiLookupResult> parseSearchResponse(String jsonBody) {
        List<DoiLookupResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode items = root.path("message").path("items");

            if (items.isArray()) {
                int rank = 0;
                for (JsonNode item : items) {
                    if (rank >= 10) break; // Safety cap
                    String doi = item.path("DOI").asText(null);
                    if (doi == null || doi.isEmpty()) {
                        rank++;
                        continue;
                    }
                    // Reuse single-work parsing: wrap item in a fake message envelope
                    String fakeResponse = objectMapper.createObjectNode()
                            .set("message", item)
                            .toString();
                    DoiLookupResult result = parseResponse(fakeResponse, doi);
                    if (result.isFound()) {
                        results.add(result);
                    }
                    rank++;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Crossref search response: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Parse Crossref JSON response into DoiLookupResult.
     */
    private DoiLookupResult parseResponse(String jsonBody, String doi) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode message = root.path("message");

            DoiLookupResult result = new DoiLookupResult();
            result.setFound(true);
            result.setDoi(doi);

            // Title: message.title[0]
            JsonNode titleNode = message.path("title");
            if (titleNode.isArray() && titleNode.size() > 0) {
                result.setTitle(titleNode.get(0).asText());
            }

            // Authors: message.author[].given + family
            StringBuilder authors = new StringBuilder();
            JsonNode authorArray = message.path("author");
            if (authorArray.isArray()) {
                for (int i = 0; i < authorArray.size(); i++) {
                    if (i > 0) authors.append("; ");
                    JsonNode author = authorArray.get(i);
                    String given = author.path("given").asText("");
                    String family = author.path("family").asText("");
                    if (!given.isEmpty() && !family.isEmpty()) {
                        authors.append(given).append(" ").append(family);
                    } else if (!family.isEmpty()) {
                        authors.append(family);
                    } else if (!given.isEmpty()) {
                        authors.append(given);
                    }
                }
            }
            if (!authors.isEmpty()) {
                result.setAuthors(authors.toString());
            }

            // Journal: message.container-title[0]
            JsonNode containerTitle = message.path("container-title");
            if (containerTitle.isArray() && containerTitle.size() > 0) {
                result.setJournal(containerTitle.get(0).asText());
            }

            // Volume
            result.setVolume(message.path("volume").asInt(0));
            if (result.getVolume() == 0) result.setVolume(null);

            // Issue
            result.setIssue(message.path("issue").asInt(0));
            if (result.getIssue() == 0) result.setIssue(null);

            // Pages
            result.setPages(message.path("page").asText(null));

            // Publication year from published-print.date-parts[0][0]
            JsonNode printDate = message.path("published-print").path("date-parts");
            if (printDate.isArray() && printDate.size() > 0) {
                JsonNode parts = printDate.get(0);
                if (parts.isArray() && parts.size() > 0) {
                    result.setPublishYear(parts.get(0).asInt());
                }
            }

            // Abstract (may contain HTML tags — strip them)
            String abstractText = message.path("abstract").asText(null);
            if (abstractText != null) {
                // Strip basic HTML tags
                abstractText = abstractText.replaceAll("<[^>]*>", "").trim();
                result.setAbstractText(abstractText);
            }

            return result;

        } catch (Exception e) {
            log.warn("Failed to parse Crossref response for DOI {}: {}", doi, e.getMessage());
            DoiLookupResult result = new DoiLookupResult();
            result.setFound(false);
            result.setDoi(doi);
            return result;
        }
    }
}
