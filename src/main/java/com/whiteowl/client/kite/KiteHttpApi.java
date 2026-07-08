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
import com.whiteowl.client.kite.model.KiteLimitType;
import com.whiteowl.client.kite.model.KiteMargin;
import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteOrderId;
import com.whiteowl.client.kite.model.KitePosition;
import com.whiteowl.client.kite.model.KitePositions;
import com.whiteowl.client.kite.model.KiteProfile;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteResponse;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.client.kite.model.KiteTwofa;
import com.whiteowl.client.kite.session.KiteSessionStore;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static com.whiteowl.client.kite.KiteConstant.*;

@Slf4j
public final class KiteHttpApi implements KiteApi {

    private final UUID uuid = UUID.randomUUID();
    private final KiteCredentials credentials;
    private final KiteSessionStore sessionStore;
    private final HttpClient httpClient;
    private final CookieManager cookieManager;
    private volatile String enctoken;

    public KiteHttpApi(KiteCredentials credentials) {
        this(credentials, null);
    }

    public KiteHttpApi(KiteCredentials credentials, KiteSessionStore sessionStore) {
        this.credentials = credentials;
        this.sessionStore = sessionStore;
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        log.info("KiteHttpApi initialized for user={}", credentials.getUsername());
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
        String url = URL_BASE + URL_ORDERS + "/" + order.getVariety().name();
        Map<String, String> params = buildOrderParams(order);
        return executePost(url, params, new TypeReference<KiteResponse<KiteOrderId>>() {});
    }

    @Override
    public KiteOrderId updateOrder(KiteOrder order) {
        String url = URL_BASE + URL_ORDERS + "/" + order.getVariety().name() + "/" + order.getOrderId();
        Map<String, String> params = buildOrderParams(order);
        return executePut(url, params, new TypeReference<KiteResponse<KiteOrderId>>() {});
    }

    @Override
    public KiteOrderId cancelOrder(KiteOrder order) {
        String url = URL_BASE + URL_ORDERS + "/" + order.getVariety().name() + "/" + order.getOrderId();
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
        Map<String, String> params = buildGttParams(condition, orders, type, expiresAt);
        return executePost(URL_BASE + URL_GTT, params, new TypeReference<KiteResponse<KiteGttTriggerId>>() {});
    }

    @Override
    public KiteGttTriggerId updateGttTrigger(int triggerId, KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        Map<String, String> params = buildGttParams(condition, orders, type, expiresAt);
        return executePut(URL_BASE + URL_GTT + "/" + triggerId, params, new TypeReference<KiteResponse<KiteGttTriggerId>>() {});
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
        HttpResponse<String> loginResponse = sendPost(URL_BASE + URL_LOGIN, loginBody);
        log.debug("Login response status={}", loginResponse.statusCode());
        KiteTwofa twofa = parseResponse(loginResponse.body(), new TypeReference<KiteResponse<KiteTwofa>>() {});
        performTwofa(twofa);
        log.info("Login successful for user={}", credentials.getUsername());
    }

    private void performTwofa(KiteTwofa twofa) {
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
        HttpResponse<String> twofaResponse = sendPost(URL_BASE + URL_TWOFA, body);
        log.debug("2FA response status={}", twofaResponse.statusCode());
        enctoken = cookieManager.getCookieStore().getCookies().stream()
                .filter(c -> "enctoken".equals(c.getName()))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new KiteApiException(HTTP_UNAUTHORIZED, "enctoken not found after 2FA"));
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
        HttpResponse<String> response = sendGet(url);
        return handleResponse(response, typeRef, url);
    }

    private <T> T executePost(String url, Map<String, String> params, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        HttpResponse<String> response = sendPost(url, formEncode(params));
        return handleResponse(response, typeRef, url);
    }

    private <T> T executePut(String url, Map<String, String> params, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        HttpRequest request = buildRequest(url)
                .PUT(HttpRequest.BodyPublishers.ofString(formEncode(params)))
                .header("Content-Type", CONTENT_TYPE_FORM)
                .build();
        HttpResponse<String> response = send(request);
        return handleResponse(response, typeRef, url);
    }

    private <T> T executeDelete(String url, TypeReference<KiteResponse<T>> typeRef) {
        ensureLoggedIn();
        HttpRequest request = buildRequest(url).DELETE().build();
        HttpResponse<String> response = send(request);
        return handleResponse(response, typeRef, url);
    }

    private <T> T handleResponse(HttpResponse<String> response, TypeReference<KiteResponse<T>> typeRef, String url) {
        int statusCode = response.statusCode();
        if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
            log.warn("Auth failure status={} url={} headers={} body={}",
                    statusCode, url, response.headers().map(), response.body());
            enctoken = null;
            if (sessionStore != null) {
                sessionStore.clear();
            }
            login();
            return executeGet(url, typeRef);
        }
        if (statusCode >= HTTP_REDIRECT_THRESHOLD) {
            log.error("HTTP error status={} url={} headers={} body={}",
                    statusCode, url, response.headers().map(), response.body());
            throw new KiteApiException(statusCode, response.body());
        }
        return parseResponse(response.body(), typeRef);
    }

    private <T> T parseResponse(String body, TypeReference<KiteResponse<T>> typeRef) {
        try {
            KiteResponse<T> kiteResponse = JSON.readValue(body, typeRef);
            if (!STATUS_SUCCESS.equalsIgnoreCase(kiteResponse.getStatus())) {
                log.error("Kite API error status={} message={} body={}",
                        kiteResponse.getStatus(), kiteResponse.getMessage(), body);
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

    private HttpResponse<String> sendGet(String url) {
        HttpRequest request = buildRequest(url).GET().build();
        return send(request);
    }

    private HttpResponse<String> sendPost(String url, String body) {
        HttpRequest request = buildRequest(url)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", CONTENT_TYPE_FORM)
                .build();
        return send(request);
    }

    private HttpRequest.Builder buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT_CHROME)
                .header("Origin", URL_BASE)
                .header("Referer", URL_BASE + URL_DASHBOARD)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Upgrade-Insecure-Requests", "1")
                .header("sec-fetch-user", "?1")
                .header("sec-fetch-site", "none")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-fetch-dest", "document")
                .header("sec-fetch-mode", "navigate")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"")
                .header("x-kite-version", "3.0.7")
                .header("x-kite-app-uuid", uuid.toString())
                .header("x-kite-userid", credentials.getUsername());
        if (enctoken != null) {
            builder.header(HEADER_AUTHORIZATION, ENCTOKEN_PREFIX + enctoken);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest request) {
        log.debug("{} {}", request.method(), request.uri());
        try {
            HttpResponse<byte[]> rawResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String body = decodeBody(rawResponse);
            HttpResponse<String> response = new StringHttpResponse(rawResponse, body);
            log.debug("{} {} status={}", request.method(), request.uri(), response.statusCode());
            return response;
        } catch (IOException e) {
            log.error("HTTP request failed {} {} requestHeaders={}",
                    request.method(), request.uri(), request.headers().map(), e);
            throw new KiteApiException(0, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP request interrupted {} {}", request.method(), request.uri(), e);
            throw new KiteApiException(0, e.getMessage());
        }
    }

    private String decodeBody(HttpResponse<byte[]> response) throws IOException {
        byte[] bytes = response.body();
        String encoding = response.headers().firstValue("content-encoding").orElse("");
        byte[] decompressed = switch (encoding.toLowerCase()) {
            case "gzip" -> decompress(new GZIPInputStream(new ByteArrayInputStream(bytes)));
            case "deflate" -> decompress(new InflaterInputStream(new ByteArrayInputStream(bytes)));
            default -> isGzipped(bytes) ? decompress(new GZIPInputStream(new ByteArrayInputStream(bytes))) : bytes;
        };
        return new String(decompressed, StandardCharsets.UTF_8);
    }

    private boolean isGzipped(byte[] bytes) {
        return bytes.length >= 2 && (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B;
    }

    private byte[] decompress(java.io.InputStream stream) throws IOException {
        try (stream) {
            return stream.readAllBytes();
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
        params.put("user_id", credentials.getUsername());
        params.put("tradingsymbol", order.getTradingsymbol());
        params.put("exchange", order.getExchange().name());
        params.put("transaction_type", order.getTransactionType().name());
        params.put("order_type", order.getLimitType().name());
        params.put("quantity", String.valueOf(order.getQuantity()));
        params.put("product", order.getProduct().name());
        params.put("validity", order.getValidity().name());
        params.put("disclosed_quantity", String.valueOf(order.getDisclosedQuantity()));
        if (KiteLimitType.LIMIT == order.getLimitType() || KiteLimitType.SL == order.getLimitType()) {
            params.put("price", String.valueOf(order.getPrice()));
        }
        if (KiteLimitType.SL == order.getLimitType() || KiteLimitType.SLM == order.getLimitType()) {
            params.put("trigger_price", String.valueOf(order.getTriggerPrice()));
        }
        if (order.getStoploss() > 0) {
            params.put("stoploss", String.valueOf(order.getStoploss()));
        }
        if (order.getSquareoff() > 0) {
            params.put("squareoff", String.valueOf(order.getSquareoff()));
        }
        if (order.getTrailingStoploss() > 0) {
            params.put("trailing_stoploss", String.valueOf(order.getTrailingStoploss()));
        }
        return params;
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

    private record StringHttpResponse(HttpResponse<byte[]> delegate, String decodedBody) implements HttpResponse<String> {
        @Override public int statusCode() { return delegate.statusCode(); }
        @Override public HttpRequest request() { return delegate.request(); }
        @Override public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
        @Override public java.net.http.HttpHeaders headers() { return delegate.headers(); }
        @Override public String body() { return decodedBody; }
        @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return delegate.sslSession(); }
        @Override public URI uri() { return delegate.uri(); }
        @Override public HttpClient.Version version() { return delegate.version(); }
    }

}
