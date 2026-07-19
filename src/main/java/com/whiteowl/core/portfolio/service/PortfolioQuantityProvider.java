package com.whiteowl.core.portfolio.service;

import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class PortfolioQuantityProvider {

    private static final PortfolioQuantityProvider INSTANCE = new PortfolioQuantityProvider();

    private final ReadOnlyLongWrapper revision = new ReadOnlyLongWrapper(0);
    private volatile Map<String, Integer> quantityMap = Map.of();
    private volatile KiteBrokerAdapter adapter;

    private PortfolioQuantityProvider() {
    }

    public static PortfolioQuantityProvider getInstance() {
        return INSTANCE;
    }

    public ReadOnlyLongProperty revisionProperty() {
        return revision.getReadOnlyProperty();
    }

    public int getQuantity(String scripId) {
        return quantityMap.getOrDefault(scripId, 0);
    }

    public void onAdapterChanged(Optional<KiteBrokerAdapter> adapterOpt) {
        this.adapter = adapterOpt.orElse(null);
        if (this.adapter == null) {
            quantityMap = Map.of();
            Platform.runLater(() -> revision.set(revision.get() + 1));
            return;
        }
        refresh();
    }

    public void refresh() {
        KiteBrokerAdapter current = adapter;
        if (current == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                quantityMap = Map.copyOf(EffectiveQuantityCalculator.computeAll(
                        current.fetchHoldings(), current.fetchPositions()));
                Platform.runLater(() -> revision.set(revision.get() + 1));
            } catch (Exception e) {
                log.warn("Failed to refresh portfolio quantities", e);
            }
        });
    }

}
