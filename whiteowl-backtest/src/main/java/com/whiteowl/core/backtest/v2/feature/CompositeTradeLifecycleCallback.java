package com.whiteowl.core.backtest.v2.feature;

import java.util.List;

/**
 * Fans lifecycle events out to multiple callbacks. A single
 * {@link TradeLifecycleCallback} slot on the engine can therefore feed both
 * excursion collection and feature capture in one run.
 */
public final class CompositeTradeLifecycleCallback implements TradeLifecycleCallback {

    private final List<TradeLifecycleCallback> callbacks;

    public CompositeTradeLifecycleCallback(List<TradeLifecycleCallback> callbacks) {
        this.callbacks = List.copyOf(callbacks);
    }

    public static TradeLifecycleCallback of(TradeLifecycleCallback... callbacks) {
        return new CompositeTradeLifecycleCallback(List.of(callbacks));
    }

    @Override
    public void onTradeEvent(TradeLifecycleEvent event) {
        for (TradeLifecycleCallback c : callbacks) {
            c.onTradeEvent(event);
        }
    }

}
