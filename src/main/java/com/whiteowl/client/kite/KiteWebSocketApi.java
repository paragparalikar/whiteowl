package com.whiteowl.client.kite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteSymbol;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.whiteowl.client.kite.KiteConstant.*;

public final class KiteWebSocketApi implements WebSocket.Listener, AutoCloseable {

    private static final long PING_INTERVAL_MS = 2500;
    private static final long RECONNECT_INTERVAL_MS = 500;
    private static final String ACTION_SUBSCRIBE = "subscribe";
    private static final String ACTION_SET_MODE = "mode";
    private static final String MODE_FULL = "full";
    private static final String MODE_QUOTE = "quote";
    private static final String MODE_LTP = "ltp";
    private static final String TYPE_ORDER = "order";
    private static final String TYPE_ERROR = "error";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DATA = "data";

    private volatile WebSocket webSocket;
    private final HttpClient httpClient;
    private final KiteCredentials credentials;
    private final KiteDataPublisher dataPublisher;
    private final Supplier<String> enctokenSupplier;
    private final Collection<KiteSymbol> symbols = new HashSet<>();
    private final ScheduledExecutorService scheduler;
    private final KiteBinaryParser binaryParser = new KiteBinaryParser();
    private volatile ScheduledFuture<?> reconnectFuture;
    private volatile ScheduledFuture<?> pingFuture;
    private final ByteBuffer binaryAccumulator = ByteBuffer.allocate(1024 * 64);

    public KiteWebSocketApi(KiteCredentials credentials,
                            KiteDataPublisher dataPublisher,
                            Supplier<String> enctokenSupplier) {
        this.credentials = credentials;
        this.dataPublisher = dataPublisher;
        this.enctokenSupplier = enctokenSupplier;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kite-ws-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (webSocket == null || webSocket.isInputClosed()) {
            connect();
        }
    }

    @Override
    public void close() {
        cancelFuture(reconnectFuture);
        cancelFuture(pingFuture);
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "closing");
            webSocket = null;
        }
        scheduler.shutdownNow();
    }

    public void subscribe(Collection<KiteSymbol> newSymbols) {
        symbols.addAll(newSymbols);
        if (webSocket != null && !webSocket.isOutputClosed()) {
            List<Integer> tokens = newSymbols.stream().map(KiteSymbol::getInstrumentToken).toList();
            webSocket.sendText(createSubscribeMessage(tokens), true);
            webSocket.sendText(createModeMessage(tokens, KiteQuoteMode.OHLC), true);
        }
    }

    @Override
    public void onOpen(WebSocket ws) {
        this.webSocket = ws;
        ws.request(1);
        scheduleReconnect();
        schedulePing();
        subscribe(symbols);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
        binaryAccumulator.put(data);
        if (last) {
            binaryAccumulator.flip();
            byte[] payload = new byte[binaryAccumulator.remaining()];
            binaryAccumulator.get(payload);
            binaryAccumulator.clear();
            binaryParser.parseBinary(payload).forEach(dataPublisher::publish);
        }
        ws.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        handleTextMessage(data.toString());
        ws.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        connect();
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        connect();
    }

    private void connect() {
        try {
            String enc = URLEncoder.encode(enctokenSupplier.get(), StandardCharsets.UTF_8);
            URI wsUri = URI.create(URL_WS + "/?api_key=" + API_KEY
                    + "&user_id=" + credentials.getUsername()
                    + "&enctoken=" + enc
                    + "&uid=" + new Date().getTime()
                    + "&user-agent=" + USER_AGENT_KITE
                    + "&version=" + WS_VERSION);
            httpClient.newWebSocketBuilder()
                    .header("User-Agent", USER_AGENT_CHROME)
                    .buildAsync(wsUri, this);
        } catch (Exception ignored) {
        }
    }

    private void handleTextMessage(String payload) {
        try {
            JsonNode data = JSON.readTree(payload);
            if (!data.has(FIELD_TYPE)) return;
            String type = data.get(FIELD_TYPE).asText();
            if (TYPE_ORDER.equals(type)) {
                KiteOrder order = JSON.readValue(data.get(FIELD_DATA).toString(), KiteOrder.class);
                dataPublisher.publish(order);
            }
        } catch (Exception ignored) {
        }
    }

    private void scheduleReconnect() {
        cancelFuture(reconnectFuture);
        reconnectFuture = scheduler.scheduleWithFixedDelay(
                this::start, RECONNECT_INTERVAL_MS, RECONNECT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void schedulePing() {
        cancelFuture(pingFuture);
        pingFuture = scheduler.scheduleAtFixedRate(() -> {
            if (webSocket != null && !webSocket.isOutputClosed()) {
                webSocket.sendPing(ByteBuffer.allocate(0));
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelFuture(ScheduledFuture<?> future) {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    private String createSubscribeMessage(List<Integer> tokens) {
        ObjectNode node = JSON.createObjectNode();
        ArrayNode list = node.arrayNode();
        tokens.forEach(list::add);
        node.set("v", list);
        node.put("a", ACTION_SUBSCRIBE);
        return node.toString();
    }

    private String createModeMessage(List<Integer> tokens, KiteQuoteMode mode) {
        ObjectNode node = JSON.createObjectNode();
        ArrayNode outer = node.arrayNode();
        outer.add(resolveMode(mode));
        ArrayNode inner = node.arrayNode();
        tokens.forEach(inner::add);
        outer.add(inner);
        node.put("a", ACTION_SET_MODE);
        node.set("v", outer);
        return node.toString();
    }

    private String resolveMode(KiteQuoteMode mode) {
        return switch (mode) {
            case FULL -> MODE_FULL;
            case OHLC -> MODE_QUOTE;
            case LTP -> MODE_LTP;
        };
    }

}
