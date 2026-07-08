package com.whiteowl.core.account.service;

import com.whiteowl.client.kite.adapter.BrokerAdapterFactory;
import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
import com.whiteowl.core.account.model.Account;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public final class ActiveAccountManager {

    private final AccountService accountService;
    private final List<Consumer<Optional<KiteBrokerAdapter>>> listeners = new CopyOnWriteArrayList<>();
    private Account activeAccount;
    private KiteBrokerAdapter activeAdapter;

    public ActiveAccountManager(AccountService accountService) {
        this.accountService = accountService;
    }

    public Optional<Account> getActiveAccount() {
        return Optional.ofNullable(activeAccount);
    }

    public Optional<KiteBrokerAdapter> getActiveAdapter() {
        return Optional.ofNullable(activeAdapter);
    }

    public boolean isConnected() {
        return activeAdapter != null;
    }

    public List<Account> getAccounts() {
        return accountService.getAccounts();
    }

    public void activate(Account account) {
        if (account == null) {
            deactivate();
            return;
        }
        if (activeAccount != null && activeAccount.getId().equals(account.getId())) {
            log.info("Account already active: {}", account.getName());
            return;
        }
        tearDown();
        log.info("Activating account: {}", account.getName());
        this.activeAccount = account;
        try {
            this.activeAdapter = BrokerAdapterFactory.getInstance().createAdapter(account);
            activeAdapter.init();
            log.info("Broker adapter initialized for account: {}", account.getName());
            notifyListeners(Optional.of(activeAdapter));
        } catch (Exception e) {
            log.error("Failed to initialize broker adapter for account: {}", account.getName(), e);
            this.activeAdapter = null;
            this.activeAccount = null;
            notifyListeners(Optional.empty());
        }
    }

    public void deactivate() {
        if (activeAccount == null) return;
        log.info("Deactivating account: {}", activeAccount.getName());
        tearDown();
        notifyListeners(Optional.empty());
    }

    public void addListener(Consumer<Optional<KiteBrokerAdapter>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Optional<KiteBrokerAdapter>> listener) {
        listeners.remove(listener);
    }

    private void tearDown() {
        if (activeAdapter != null) {
            try {
                activeAdapter.close();
            } catch (Exception e) {
                log.warn("Error closing broker adapter", e);
            }
        }
        activeAdapter = null;
        activeAccount = null;
    }

    private void notifyListeners(Optional<KiteBrokerAdapter> adapter) {
        for (Consumer<Optional<KiteBrokerAdapter>> listener : listeners) {
            try {
                listener.accept(adapter);
            } catch (Exception e) {
                log.error("Error notifying account change listener", e);
            }
        }
    }

}
