package com.whiteowl.core.bar.aggregation;

import lombok.Getter;

@Getter
public final class LiveBar {

    private final long barStartTimestamp;
    private float open;
    private float high;
    private float low;
    private float close;
    private long volume;
    private long lastCumulativeVolume;

    public LiveBar(long barStartTimestamp, float price, long cumulativeVolume) {
        this.barStartTimestamp = barStartTimestamp;
        this.open = price;
        this.high = price;
        this.low = price;
        this.close = price;
        this.volume = 0;
        this.lastCumulativeVolume = cumulativeVolume;
    }

    public void update(float price, long cumulativeVolume) {
        close = price;
        if (price > high) high = price;
        if (price < low) low = price;
        if (cumulativeVolume > lastCumulativeVolume) {
            volume += (cumulativeVolume - lastCumulativeVolume);
            lastCumulativeVolume = cumulativeVolume;
        }
    }

    /**
     * Merges a completed sub-bar (e.g., a 1-minute bar) into this bar.
     * Used by IntradayBarAggregator to build higher-timeframe bars.
     */
    public void merge(float barOpen, float barHigh, float barLow, float barClose, long barVolume) {
        close = barClose;
        if (barHigh > high) high = barHigh;
        if (barLow < low) low = barLow;
        volume += barVolume;
    }

}
