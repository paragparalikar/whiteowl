package com.whiteowl.core.backtest.algotest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.backtest.algotest.model.BacktestRequest;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.BacktestStatusResponse;
import com.whiteowl.core.backtest.algotest.model.BacktestSubmitResponse;
import com.whiteowl.core.backtest.algotest.model.MarginEstimate;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core-Java client for the AlgoTest backtest api (https://api.algotest.in).
 *
 * Flow: POST /backtest returns {@code backtest_id}; the job runs server-side;
 * GET /backtest_status/{id} reports "running"/"completed"; GET /backtest/{id}
 * returns the full result including the summary metrics.
 *
 * Auth is the browser session: the {@code access_token_cookie} cookie carries
 * the JWT and the {@code csrf_access_token} cookie must be echoed in the
 * {@code x-csrf-token-access} header. Paste the full Cookie header value
 * copied from browser devtools into the driver; both values are extracted
 * from it here.
 */
@Slf4j
public class AlgoTestClient {

    public static final String API_BASE = "https://api.algotest.in";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient;
    private final String cookieHeader;
    private final String csrfToken;

    /** @param cookieHeader full "Cookie:" header value copied from browser devtools */
    public AlgoTestClient(String cookieHeader) {
        this.cookieHeader = requireCookie(cookieHeader);
        this.csrfToken = parseCookies(cookieHeader).get("csrf_access_token");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** Submits a backtest and returns its backtest id. */
    public String submit(BacktestRequest request) {
        try {
            String body = MAPPER.writeValueAsString(request);
            log.debug("POST /backtest body: {}", body);
            HttpRequest httpRequest = requestBuilder(API_BASE + "/backtest")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = send(httpRequest);
            BacktestSubmitResponse parsed = MAPPER.readValue(response.body(), BacktestSubmitResponse.class);
            if (parsed.getBacktestId() == null) {
                throw new AlgoTestException("Backtest submission returned no id: " + response.body());
            }
            return parsed.getBacktestId();
        } catch (AlgoTestException e) {
            throw e;
        } catch (Exception e) {
            throw new AlgoTestException("Backtest submission failed: " + e.getMessage(), e);
        }
    }

    public BacktestStatusResponse getStatus(String backtestId) {
        HttpRequest request = requestBuilder(API_BASE + "/backtest_status/" + backtestId).GET().build();
        try {
            return MAPPER.readValue(send(request).body(), BacktestStatusResponse.class);
        } catch (AlgoTestException e) {
            throw e;
        } catch (Exception e) {
            throw new AlgoTestException("Status check failed for " + backtestId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Margin estimate for a set of positions — same endpoint the site's
     * "Margin Estimate" button calls. {@code forExpiryDay} is the
     * "Margin Estimate for expiry day" toggle: when true the server prices
     * margins under expiry-day rules (higher span on short options).
     */
    public MarginEstimate.Response estimateMargin(List<MarginEstimate.Position> positions,
                                                  boolean forExpiryDay) {
        try {
            String body = MAPPER.writeValueAsString(
                    MarginEstimate.Request.of(positions, forExpiryDay));
            HttpRequest httpRequest = requestBuilder(API_BASE + "/marginCalcAPI/calculate_margin")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = send(httpRequest);
            return MAPPER.readValue(response.body(), MarginEstimate.Response.class);
        } catch (AlgoTestException e) {
            throw e;
        } catch (Exception e) {
            throw new AlgoTestException("Margin estimate failed: " + e.getMessage(), e);
        }
    }

    public BacktestResult getResult(String backtestId) {
        HttpRequest request = requestBuilder(API_BASE + "/backtest/" + backtestId).GET().build();
        try {
            return MAPPER.readValue(send(request).body(), BacktestResult.class);
        } catch (AlgoTestException e) {
            throw e;
        } catch (Exception e) {
            throw new AlgoTestException("Result fetch failed for " + backtestId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Submits the backtest, polls status until completion and returns the result.
     *
     * @param pollInterval delay between status checks
     * @param timeout      max time to wait for completion
     */
    public BacktestResult runAndWait(BacktestRequest request, Duration pollInterval, Duration timeout) {
        String backtestId = submit(request);
        log.info("Submitted backtest {}", backtestId);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            sleep(pollInterval);
            BacktestStatusResponse status = getStatus(backtestId);
            log.debug("Backtest {} status: {}", backtestId, status.getStatus());
            if (status.isCompleted()) {
                return getResult(backtestId);
            }
            if (status.isFailed()) {
                throw new AlgoTestException("Backtest " + backtestId + " failed with status " + status.getStatus());
            }
            if (System.nanoTime() > deadline) {
                throw new AlgoTestException("Backtest " + backtestId + " did not complete within " + timeout);
            }
        }
    }

    private HttpRequest.Builder requestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", "https://algotest.in")
                .header("Referer", "https://algotest.in/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36")
                .header("Cookie", cookieHeader);
        if (csrfToken != null) {
            builder.header("x-csrf-token-access", csrfToken);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AlgoTestException("Auth failed (HTTP " + response.statusCode()
                        + ") — refresh the cookie. Body: " + response.body());
            }
            if (response.statusCode() >= 400) {
                throw new AlgoTestException("HTTP " + response.statusCode() + " from "
                        + request.uri() + ": " + response.body());
            }
            return response;
        } catch (AlgoTestException e) {
            throw e;
        } catch (Exception e) {
            throw new AlgoTestException("Request to " + request.uri() + " failed: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseCookies(String cookieHeader) {
        Map<String, String> cookies = new HashMap<>();
        for (String pair : cookieHeader.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                cookies.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return cookies;
    }

    private static String requireCookie(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()
                || !cookieHeader.contains("access_token_cookie=")) {
            throw new AlgoTestException("Cookie header must contain access_token_cookie — "
                    + "copy the full Cookie request header value from browser devtools on algotest.in");
        }
        return cookieHeader;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlgoTestException("Interrupted while waiting for backtest", e);
        }
    }
}
