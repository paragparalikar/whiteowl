package com.whiteowl.workbench.bar.download;

import com.whiteowl.client.api.Candle;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
final class BarDataVerifier {

    private static final float UPPER_GAP_MULTIPLIER = 1.4f;
    private static final float LOWER_GAP_MULTIPLIER = 0.6f;

    private final BarsRepository barsRepository;

    List<Candle> verify(String scripId, Timeframe timeframe, List<Candle> candles) throws InvalidBarDataException {
        verifyAgainstPersistedData(scripId, timeframe, candles);
        return verifyDownloadedData(scripId, timeframe, candles);
    }

    private void verifyAgainstPersistedData(String scripId, Timeframe timeframe, List<Candle> candles)
            throws InvalidBarDataException {
        if (candles.isEmpty()) return;
        try {
            Bars bars = barsRepository.load(scripId, timeframe);
            if (bars == null || bars.size() == 0) return;
            int lastIndex = bars.size() - 1;
            float persistedClose = bars.getClose(lastIndex);
            float downloadedOpen = candles.getFirst().getOpen();
            if (hasGap(persistedClose, downloadedOpen)) {
                throwGapException(scripId, timeframe, candles.getFirst().getTimestamp(), persistedClose, downloadedOpen);
            }
        } catch (IOException ignored) {
        }
    }

    private List<Candle> verifyDownloadedData(String scripId, Timeframe timeframe, List<Candle> candles)
            throws InvalidBarDataException {
        if (candles.size() <= 1) return candles;
        for (int i = candles.size() - 1; i > 0; i--) {
            float previousClose = candles.get(i - 1).getClose();
            float currentOpen = candles.get(i).getOpen();
            if (hasGap(previousClose, currentOpen)) {
                if (hasPersistedData(scripId, timeframe)) {
                    throwGapException(scripId, timeframe, candles.get(i).getTimestamp(), previousClose, currentOpen);
                } else {
                    return candles.subList(i, candles.size());
                }
            }
        }
        return candles;
    }

    private boolean hasPersistedData(String scripId, Timeframe timeframe) {
        return barsRepository.exists(scripId, timeframe);
    }

    private boolean hasGap(float previousClose, float currentOpen) {
        return previousClose > (currentOpen * UPPER_GAP_MULTIPLIER)
                || previousClose < (currentOpen * LOWER_GAP_MULTIPLIER);
    }

    private void throwGapException(String scripId, Timeframe timeframe, long timestamp,
                                   float previousClose, float currentOpen) throws InvalidBarDataException {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        throw InvalidBarDataException.builder()
                .scripId(scripId)
                .timeframe(timeframe.getLabel())
                .timestamp(dateTime)
                .previousClose(previousClose)
                .currentOpen(currentOpen)
                .build();
    }

}
