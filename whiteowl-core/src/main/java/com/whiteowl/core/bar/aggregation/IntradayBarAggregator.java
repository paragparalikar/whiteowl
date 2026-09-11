package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Aggregates completed 1-minute bars into higher intraday timeframes
 * (3m, 5m, 10m, 15m, 30m, 1h, 2h, 3h).
 * Implements BarCompletionListener to receive 1m bar completions.
 */
@Slf4j
public final class IntradayBarAggregator implements BarCompletionListener {

    private static final Timeframe[] HIGHER_TIMEFRAMES = {
            Timeframe.THREE_MINUTE,
            Timeframe.FIVE_MINUTE,
            Timeframe.TEN_MINUTE,
            Timeframe.FIFTEEN_MINUTE,
            Timeframe.THIRTY_MINUTE,
            Timeframe.ONE_HOUR,
            Timeframe.TWO_HOUR,
            Timeframe.THREE_HOUR
    };

    private final String scripId;
    private final Map<Timeframe, LiveBar> activeBars = new EnumMap<>(Timeframe.class);
    private final List<BarCompletionListener> listeners = new CopyOnWriteArrayList<>();

    public IntradayBarAggregator(String scripId) {
        this.scripId = scripId;
    }

    public void addListener(BarCompletionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BarCompletionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onBarCompleted(String scripId, Timeframe timeframe, long timestamp,
                               float open, float high, float low, float close, long volume) {
        if (timeframe != Timeframe.ONE_MINUTE) return;

        for (Timeframe tf : HIGHER_TIMEFRAMES) {
            long barStart = alignToTimeframe(timestamp, tf);
            LiveBar bar = activeBars.get(tf);

            if (bar == null || bar.getBarStartTimestamp() != barStart) {
                if (bar != null) {
                    notifyCompleted(tf, bar);
                }
                bar = new LiveBar(barStart, open, 0);
                bar.merge(open, high, low, close, volume);
                activeBars.put(tf, bar);
            } else {
                bar.merge(open, high, low, close, volume);
            }

            notifyUpdated(tf, bar);
        }
    }

    @Override
    public void onBarUpdated(String scripId, Timeframe timeframe, long timestamp,
                             float open, float high, float low, float close, long volume) {
        // In-progress 1m bar updates are not propagated to higher TF accumulators.
        // Only completed 1m bars should feed higher TFs.
    }

    public void flush() {
        for (Timeframe tf : HIGHER_TIMEFRAMES) {
            LiveBar bar = activeBars.remove(tf);
            if (bar != null) {
                notifyCompleted(tf, bar);
            }
        }
    }

    private void notifyCompleted(Timeframe tf, LiveBar bar) {
        for (BarCompletionListener listener : listeners) {
            try {
                listener.onBarCompleted(scripId, tf, bar.getBarStartTimestamp(),
                        bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
            } catch (Exception e) {
                log.warn("Error in bar completion listener for {}", tf, e);
            }
        }
    }

    private void notifyUpdated(Timeframe tf, LiveBar bar) {
        for (BarCompletionListener listener : listeners) {
            try {
                listener.onBarUpdated(scripId, tf, bar.getBarStartTimestamp(),
                        bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
            } catch (Exception e) {
                log.warn("Error in bar update listener for {}", tf, e);
            }
        }
    }

    static long alignToTimeframe(long epochMillis, Timeframe tf) {
        long intervalMs = tf.getSeconds() * 1000L;
        return (epochMillis / intervalMs) * intervalMs;
    }

}
