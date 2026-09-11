package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Universe filter using the LWMS (Liquidity-Weighted Movement Score) metric
 * combined with hard filters.
 *
 * <p>LWMS = max(H−O, O−L) × √(C×V). This captures both intraday movement
 * and liquidity in a single metric. The rolling 63-day average is cross-sectionally
 * ranked per date, and the top N (default 500, configurable) are retained.</p>
 *
 * <p>Hard filters additionally require:</p>
 * <ul>
 *   <li>Average daily turnover (ADT = C×V, 20-day SMA) &gt; 50 Cr</li>
 *   <li>ATR(14)/Close &gt; 1.5% (minimum volatility)</li>
 * </ul>
 */
@Slf4j
public final class LwmsUniverseFilter {

    private static final int LWMS_LOOKBACK = 63;      // ~1 quarter
    private static final int ADT_LOOKBACK = 20;
    private static final int ATR_PERIOD = 14;

    private final FilterConfig config;

    public LwmsUniverseFilter() {
        this(FilterConfig.builder().build());
    }

    public LwmsUniverseFilter(FilterConfig config) {
        this.config = config;
    }

    /**
     * Filter the universe to produce a per-date list of qualifying symbols.
     *
     * @param universe     the loaded universe
     * @param excluded     symbols excluded by data cleaning (skipped entirely)
     * @return a FilteredUniverse with per-date qualifying symbol sets
     */
    public FilteredUniverse filter(UniverseDataFrame universe, Set<String> excluded) {
        String[] allSymbols = universe.getSymbols();
        long[] dates = universe.getDates();
        int dateCount = dates.length;

        // Step 1: Compute per-scrip indicator arrays needed for filtering
        Map<String, float[]> lwmsSmoothed = new LinkedHashMap<>();
        Map<String, float[]> adtValues = new LinkedHashMap<>();
        Map<String, float[]> atrValues = new LinkedHashMap<>();

        for (String symbol : allSymbols) {
            if (excluded.contains(symbol)) continue;
            Bars bars = universe.getBars(symbol);
            if (bars == null) continue;
            int size = bars.size();

            // Compute raw LWMS per bar: max(H-O, O-L) * sqrt(C*V)
            float[] rawLwms = new float[size];
            for (int i = 0; i < size; i++) {
                float c = bars.getClose(i);
                long v = bars.getVolume(i);
                float liquidity = (float) Math.sqrt((double) c * v);
                rawLwms[i] = liquidity;
            }

            // Rolling 63-day SMA of LWMS
            float[] smoothed = IndicatorFunctions.sma(rawLwms, size, LWMS_LOOKBACK);
            lwmsSmoothed.put(symbol, smoothed);

            // ADT = C * V, 20-day SMA (in rupees, need to divide by 1e7 for Cr)
            float[] turnover = new float[size];
            for (int i = 0; i < size; i++) {
                turnover[i] = bars.getClose(i) * bars.getVolume(i);
            }
            float[] adt = IndicatorFunctions.sma(turnover, size, ADT_LOOKBACK);
            adtValues.put(symbol, adt);

            // ATR(14)
            float[] atr = IndicatorFunctions.atr(
                    bars.arrays().high(), bars.arrays().low(), bars.arrays().close(),
                    size, ATR_PERIOD);
            atrValues.put(symbol, atr);
        }

        // Step 2: For each date, apply filters
        Map<Integer, Set<String>> qualifyingByDate = new LinkedHashMap<>();
        Set<String> candidates = lwmsSmoothed.keySet();

        for (int d = 0; d < dateCount; d++) {
            // Collect LWMS values for all valid symbols on this date
            List<SymbolScore> scores = new ArrayList<>();

            for (String symbol : candidates) {
                Bars bars = universe.getBars(symbol);
                int barIdx = universe.getBarIndex(symbol, d);
                if (barIdx < 0) continue;

                float smoothedLwms = lwmsSmoothed.get(symbol)[barIdx];
                if (Float.isNaN(smoothedLwms)) continue;

                // Hard filter 1: ADT > threshold (default 50 Cr = 5e8)
                float adt = adtValues.get(symbol)[barIdx];
                if (Float.isNaN(adt) || adt < config.minAdtRupees) continue;

                // Hard filter 2: ATR(14)/Close > threshold (minimum volatility)
                float close = bars.getClose(barIdx);
                float atr = atrValues.get(symbol)[barIdx];
                if (Float.isNaN(atr) || close <= 0 || (atr / close) < config.minAtrPct) continue;

                scores.add(new SymbolScore(symbol, smoothedLwms));
            }

            // Cross-sectional ranking: keep top N by LWMS
            if (!scores.isEmpty()) {
                scores.sort(Comparator.comparingDouble(SymbolScore::score).reversed());
                int keepCount = Math.min(config.topN, scores.size());
                Set<String> qualifying = new LinkedHashSet<>();
                for (int i = 0; i < keepCount; i++) {
                    qualifying.add(scores.get(i).symbol());
                }
                qualifyingByDate.put(d, qualifying);
            }
        }

        log.info("LWMS filter: {} dates with qualifying symbols, avg universe size = {}",
                qualifyingByDate.size(),
                qualifyingByDate.isEmpty() ? 0 :
                        qualifyingByDate.values().stream().mapToInt(Set::size).average().orElse(0));

        return new FilteredUniverse(universe, qualifyingByDate, excluded);
    }

    private record SymbolScore(String symbol, float score) {}

    /**
     * Configuration for the LWMS universe filter.
     */
    @Getter
    @Builder
    public static class FilterConfig {
        /** Minimum average daily turnover in rupees (default: 50 Cr = 5e8). */
        @Builder.Default
        private final float minAdtRupees = 5e8f;

        /** Minimum ATR(14)/Close ratio (default: 0.015 = 1.5%). */
        @Builder.Default
        private final float minAtrPct = 0.015f;

        /** Top N symbols to keep after LWMS ranking per date (default: 500). */
        @Builder.Default
        private final int topN = 500;
    }
}
