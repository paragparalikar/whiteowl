package com.whiteowl.core.backtest.v2.engine;

import java.util.List;

public final class PortfolioState {

    private float cash;
    private float initialCapital;
    private final PositionTracker tracker;
    private float cumulativePnl;

    public PortfolioState(float initialCapital, PositionTracker tracker) {
        this.initialCapital = initialCapital;
        this.cash = initialCapital;
        this.tracker = tracker;
        this.cumulativePnl = 0f;
    }

    public float getCash() {
        return cash;
    }

    public void setCash(float cash) {
        this.cash = cash;
    }

    public void adjustCash(float amount) {
        this.cash += amount;
    }

    public float getInitialCapital() {
        return initialCapital;
    }

    public float getCumulativePnl() {
        return cumulativePnl;
    }

    public void addPnl(float pnl) {
        this.cumulativePnl += pnl;
    }

    public float getEquity(float currentPrice) {
        return cash + tracker.unrealizedPnl(currentPrice);
    }

    public boolean hasOpenPositions() {
        return tracker.hasPositions();
    }

    public int positionCount() {
        return tracker.positionCount();
    }

    public int totalSize() {
        return tracker.totalSize();
    }

    public float avgPrice() {
        return tracker.avgPrice();
    }

    public float unrealizedPnl(float currentPrice) {
        return tracker.unrealizedPnl(currentPrice);
    }

    public List<OpenPosition> getOpenPositions() {
        return tracker.getPositions();
    }

    public int latestEntryBarIndex() {
        return tracker.latestEntryBarIndex();
    }

}
