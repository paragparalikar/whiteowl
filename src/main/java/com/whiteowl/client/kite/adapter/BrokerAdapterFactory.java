package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteLazyApi;
import com.whiteowl.client.kite.session.KiteSessionStore;
import com.whiteowl.core.account.model.Account;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BrokerAdapterFactory {

    private static final String DEFAULT_PORTFOLIO_ID = "default";
    private final KiteSessionStore sessionStore = new KiteSessionStore();

    public KiteBrokerAdapter createAdapter(Account account) {
        log.info("Creating broker adapter for account: {}", account.getName());
        KiteCredentials credentials = toCredentials(account);
        KiteLazyApi lazyApi = new KiteLazyApi(() -> credentials, sessionStore);
        return new KiteBrokerAdapter(lazyApi, DEFAULT_PORTFOLIO_ID);
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
