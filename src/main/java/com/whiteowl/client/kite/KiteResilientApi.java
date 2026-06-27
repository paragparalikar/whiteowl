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
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class KiteResilientApi implements KiteApi {

    private static final int CANDLE_LIMIT_PER_SECOND = 3;
    private static final int QUOTE_LIMIT_PER_SECOND = 1;
    private static final int ORDER_LIMIT_PER_SECOND = 10;
    private static final int OTHERS_LIMIT_PER_SECOND = 10;
    private static final int ALL_LIMIT_PER_MINUTE = 200;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_WAIT_MS = 1000;

    private final KiteApi delegate;

    private final RateLimiter candleDataRateLimiter = RateLimiter.of("candle-data", RateLimiterConfig.custom()
            .limitForPeriod(CANDLE_LIMIT_PER_SECOND)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofHours(6))
            .build());

    private final RateLimiter quoteRateLimiter = RateLimiter.of("quote", RateLimiterConfig.custom()
            .limitForPeriod(QUOTE_LIMIT_PER_SECOND)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMinutes(1))
            .build());

    private final RateLimiter orderRateLimiter = RateLimiter.of("order", RateLimiterConfig.custom()
            .limitForPeriod(ORDER_LIMIT_PER_SECOND)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMinutes(1))
            .build());

    private final RateLimiter othersRateLimiter = RateLimiter.of("other", RateLimiterConfig.custom()
            .limitForPeriod(OTHERS_LIMIT_PER_SECOND)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMinutes(1))
            .build());

    private final RateLimiter allRateLimiter = RateLimiter.of("all", RateLimiterConfig.custom()
            .limitForPeriod(ALL_LIMIT_PER_MINUTE)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofHours(6))
            .build());

    private final Retry retry = Retry.of("retry", RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .failAfterMaxAttempts(true)
            .waitDuration(Duration.ofMillis(RETRY_WAIT_MS))
            .build());

    private <T> T execute(Supplier<T> supplier, RateLimiter rateLimiter, boolean withRetry) {
        supplier = RateLimiter.decorateSupplier(rateLimiter, supplier);
        supplier = RateLimiter.decorateSupplier(allRateLimiter, supplier);
        if (withRetry) supplier = Retry.decorateSupplier(retry, supplier);
        return supplier.get();
    }

    @Override
    public void init() {
        delegate.init();
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public List<KiteCandle> getHistoricalData(KiteSymbol symbol, KiteInterval interval, ZonedDateTime from, ZonedDateTime to) {
        return execute(() -> delegate.getHistoricalData(symbol, interval, from, to), candleDataRateLimiter, false);
    }

    @Override
    public KiteProfile getProfile() {
        return execute(delegate::getProfile, othersRateLimiter, true);
    }

    @Override
    public KiteMargin getMargin() {
        return execute(delegate::getMargin, othersRateLimiter, true);
    }

    @Override
    public List<KiteHolding> getHoldings() {
        return execute(delegate::getHoldings, othersRateLimiter, true);
    }

    @Override
    public List<KitePosition> getPositions() {
        return execute(delegate::getPositions, othersRateLimiter, true);
    }

    @Override
    public List<KiteOrder> getOrders() {
        return execute(delegate::getOrders, othersRateLimiter, true);
    }

    @Override
    public KiteOrderId createOrder(KiteOrder order) {
        return execute(() -> delegate.createOrder(order), orderRateLimiter, true);
    }

    @Override
    public KiteOrderId updateOrder(KiteOrder order) {
        return execute(() -> delegate.updateOrder(order), orderRateLimiter, true);
    }

    @Override
    public KiteOrderId cancelOrder(KiteOrder order) {
        return execute(() -> delegate.cancelOrder(order), orderRateLimiter, true);
    }

    @Override
    public Map<String, KiteQuote> getQuotes(Collection<KiteSymbol> instruments, KiteQuoteMode mode) {
        return execute(() -> delegate.getQuotes(instruments, mode), quoteRateLimiter, true);
    }

    @Override
    public void subscribe(Collection<KiteSymbol> symbols) {
        delegate.subscribe(symbols);
    }

    @Override
    public List<KiteGttTrigger> getGttTriggers() {
        return execute(delegate::getGttTriggers, orderRateLimiter, true);
    }

    @Override
    public KiteGttTrigger getGttTrigger(int triggerId) {
        return execute(() -> delegate.getGttTrigger(triggerId), orderRateLimiter, true);
    }

    @Override
    public KiteGttTriggerId createGttTrigger(KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return execute(() -> delegate.createGttTrigger(condition, orders, type, expiresAt), orderRateLimiter, true);
    }

    @Override
    public KiteGttTriggerId updateGttTrigger(int triggerId, KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt) {
        return execute(() -> delegate.updateGttTrigger(triggerId, condition, orders, type, expiresAt), orderRateLimiter, true);
    }

    @Override
    public KiteGttTriggerId deleteGttTrigger(int triggerId) {
        return execute(() -> delegate.deleteGttTrigger(triggerId), orderRateLimiter, true);
    }

}
