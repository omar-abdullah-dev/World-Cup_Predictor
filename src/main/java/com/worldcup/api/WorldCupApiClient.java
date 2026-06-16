package com.worldcup.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.api.dto.FixtureDto;
import com.worldcup.api.dto.LiveScoreDto;
import com.worldcup.api.dto.StandingDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralised HTTP client for the WorldCupAPI.
 * Base URL  : https://api.worldcupapi.com
 * Auth      : ?key=<API_KEY> query param
 * All calls go through this class — no bean touches the API directly.
 */
@ApplicationScoped
public class WorldCupApiClient {

    private static final Logger LOG = Logger.getLogger(WorldCupApiClient.class.getName());
    private static final String BASE_URL = "https://api.worldcupapi.com";
    private static final String API_KEY  = "U5FZtdxCvDr9SD9rG";
    private static final int    TIMEOUT_SEC = 15;
    private static final int    MAX_RETRIES = 3;

    private final HttpClient    http   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC)).build();
    private final ObjectMapper  mapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Public API methods
    // -----------------------------------------------------------------------

    /** GET /fixtures — all fixtures, optionally filtered by date (YYYY-MM-DD). */
    public List<FixtureDto> getFixtures(String date) {
        StringBuilder url = new StringBuilder(BASE_URL).append("/fixtures?key=").append(API_KEY);
        if (date != null && !date.isBlank()) url.append("&date=").append(date);
        return getList(url.toString(), new TypeReference<List<FixtureDto>>() {});
    }

    /** GET /fixtures?group=A — fixtures for one group. */
    public List<FixtureDto> getFixturesByGroup(String group) {
        String url = BASE_URL + "/fixtures?key=" + API_KEY + "&group=" + group;
        return getList(url, new TypeReference<List<FixtureDto>>() {});
    }

    /** GET /livescores — all currently live matches. */
    public List<LiveScoreDto> getLiveScores() {
        String url = BASE_URL + "/livescores?key=" + API_KEY;
        return getList(url, new TypeReference<List<LiveScoreDto>>() {});
    }

    /** GET /standings?group=A — published group standings. */
    public List<StandingDto> getStandings(String group) {
        String url = BASE_URL + "/standings?key=" + API_KEY + "&group=" + group;
        return getList(url, new TypeReference<List<StandingDto>>() {});
    }

    /** GET /history?date_from=…&date_to=… — finished historical matches. */
    public List<FixtureDto> getHistory(String dateFrom, String dateTo) {
        String url = BASE_URL + "/history?key=" + API_KEY
                + "&date_from=" + dateFrom + "&date_to=" + dateTo;
        return getList(url, new TypeReference<List<FixtureDto>>() {});
    }

    // -----------------------------------------------------------------------
    // Generic helpers
    // -----------------------------------------------------------------------

    private <T> List<T> getList(String url, TypeReference<List<T>> type) {
        String body = executeWithRetry(url);
        if (body == null) return Collections.emptyList();
        try {
            // Check for error envelope {"success":false,...}
            JsonNode root = mapper.readTree(body);
            if (root.isObject() && root.has("success") && !root.get("success").asBoolean()) {
                String err = root.path("error").asText("unknown");
                int    code = root.path("code").asInt(0);
                LOG.warning("[WorldCupApiClient] API error " + code + ": " + err + " | url=" + url);
                return Collections.emptyList();
            }
            return mapper.readValue(body, type);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[WorldCupApiClient] Parse error for " + url, e);
            return Collections.emptyList();
        }
    }

    /** HTTP GET with up to MAX_RETRIES retries on 5xx / IOException. */
    private String executeWithRetry(String url) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                        .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                if (status == 200) return resp.body();
                if (status == 429) {
                    LOG.warning("[WorldCupApiClient] Rate limited (429). Attempt " + attempt);
                    Thread.sleep(2000L * attempt);
                } else if (status >= 500) {
                    LOG.warning("[WorldCupApiClient] Server error " + status + ". Attempt " + attempt);
                    Thread.sleep(1000L * attempt);
                } else {
                    LOG.warning("[WorldCupApiClient] HTTP " + status + " for " + url);
                    return null; // 4xx — don't retry
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[WorldCupApiClient] IO error attempt " + attempt + " url=" + url, e);
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ix) {
                    Thread.currentThread().interrupt(); return null;
                }
            }
        }
        LOG.warning("[WorldCupApiClient] All " + MAX_RETRIES + " attempts failed for " + url);
        return null;
    }
}
