package com.whiteowl.client.kite;

import com.whiteowl.client.kite.model.KiteCandle;
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
import com.whiteowl.client.kite.model.KitePosition;
import com.whiteowl.client.kite.model.KiteProfile;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.client.kite.session.KiteSessionStore;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public final class KiteLazyApi implements KiteApi {

    private final Supplier<KiteCredentials> credentialsSupplier;
    private final KiteSessionStore sessionStore;
    private volatile KiteApi delegate;

    public KiteLazyApi(Supplier<KiteCredentials> credentialsSupplier) {
        this(credentialsSupplier, null);
    }

    public KiteLazyApi(Supplier<KiteCredentials> credentialsSupplier, KiteSessionStore sessionStore) {
        this.credentialsSupplier = credentialsSupplier;
        this.sessionStore = sessionStore;
    }

    @Override
    public void init() {
        ensureInitialized();
    }

    @Override
    public void close() throws Exception {
        KiteApi api = delegate;
        if (api != null) {
            api.close();
        }
    }

    @Override
    public List<KiteCandle> getHistoricalData(KiteSymbol symbol, KiteInterval interval, ZonedDateTime from, ZonedDateTime to) {
        return ensureInitialized().getHistoricalData(symbol, interval, from, to);
    }

    @Override
    public KiteProfile getProfile() {
        return ensureInitialized().getProfile();
    }

    @Override
    public KiteMargin getMargin() {
        return ensureInitialized().getMargin();
    }

    @Override
    public List<KiteHolding> getHoldings() {
        return ensureInitialized().getHoldings();
    }

    @Override
    public List<KitePosition> getPositions() {
        return ensureInitialized().getPositions();
    }

    @Override
    public List<KiteOrder> getOrders() {
        return ensureInitialized().getOrders();
    }

    @Override
    public KiteOrderId createOrder(KiteOrder order) {
        return ensureInitialized().createOrder(order);
    }

    @Override
    public KiteOrderId updateOrder(KiteOrder order) {
        return ensureInitialized().updateOrder(order);
    }

    @Override
    public KiteOrderId cancelOrder(KiteOrder order) {
        return ensureInitialized().cancelOrder(order);
    }

    @Override
    public Map<String, KiteQuote> getQuotes(Collection<KiteSymbol> instruments, KiteQuoteMode mode) {
        return ensureInitialized().getQuotes(instruments, mode);
    }

    @Override
    public void subscribe(Collection<KiteSymbol> symbols) {
        ensureInitialized().subscribe(symbols);
    }

    @Override
    public List<KiteGttTrigger> getGttTriggers() {
        return ensureInitialized().getGttTriggers();
    }

    @Override
    public KiteGttTrigger getGttTrigger(int triggerId) {
        return ensureInitialized().getGttTrigger(triggerId);
    }

    @Override
    public KiteGttTriggerId createGttTrigger(KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return ensureInitialized().createGttTrigger(condition, orders, type, expiresAt);
    }

    @Override
    public KiteGttTriggerId updateGttTrigger(int triggerId, KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return ensureInitialized().updateGttTrigger(triggerId, condition, orders, type, expiresAt);
    }

    @Override
    public KiteGttTriggerId deleteGttTrigger(int triggerId) {
        return ensureInitialized().deleteGttTrigger(triggerId);
    }

    private KiteApi ensureInitialized() {
        KiteApi api = delegate;
        if (api != null) return api;
        synchronized (this) {
            if (delegate != null) return delegate;
            log.info("Lazily initializing Kite API");
            KiteCredentials credentials = credentialsSupplier.get();
            KiteApi httpApi = new KiteHttpApi(credentials, sessionStore);
            KiteApi resilientApi = new KiteResilientApi(httpApi);
            resilientApi.init();
            delegate = resilientApi;
            log.info("Kite API initialized lazily");
            return delegate;
        }
    }

}
