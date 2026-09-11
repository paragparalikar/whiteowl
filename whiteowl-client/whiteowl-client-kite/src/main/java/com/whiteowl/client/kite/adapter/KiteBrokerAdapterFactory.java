package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.KiteApi;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteLazyApi;
import com.whiteowl.client.kite.session.KiteSessionStore;
import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.broker.BrokerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class KiteBrokerAdapterFactory implements com.whiteowl.core.broker.BrokerAdapterFactory {

    private static final String DEFAULT_PORTFOLIO_ID = "default";
    private static final KiteBrokerAdapterFactory INSTANCE = new KiteBrokerAdapterFactory();

    private final KiteSessionStore sessionStore = new KiteSessionStore();
    private final Map<String, KiteApi> apiCache = new ConcurrentHashMap<>();

    private KiteBrokerAdapterFactory() {
    }

    public static KiteBrokerAdapterFactory getInstance() {
        return INSTANCE;
    }

    public KiteSessionStore getSessionStore() {
        return sessionStore;
    }

    @Override
    public BrokerAdapter createAdapter(Account account) {
        log.info("Creating broker adapter for account: {}", account.getName());
        KiteApi api = apiCache.computeIfAbsent(account.getUserId(), userId -> {
            KiteCredentials credentials = toCredentials(account);
            log.info("Initializing new KiteApi for user={}", userId);
            return new KiteLazyApi(() -> credentials, sessionStore);
        });
        return new KiteBrokerAdapter(api, DEFAULT_PORTFOLIO_ID);
    }

    @Override
    public void closeAll() {
        apiCache.values().forEach(api -> {
            try {
                api.close();
            } catch (Exception e) {
                log.warn("Error closing cached KiteApi", e);
            }
        });
        apiCache.clear();
    }

    private KiteCredentials toCredentials(Account account) {
        return KiteCredentials.builder()
                .portfolioId(DEFAULT_PORTFOLIO_ID)
                .username(account.getUserId())
                .password(account.getPassword())
                .pin(account.getPin())
                .build();
    }

}
