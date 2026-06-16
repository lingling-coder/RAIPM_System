package com.institute.achievement.integration.doi;

import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
