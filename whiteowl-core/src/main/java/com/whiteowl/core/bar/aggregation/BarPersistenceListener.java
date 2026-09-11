package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Persists completed bars to the BarsRepository.
 * Appends each completed bar to the existing binary file for the scrip/timeframe.
 */
@Slf4j
@RequiredArgsConstructor
public final class BarPersistenceListener implements BarCompletionListener {

    private final BarsRepository barsRepository;

    @Override
    public void onBarCompleted(String scripId, Timeframe timeframe, long timestamp,
                               float open, float high, float low, float close, long volume) {
        try {
            Bars bar = new Bars(scripId, timeframe, 1);
            bar.append(timestamp, open, high, low, close, volume);
            barsRepository.append(scripId, timeframe, bar, 0, 1);
            log.debug("Persisted {} bar at {} for {}", timeframe, timestamp, scripId);
        } catch (IOException e) {
            log.error("Failed to persist {} bar for {}", timeframe, scripId, e);
        }
    }

}
