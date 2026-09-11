package com.whiteowl.client.kite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.whiteowl.client.kite.model.KiteCandle;
import com.whiteowl.client.kite.model.KiteCandleSeries;
import com.whiteowl.client.kite.model.KiteGttCondition;
import com.whiteowl.client.kite.model.KiteGttOrder;
import com.whiteowl.client.kite.model.KiteGttTrigger;
import com.whiteowl.client.kite.model.KiteGttTriggerId;
import com.whiteowl.client.kite.model.KiteGttType;
import com.whiteowl.client.kite.model.KiteHolding;
import com.whiteowl.client.kite.model.KiteInterval;
import com.whiteowl.client.kite.model.KiteMargin;
import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteOrderId;
import com.whiteowl.client.kite.model.KiteOrderVariety;
import com.whiteowl.client.kite.model.KitePosition;
import com.whiteowl.client.kite.model.KitePositions;
import com.whiteowl.client.kite.model.KiteProfile;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteResponse;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.client.kite.model.KiteTwofa;
import com.whiteowl.client.kite.session.KiteSessionStore;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyType;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.whiteowl.client.kite.KiteConstant.*;

@Slf4j
public final class KiteHttpApi implements KiteApi {

    private final UUID uuid = UUID.randomUUID();
    private final KiteCredentials credentials;
    private final KiteSessionStore sessionStore;
    private final AsyncHttpClient httpClient;
    private volatile String enctoken;

    public KiteHttpApi(KiteCredentials credentials) {
        this(credentials, null);
    }

    public KiteHttpApi(KiteCredentials credentials, KiteSessionStore sessionStore) {
        this.credentials = credentials;
        this.sessionStore = sessionStore;
        this.httpClient = buildHttpClient();
        log.info("KiteHttpApi initialized for user={}", credentials.getUsername());
    }

    private static AsyncHttpClient buildHttpClient() {
        String proxyHost = System.getProperty("https.proxyHost", System.getProperty("http.proxyHost", ""));
        String proxyUser = System.getProperty("https.proxyUser", System.getProperty("http.proxyUser", ""));
        String proxyPass = System.getProperty("https.proxyPassword", System.getProperty("http.proxyPassword", ""));
        String proxyPort = System.getProperty("https.proxyPort", System.getProperty("http.proxyPort", "8080"));

        DefaultAsyncHttpClientConfig.Builder config = Dsl.config()
                .setFollowRedirect(false)
                .setKeepAlive(true)
                .setMaxRedirects(0);

        if (!proxyHost.isEmpty()) {
            int port = Integer.parseInt(proxyPort);
            ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(proxyHost, port)
                    .setProxyType(ProxyType.HTTP);
            if (!proxyUser.isEmpty()) {
                log.info("Building AsyncHttpClient with NTLM proxy: host={} port={} user={}", proxyHost, proxyPort, proxyUser);
                proxyBuilder.setRealm(new Realm.Builder(proxyUser, proxyPass)
                        .setScheme(Realm.AuthScheme.NTLM)
                        .setNtlmDomain("")
                        .setNtlmHost(""));
            } else {
                log.info("Building AsyncHttpClient with proxy (no credentials): host={} port={}", proxyHost, proxyPort);
            }
            config.setProxyServer(proxyBuilder.build());
        } else {
            log.info("Building AsyncHttpClient with no proxy");
        }

        return Dsl.asyncHttpClient(config);
    }

    public String getEnctoken() {
        return enctoken;
    }

    @Override
    public void init() {
        if (sessionStore != null) {
            sessionStore.loadEnctoken(credentials.getUsername())
                    .ifPresent(token -> {
                        this.enctoken = token;
                        log.info("Restored session from disk for user={}", credentials.getUsername());
                    });
        }
        if (enctoken == null) {
            login();
        }
    }

    @Override
    public void close() {
        enctoken = null;
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Error closing AsyncHttpClient for user={}", credentials.getUsername(), e);
        }
    }

    @Override
    public List<KiteCandle> getHistoricalData(KiteSymbol symbol, KiteInterval interval, ZonedDateTime from, ZonedDateTime to) {
        String url = URL_BASE + URL_BARS + "/" + symbol.getInstrumentToken() + "/" + interval.getText()
                + "?oi=1&user_id=" + URLEncoder.encode(credentials.getUsername(), StandardCharsets.UTF_8)
                + "&from=" + URLEncoder.encode(FORMATTER.format(from), StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(FORMATTER.format(to), StandardCharsets.UTF_8);
        KiteCandleSeries series = executeGet(url, new TypeReference<KiteResponse<KiteCandleSeries>>() {});
        return series.toCandles();
    }

    @Override
    public KiteProfile getProfile() {
        return executeGet(URL_BASE + URL_PROFILE, new TypeReference<KiteResponse<KiteProfile>>() {});
    }

    @Override
    public KiteMargin getMargin() {
        return executeGet(URL_BASE + URL_MARGIN, new TypeReference<KiteResponse<KiteMargin>>() {});
    }

    @Override
    public List<KiteHolding> getHoldings() {
        return executeGet(URL_BASE + URL_HOLDINGS, new TypeReference<KiteResponse<List<KiteHolding>>>() {});
    }

    @Override
    public List<KitePosition> getPositions() {
        KitePositions positions = executeGet(URL_BASE + URL_POSITIONS, new TypeReference<KiteResponse<KitePositions>>() {});
        return positions != null && positions.getNet() != null ? positions.getNet() : List.of();
    }

    @Override
    public List<KiteOrder> getOrders() {
        return executeGet(URL_BASE + URL_ORDERS, new TypeReference<KiteResponse<List<KiteOrder>>>() {});
    }

    @Override
    public KiteOrderId createOrder(KiteOrder order) {
        String url = URL_BASE + URL_ORDERS + "/" + resolveVariety(order.getVariety());
        return executePost(url, buildOrderParams(order), new TypeReference<KiteResponse<KiteOrderId>>() {});
    }

    @Override
    public KiteOrderId updateOrder(KiteOrder order) {
        String url = URL_BASE + URL_ORDERS + "/" + resolveVariety(order.getVariety()) + "/" + order.getOrderId();
        return executePut(url, buildOrderParams(order), new TypeReference<KiteResponse<KiteOrderId>>() {});
    }

    @Override
    public KiteOrderId cancelOrder(KiteOrder order) {
        String url = URL_BASE + URL_ORDERS + "/" + resolveVariety(order.getVariety()) + "/" + order.getOrderId();
        return executeDelete(url, new TypeReference<KiteResponse<KiteOrderId>>() {});
    }

    @Override
    public Map<String, KiteQuote> getQuotes(Collection<KiteSymbol> instruments, KiteQuoteMode mode) {
        String baseUrl = resolveQuoteUrl(mode);
        String queryString = instruments.stream()
                .map(i -> "i=" + URLEncoder.encode(i.getExchange().name() + ":" + i.getTradingsymbol(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return executeGet(baseUrl + "?" + queryString, new TypeReference<KiteResponse<Map<String, KiteQuote>>>() {});
    }

    @Override
    public void subscribe(Collection<KiteSymbol> symbols) {
        throw new UnsupportedOperationException("WebSocket subscription not implemented in HTTP API");
    }

    @Override
    public List<KiteGttTrigger> getGttTriggers() {
        return executeGet(URL_BASE + URL_GTT, new TypeReference<KiteResponse<List<KiteGttTrigger>>>() {});
    }

    @Override
    public KiteGttTrigger getGttTrigger(int triggerId) {
        return executeGet(URL_BASE + URL_GTT + "/" + triggerId, new TypeReference<KiteResponse<KiteGttTrigger>>() {});
    }

    @Override
    public KiteGttTriggerId createGttTrigger(KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return executePost(URL_BASE + URL_GTT, buildGttParams(condition, orders, type, expiresAt), new TypeReference<KiteResponse<KiteGttTriggerId>>() {});
    }

    @Override
    public KiteGttTriggerId updateGttTrigger(int triggerId, KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return executePut(URL_BASE + URL_GTT + "/" + triggerId, buildGttParams(condition, orders, type, expiresAt), new TypeReference<KiteResponse<KiteGttTriggerId>>() {});
    }

    @Override
    public KiteGttTriggerId deleteGttTrigger(int triggerId) {
        return executeDelete(URL_BASE + URL_GTT + "/" + triggerId, new TypeReference<KiteResponse<KiteGttTriggerId>>() {});
    }

    private synchronized void login() {
        if (enctoken != null) {
            return;
        }
        log.info("Starting login for user={}", credentials.getUsername());
        sendGet(URL_BASE + "/");
        String loginBody = formEncode(Map.of(
                "user_id", credentials.getUsername(),
                "password", credentials.getPassword()));
        Response loginResponse = sendPost(URL_BASE + URL_LOGIN, loginBody);
        log.info("Login response status={} user={}", loginResponse.getStatusCode(), credentials.getUsername());
        if (loginResponse.getStatusCode() >= HTTP_ERROR_THRESHOLD) {
            log.error("Login HTTP error status={} user={} body={}", loginResponse.getStatusCode(), credentials.getUsername(), loginResponse.getResponseBody());
        } else if (loginResponse.getResponseBody() == null || loginResponse.getResponseBody().isBlank()) {
            log.error("Login response body is empty status={} user={}", loginResponse.getStatusCode(), credentials.getUsername());
        } else {
            log.debug("Login response body={}", loginResponse.getResponseBody());
        }
        KiteTwofa twofa = parseResponse(loginResponse, new TypeReference<KiteResponse<KiteTwofa>>() {}, URL_LOGIN);
        performTwofa(twofa);
        log.info("Login successful for user={}", credentials.getUsername());
    }

    private void performTwofa(KiteTwofa twofa) {
        log.info("Performing 2FA for user={} twofa_type={} request_id={} locked={} captcha={}",
                credentials.getUsername(), twofa.getTwofaType(), twofa.getRequestId(), twofa.isLocked(), twofa.isCaptcha());
        if (twofa.isLocked()) {
            throw new KiteApiException(HTTP_FORBIDDEN, "Account locked for " + credentials.getUsername());
        }
        if (twofa.isCaptcha()) {
            throw new KiteApiException(HTTP_FORBIDDEN, "CAPTCHA required for " + credentials.getUsername());
        }
        String totpValue = generateTotp(credentials.getPin());
        String body = formEncode(Map.of(
                "skip_session", "",
                "user_id", credentials.getUsername(),
                "request_id", twofa.getRequestId(),
                "twofa_type", twofa.getTwofaType(),
                "twofa_value", totpValue));
        Response twofaResponse = sendPost(URL_BASE + URL_TWOFA, body);
        log.info("2FA response status={} user={}", twofaResponse.getStatusCode(), credentials.getUsername());
        if (twofaResponse.getStatusCode() >= HTTP_ERROR_THRESHOLD || twofaResponse.getResponseBody() == null || twofaResponse.getResponseBody().isBlank()) {
            log.error("2FA failed status={} user={} body={}", twofaResponse.getStatusCode(), credentials.getUsername(), twofaResponse.getResponseBody());
        } else {
            log.debug("2FA response body={}", twofaResponse.getResponseBody());
        }
        List<Cookie> cookies = twofaResponse.getCookies();
        log.debug("Cookies after 2FA: count={} names={}", cookies.size(),
                cookies.stream().map(Cookie::name).toList());
        enctoken = cookies.stream()
                .filter(c -> "enctoken".equals(c.name()))
                .map(Cookie::value)
                .findFirst()
                .orElseThrow(() -> new KiteApiException(HTTP_UNAUTHORIZED, "enctoken not found after 2FA for user=" + credentials.getUsername()));
        log.info("enctoken acquired for user={}", credentials.getUsername());
        if (sessionStore != null) {
            sessionStore.save(credentials.getUsername(), enctoken);
        }
    }

    private String generateTotp(String pin) {
        int second = java.time.LocalDateTime.now().getSecond();
        waitIfNearBoundary(second);
        return String.format(TOTP_FORMAT, new GoogleAuthenticator().getTotpPassword(pin));
    }

    private void waitIfNearBoundary(int second) {
        long sleepMs = 0;
        if (second >= TOTP_WAIT_LOWER && second <= TOTP_WAIT_UPPER_30) {
            sleepMs = (SECONDS_30 - second) * 1000L;
        } else if (second >= (TOTP_WAIT_LOWER + SECONDS_30) && second <= TOTP_WAIT_UPPER_60) {
            sleepMs = (SECONDS_60 - second) * 1000L;
        }
        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private <T> T executeGet(String url, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        Response response = sendGet(url);
        if (isAuthFailure(response, url)) {
            response = sendGet(url);
        }
        return handleResponse(response, typeRef, url);
    }

    private <T> T executePost(String url, Map<String, String> params, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        Response response = sendPost(url, formEncode(params));
        if (isAuthFailure(response, url)) {
            response = sendPost(url, formEncode(params));
        }
        return handleResponse(response, typeRef, url);
    }

    private <T> T executePut(String url, Map<String, String> params, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        Response response = send("PUT", url, formEncode(params));
        if (isAuthFailure(response, url)) {
            response = send("PUT", url, formEncode(params));
        }
        return handleResponse(response, typeRef, url);
    }

    private <T> T executeDelete(String url, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        Response response = send("DELETE", url, null);
        if (isAuthFailure(response, url)) {
            response = send("DELETE", url, null);
        }
        return handleResponse(response, typeRef, url);
    }

    private boolean isAuthFailure(Response response, String url) {
        int statusCode = response.getStatusCode();
        if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
            log.warn("Auth failure status={} url={} user={} body={}, re-logging in",
                    statusCode, url, credentials.getUsername(), response.getResponseBody());
            enctoken = null;
            if (sessionStore != null) {
                sessionStore.clear();
            }
            login();
            return true;
        }
        return false;
    }

    private <T> T handleResponse(Response response, TypeReference<KiteResponse<T>> typeRef, String url) {
        int statusCode = response.getStatusCode();
        if (statusCode >= HTTP_ERROR_THRESHOLD) {
            log.error("HTTP error status={} url={} body={}", statusCode, url, response.getResponseBody());
            throw new KiteApiException(statusCode, response.getResponseBody());
        }
        return parseResponse(response, typeRef, url);
    }

    private <T> T parseResponse(Response response, TypeReference<KiteResponse<T>> typeRef, String url) {
        String body = response.getResponseBody();
        if (body == null || body.isBlank()) {
            log.error("Empty response body status={} url={} user={}", response.getStatusCode(), url, credentials.getUsername());
            throw new KiteApiException(0, "Empty response body for user=" + credentials.getUsername());
        }
        try {
            KiteResponse<T> kiteResponse = JSON.readValue(body, typeRef);
            if (!STATUS_SUCCESS.equalsIgnoreCase(kiteResponse.getStatus())) {
                log.error("Kite API error status={} message={} body={}", kiteResponse.getStatus(), kiteResponse.getMessage(), body);
                throw new KiteApiException(0, kiteResponse.getMessage());
            }
            return kiteResponse.getData();
        } catch (IOException e) {
            log.error("JSON parse failed body={}", body, e);
            throw new KiteApiException(0, e.getMessage());
        }
    }

    private void ensureLoggedIn() {
        if (enctoken == null) {
            login();
        }
    }

    private Response sendGet(String url) {
        return send("GET", url, null);
    }

    private Response sendPost(String url, String body) {
        return send("POST", url, body);
    }

    private Response send(String method, String url, String body) {
        log.debug("{} {}", method, url);
        try {
            BoundRequestBuilder builder = switch (method) {
                case "POST" -> httpClient.preparePost(url);
                case "PUT" -> httpClient.preparePut(url);
                case "DELETE" -> httpClient.prepareDelete(url);
                default -> httpClient.prepareGet(url);
            };
            applyHeaders(builder);
            if (body != null) {
                builder.setHeader("Content-Type", "application/x-www-form-urlencoded")
                       .setBody(body);
            }
            Response response = builder.execute().get();
            log.debug("{} {} status={}", method, url, response.getStatusCode());
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KiteApiException(0, "Request interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            log.error("HTTP request failed {} {}", method, url, e.getCause());
            throw new KiteApiException(0, e.getCause().getMessage());
        }
    }

    private void applyHeaders(BoundRequestBuilder builder) {
        builder.setHeader("User-Agent", USER_AGENT_CHROME)
               .setHeader("Origin", URL_BASE)
               .setHeader("Referer", URL_BASE + URL_DASHBOARD)
               .setHeader("Accept", "application/json, text/plain, */*")
               .setHeader("Accept-Language", "en-US,en;q=0.9")
               .setHeader("sec-fetch-dest", "empty")
               .setHeader("sec-fetch-mode", "cors")
               .setHeader("sec-fetch-site", "same-origin")
               .setHeader("sec-ch-ua-mobile", "?0")
               .setHeader("sec-ch-ua-platform", "\"Windows\"")
               .setHeader("sec-ch-ua", SEC_CH_UA)
               .setHeader("x-kite-version", KITE_VERSION)
               .setHeader("x-kite-app-uuid", uuid.toString())
               .setHeader("x-kite-userid", credentials.getUsername());
        if (enctoken != null) {
            builder.setHeader(HEADER_AUTHORIZATION, ENCTOKEN_PREFIX + enctoken);
        }
    }

    private String resolveQuoteUrl(KiteQuoteMode mode) {
        return switch (mode) {
            case FULL -> URL_BASE + URL_QUOTE;
            case OHLC -> URL_BASE + URL_QUOTE_OHLC;
            case LTP -> URL_BASE + URL_QUOTE_LTP;
        };
    }

    private Map<String, String> buildOrderParams(KiteOrder order) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("variety", resolveVariety(order.getVariety()));
        params.put("exchange", order.getExchange().name());
        params.put("tradingsymbol", order.getTradingsymbol());
        params.put("transaction_type", order.getTransactionType().name());
        params.put("order_type", order.getLimitType().name());
        params.put("quantity", String.valueOf(order.getQuantity()));
        params.put("price", String.valueOf(order.getPrice()));
        params.put("product", order.getProduct().name());
        params.put("validity", order.getValidity().name());
        params.put("disclosed_quantity", String.valueOf(order.getDisclosedQuantity()));
        params.put("trigger_price", String.valueOf(order.getTriggerPrice()));
        params.put("squareoff", String.valueOf(order.getSquareoff()));
        params.put("stoploss", String.valueOf(order.getStoploss()));
        params.put("trailing_stoploss", String.valueOf(order.getTrailingStoploss()));
        params.put("user_id", credentials.getUsername());
        return params;
    }

    private String resolveVariety(KiteOrderVariety variety) {
        return variety.name().toLowerCase();
    }

    private Map<String, String> buildGttParams(KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("condition", JSON.writeValueAsString(condition));
            params.put("orders", JSON.writeValueAsString(orders));
            params.put("type", resolveGttTypeString(type));
            params.put("expires_at", expiresAt);
            return params;
        } catch (IOException e) {
            throw new KiteApiException(0, "Failed to serialize GTT params: " + e.getMessage());
        }
    }

    private String resolveGttTypeString(KiteGttType type) {
        return switch (type) {
            case SINGLE -> "single";
            case TWO_LEG -> "two-leg";
            case TRAILING_SINGLE -> "trailing-single";
            case TRAILING_TWO_LEG -> "trailing-two-leg";
        };
    }

    private static String formEncode(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

}
