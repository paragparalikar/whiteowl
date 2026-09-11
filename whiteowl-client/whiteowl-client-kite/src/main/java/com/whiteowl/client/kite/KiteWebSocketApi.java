package com.whiteowl.client.kite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteSymbol;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyType;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.whiteowl.client.kite.KiteConstant.*;

public final class KiteWebSocketApi implements AutoCloseable {

    private static final long PING_INTERVAL_MS = 2500;
    private static final long RECONNECT_INTERVAL_MS = 500;
    private static final String ACTION_SUBSCRIBE = "subscribe";
    private static final String ACTION_SET_MODE = "mode";
    private static final String MODE_FULL = "full";
    private static final String MODE_QUOTE = "quote";
    private static final String MODE_LTP = "ltp";
    private static final String TYPE_ORDER = "order";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DATA = "data";

    private volatile WebSocket webSocket;
    private final AsyncHttpClient httpClient;
    private final KiteCredentials credentials;
    private final KiteDataPublisher dataPublisher;
    private final Supplier<String> enctokenSupplier;
    private final Collection<KiteSymbol> symbols = new HashSet<>();
    private final ScheduledExecutorService scheduler;
    private final KiteBinaryParser binaryParser = new KiteBinaryParser();
    private volatile ScheduledFuture<?> reconnectFuture;
    private volatile ScheduledFuture<?> pingFuture;

    public KiteWebSocketApi(KiteCredentials credentials,
                            KiteDataPublisher dataPublisher,
                            Supplier<String> enctokenSupplier) {
        this.credentials = credentials;
        this.dataPublisher = dataPublisher;
        this.enctokenSupplier = enctokenSupplier;
        this.httpClient = buildHttpClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kite-ws-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    private static AsyncHttpClient buildHttpClient() {
        String proxyHost = System.getProperty("https.proxyHost", System.getProperty("http.proxyHost", ""));
        String proxyUser = System.getProperty("https.proxyUser", System.getProperty("http.proxyUser", ""));
        String proxyPass = System.getProperty("https.proxyPassword", System.getProperty("http.proxyPassword", ""));
        String proxyPort = System.getProperty("https.proxyPort", System.getProperty("http.proxyPort", "8080"));

        DefaultAsyncHttpClientConfig.Builder config = Dsl.config().setKeepAlive(true);

        if (!proxyHost.isEmpty()) {
            int port = Integer.parseInt(proxyPort);
            ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(proxyHost, port)
                    .setProxyType(ProxyType.HTTP);
            if (!proxyUser.isEmpty()) {
                Realm realm = new Realm.Builder(proxyUser, proxyPass)
                        .setScheme(Realm.AuthScheme.NTLM)
                        .setNtlmDomain("")
                        .setNtlmHost("")
                        .build();
                proxyBuilder.setRealm(realm);
            }
            config.setProxyServer(proxyBuilder.build());
        }

        return Dsl.asyncHttpClient(config);
    }

    public synchronized void start() {
        if (webSocket == null || !webSocket.isOpen()) {
            connect();
        }
    }

    @Override
    public void close() {
        cancelFuture(reconnectFuture);
        cancelFuture(pingFuture);
        if (webSocket != null) {
            webSocket.sendCloseFrame();
            webSocket = null;
        }
        scheduler.shutdownNow();
        try {
            httpClient.close();
        } catch (Exception ignored) {
        }
    }

    public void subscribe(Collection<KiteSymbol> newSymbols) {
        symbols.addAll(newSymbols);
        if (webSocket != null && webSocket.isOpen()) {
            List<Integer> tokens = newSymbols.stream().map(KiteSymbol::getInstrumentToken).toList();
            webSocket.sendTextFrame(createSubscribeMessage(tokens));
            webSocket.sendTextFrame(createModeMessage(tokens, KiteQuoteMode.FULL));
        }
    }

    private void connect() {
        try {
            String enc = URLEncoder.encode(enctokenSupplier.get(), StandardCharsets.UTF_8);
            String wsUrl = URL_WS + "/?api_key=" + API_KEY
                    + "&user_id=" + credentials.getUsername()
                    + "&enctoken=" + enc
                    + "&uid=" + new Date().getTime()
                    + "&user-agent=" + USER_AGENT_KITE
                    + "&version=" + WS_VERSION;
            httpClient.prepareGet(wsUrl)
                    .setHeader("User-Agent", USER_AGENT_CHROME)
                    .execute(new WebSocketUpgradeHandler.Builder()
                            .addWebSocketListener(new WebSocketListener() {
                                @Override
                                public void onOpen(WebSocket ws) {
                                    webSocket = ws;
                                    scheduleReconnect();
                                    schedulePing();
                                    subscribe(symbols);
                                }

                                @Override
                                public void onClose(WebSocket ws, int code, String reason) {
                                    webSocket = null;
                                    connect();
                                }

                                @Override
                                public void onError(Throwable t) {
                                    webSocket = null;
                                    connect();
                                }

                                @Override
                                public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
                                    if (finalFragment) {
                                        binaryParser.parseBinary(payload).forEach(dataPublisher::publish);
                                    }
                                }

                                @Override
                                public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                                    if (finalFragment) {
                                        handleTextMessage(payload);
                                    }
                                }
                            })
                            .build());
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
            if (webSocket != null && webSocket.isOpen()) {
                webSocket.sendPingFrame();
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
