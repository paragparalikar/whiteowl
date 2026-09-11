package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Filters the LWMS-qualified universe to the daily "Stocks in Play" by RVOL.
 *
 * <p>RVOL (Relative Volume) measures how much volume a stock is trading relative
 * to its recent average. Zarattini et al. (2024) showed this filter alone lifts
 * ORB returns from 3.2% to 41.6% annually.</p>
 *
 * <p>Phase 1 (daily data only): {@code RVOL = Volume / SMA(Volume, 20)}<br>
 * Phase 2 (intraday data): {@code RVOL = Volume_first_15min / mean(Volume_first_15min, 14d)}</p>
 *
 * <p>Symbols with RVOL &ge; threshold (default 1.5) are kept, ranked by RVOL,
 * and the top N (default 30) are selected.</p>
 */
@Slf4j
public final class RvolFilter {

    private static final int VOLUME_SMA_PERIOD = 20;

    private final float minRvol;
    private final int maxStocks;

    public RvolFilter() {
        this(1.5f, 30);
    }

    /**
     * @param minRvol   minimum RVOL to qualify (default: 1.5)
     * @param maxStocks maximum number of stocks to keep per day (default: 30)
     */
    public RvolFilter(float minRvol, int maxStocks) {
        this.minRvol = minRvol;
        this.maxStocks = maxStocks;
    }

    /**
     * Filter a date's qualified symbols by RVOL, returning only the "Stocks in Play".
     *
     * @param universe     the universe data
     * @param dateIndex    which date to filter
     * @param candidates   the LWMS-qualified symbols for this date
     * @param rvolCache    pre-computed RVOL arrays by symbol (populated on first call per symbol)
     * @return list of symbols passing the RVOL filter, ranked by RVOL descending
     */
    public List<String> filter(UniverseDataFrame universe, int dateIndex,
                               Set<String> candidates,
                               Map<String, float[]> rvolCache) {
        List<SymbolRvol> scored = new ArrayList<>();

        for (String symbol : candidates) {
            float[] rvolArr = rvolCache.computeIfAbsent(symbol,
                    s -> computeRvol(universe.getBars(s)));
            if (rvolArr == null) continue;

            int barIdx = universe.getBarIndex(symbol, dateIndex);
            if (barIdx < 0 || barIdx >= rvolArr.length) continue;

            float rvol = rvolArr[barIdx];
            if (Float.isNaN(rvol) || rvol < minRvol) continue;

            scored.add(new SymbolRvol(symbol, rvol));
        }

        // Sort by RVOL descending and take top N
        scored.sort(Comparator.comparingDouble(SymbolRvol::rvol).reversed());
        int keep = Math.min(scored.size(), maxStocks);

        List<String> result = new ArrayList<>(keep);
        for (int i = 0; i < keep; i++) {
            result.add(scored.get(i).symbol());
        }

        return result;
    }

    /**
     * Compute RVOL array for a symbol's bars.
     * Phase 1: RVOL = Volume / SMA(Volume, 20).
     *
     * @return float[] aligned to bars indices, or null if bars insufficient
     */
    float[] computeRvol(Bars bars) {
        if (bars == null || bars.size() < VOLUME_SMA_PERIOD) return null;

        int size = bars.size();

        // Convert volume to float for SMA computation
        float[] volumeFloat = new float[size];
        for (int i = 0; i < size; i++) {
            volumeFloat[i] = (float) bars.getVolume(i);
        }

        float[] volumeSma = IndicatorFunctions.sma(volumeFloat, size, VOLUME_SMA_PERIOD);

        float[] rvol = new float[size];
        for (int i = 0; i < size; i++) {
            if (Float.isNaN(volumeSma[i]) || volumeSma[i] <= 0) {
                rvol[i] = Float.NaN;
            } else {
                rvol[i] = volumeFloat[i] / volumeSma[i];
            }
        }

        return rvol;
    }

    private record SymbolRvol(String symbol, float rvol) {}
}
