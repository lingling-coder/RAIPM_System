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
import java.util.*;

/**
 * Client for the OpenAlex Works API (https://api.openalex.org/works/doi:{doi}).
 * <p>
 * OpenAlex is a free, open-source index of scholarly works.
 * Provides richer metadata than Crossref for some use cases.
 * 5-second timeout per request.
 */
@Slf4j
@Component
public class OpenAlexClient {

    private static final String API_BASE = "https://api.openalex.org/works/doi:";
    private static final String SEARCH_API = "https://api.openalex.org/works";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAlexClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Look up a DOI on OpenAlex.
     *
     * @param doi the DOI to look up
     * @return Optional containing the result if found, empty if not
     */
    public Optional<DoiLookupResult> lookup(String doi) {
        try {
            String url = API_BASE + doi;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("OpenAlex returned status {} for DOI: {}", response.statusCode(), doi);
                return Optional.empty();
            }

            return Optional.of(parseResponse(response.body(), doi));

        } catch (Exception e) {
            log.warn("OpenAlex lookup failed for DOI {}: {}", doi, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Search by Title + Author ──────────────────────────────────────────

    /**
     * Search OpenAlex by title and optional authors, returning up to {@code rows} candidates.
     * <p>
     * OpenAlex indexes a broader range of international and regional journals than Crossref,
     * including some Chinese/Asian publications.
     *
     * @param title   paper title (required)
     * @param authors optional author names for query refinement
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
            String url = SEARCH_API + "?search=" + encodedQuery + "&per_page=" + rows;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("OpenAlex search returned status {} for query: {}", response.statusCode(), query);
                return List.of();
            }

            return parseSearchResponse(response.body());

        } catch (Exception e) {
            log.warn("OpenAlex search failed for query '{}': {}", title, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse OpenAlex search response (list of works) into DoiLookupResult list.
     */
    private List<DoiLookupResult> parseSearchResponse(String jsonBody) {
        List<DoiLookupResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode items = root.path("results");

            if (items.isArray()) {
                for (JsonNode work : items) {
                    // Extract DOI from the work's "doi" field (full URL like https://doi.org/10.xxx/xxx)
                    String doiUrl = work.path("doi").asText(null);
                    String doi = null;
                    if (doiUrl != null && doiUrl.startsWith("https://doi.org/")) {
                        doi = doiUrl.substring("https://doi.org/".length());
                    }
                    if (doi == null || doi.isEmpty()) {
                        continue;
                    }
                    // Reuse single-work parse: serialize the work node back to JSON string
                    String workJson = objectMapper.writeValueAsString(work);
                    DoiLookupResult result = parseResponse(workJson, doi);
                    if (result.isFound()) {
                        results.add(result);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse OpenAlex search response: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Parse OpenAlex JSON response into DoiLookupResult.
     */
    private DoiLookupResult parseResponse(String jsonBody, String doi) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            DoiLookupResult result = new DoiLookupResult();
            result.setFound(true);
            result.setDoi(doi);

            // Title: display_name
            result.setTitle(root.path("display_name").asText(null));

            // Authors: authorships[].author.display_name (semicolon-separated)
            StringBuilder authors = new StringBuilder();
            JsonNode authorships = root.path("authorships");
            if (authorships.isArray()) {
                for (int i = 0; i < authorships.size(); i++) {
                    if (i > 0) authors.append("; ");
                    JsonNode author = authorships.get(i).path("author");
                    authors.append(author.path("display_name").asText(""));
                }
            }
            if (!authors.isEmpty()) {
                result.setAuthors(authors.toString());
            }

            // Journal: primary_location.source.display_name
            JsonNode primaryLocation = root.path("primary_location");
            if (!primaryLocation.isMissingNode()) {
                JsonNode source = primaryLocation.path("source");
                result.setJournal(source.path("display_name").asText(null));
            }

            // Volume and Issue
            result.setVolume(root.path("biblio").path("volume").asText(null) != null
                    ? Integer.parseInt(root.path("biblio").path("volume").asText()) : null);
            result.setIssue(root.path("biblio").path("issue").asText(null) != null
                    ? Integer.parseInt(root.path("biblio").path("issue").asText()) : null);

            // Pages
            result.setPages(root.path("biblio").path("pages").asText(null));

            // Publication year
            result.setPublishYear(root.path("publication_year").asInt(0));
            if (result.getPublishYear() == 0) result.setPublishYear(null);

            // Abstract (via abstract_inverted_index — de-invert)
            JsonNode invertedIndex = root.path("abstract_inverted_index");
            if (!invertedIndex.isMissingNode() && !invertedIndex.isEmpty()) {
                String abstractText = deinvertAbstract(invertedIndex);
                result.setAbstractText(abstractText);
            }

            return result;

        } catch (Exception e) {
            log.warn("Failed to parse OpenAlex response for DOI {}: {}", doi, e.getMessage());
            DoiLookupResult result = new DoiLookupResult();
            result.setFound(false);
            result.setDoi(doi);
            return result;
        }
    }

    /**
     * De-invert the abstract_inverted_index format used by OpenAlex.
     * <p>
     * OpenAlex stores abstracts as a word -> position[] map.
     * We need to reconstruct the ordered text from it.
     */
    private String deinvertAbstract(JsonNode invertedIndex) {
        try {
            // Determine max position to size the array
            int maxPos = 0;
            Map<String, List<Integer>> wordPositions = new HashMap<>();

            Iterator<Map.Entry<String, JsonNode>> fields = invertedIndex.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String word = entry.getKey();
                List<Integer> positions = new ArrayList<>();
                for (JsonNode pos : entry.getValue()) {
                    int p = pos.asInt();
                    positions.add(p);
                    if (p > maxPos) maxPos = p;
                }
                wordPositions.put(word, positions);
            }

            // Build word array
            String[] words = new String[maxPos + 1];
            for (Map.Entry<String, List<Integer>> entry : wordPositions.entrySet()) {
                for (int pos : entry.getValue()) {
                    words[pos] = entry.getKey();
                }
            }

            // Join with spaces
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("Failed to de-invert abstract: {}", e.getMessage());
            return null;
        }
    }
}
