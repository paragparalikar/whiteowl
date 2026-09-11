package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.tick.model.Tick;
import com.whiteowl.core.tick.model.TickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builds 1-minute bars from incoming ticks for a single scrip.
 * When a 1-minute bar is completed (tick crosses into the next minute boundary),
 * the bar is finalized and published to all registered listeners.
 */
@Slf4j
public final class TickBarAggregator implements TickListener {

    private static final long ONE_MINUTE_MS = Timeframe.ONE_MINUTE.getSeconds() * 1000L;

    private final String scripId;
    private final List<BarCompletionListener> listeners = new CopyOnWriteArrayList<>();
    private volatile LiveBar activeBar;

    public TickBarAggregator(String scripId) {
        this.scripId = scripId;
    }

    public void addListener(BarCompletionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BarCompletionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onTick(Tick tick) {
        if (!scripId.equals(tick.getScripId())) return;

        long tickTime = tick.getTimestamp();
        long barStart = alignToMinute(tickTime);
        float price = tick.getLastTradedPrice();
        long cumulativeVolume = tick.getVolume();

        LiveBar bar = activeBar;
        if (bar == null || bar.getBarStartTimestamp() != barStart) {
            if (bar != null) {
                completeBar(bar);
            }
            activeBar = new LiveBar(barStart, price, cumulativeVolume);
        } else {
            bar.update(price, cumulativeVolume);
            notifyBarUpdated(bar);
        }
    }

    private void completeBar(LiveBar bar) {
        for (BarCompletionListener listener : listeners) {
            try {
                listener.onBarCompleted(scripId, Timeframe.ONE_MINUTE, bar.getBarStartTimestamp(),
                        bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
            } catch (Exception e) {
                log.warn("Error in bar completion listener", e);
            }
        }
    }

    private void notifyBarUpdated(LiveBar bar) {
        for (BarCompletionListener listener : listeners) {
            try {
                listener.onBarUpdated(scripId, Timeframe.ONE_MINUTE, bar.getBarStartTimestamp(),
                        bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
            } catch (Exception e) {
                log.warn("Error in bar update listener", e);
            }
        }
    }

    /**
     * Force-completes the current active bar (e.g., at market close or shutdown).
     */
    public void flush() {
        LiveBar bar = activeBar;
        if (bar != null) {
            completeBar(bar);
            activeBar = null;
        }
    }

    public LiveBar getActiveBar() {
        return activeBar;
    }

    static long alignToMinute(long epochMillis) {
        return (epochMillis / ONE_MINUTE_MS) * ONE_MINUTE_MS;
    }

}
