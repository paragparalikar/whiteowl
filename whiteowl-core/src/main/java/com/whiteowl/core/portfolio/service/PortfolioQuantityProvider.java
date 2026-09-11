package com.whiteowl.core.portfolio.service;

import com.whiteowl.core.broker.BrokerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public final class PortfolioQuantityProvider {

    private static final PortfolioQuantityProvider INSTANCE = new PortfolioQuantityProvider();

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile Map<String, Integer> quantityMap = Map.of();
    private volatile BrokerAdapter adapter;

    private PortfolioQuantityProvider() {
    }

    public static PortfolioQuantityProvider getInstance() {
        return INSTANCE;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public int getQuantity(String scripId) {
        return quantityMap.getOrDefault(scripId, 0);
    }

    public void onAdapterChanged(Optional<BrokerAdapter> adapterOpt) {
        this.adapter = adapterOpt.orElse(null);
        if (this.adapter == null) {
            quantityMap = Map.of();
            notifyListeners();
            return;
        }
        refresh();
    }

    public void refresh() {
        BrokerAdapter current = adapter;
        if (current == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                quantityMap = Map.copyOf(EffectiveQuantityCalculator.computeAll(
                        current.fetchHoldings(), current.fetchPositions()));
                notifyListeners();
            } catch (Exception e) {
                log.warn("Failed to refresh portfolio quantities", e);
            }
        });
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception e) {
                log.warn("Error notifying portfolio quantity listener", e);
            }
        }
    }

}
