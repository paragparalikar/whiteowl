package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Universe filter that selects the top N stocks by average daily turnover
 * (Close × Volume) over a rolling lookback window.
 *
 * <p>No hard filters are applied — the sole criterion is the cross-sectional
 * rank of the rolling average turnover. The default configuration keeps the
 * top 1000 stocks ranked by their 200-day average turnover.</p>
 */
@Slf4j
public final class TurnoverUniverseFilter {

    private final FilterConfig config;

    public TurnoverUniverseFilter() {
        this(FilterConfig.builder().build());
    }

    public TurnoverUniverseFilter(FilterConfig config) {
        this.config = config;
    }

    /**
     * Filter the universe to produce a per-date list of qualifying symbols,
     * ranked by average turnover.
     *
     * @param universe the loaded universe
     * @param excluded symbols excluded by data cleaning (skipped entirely)
     * @return a FilteredUniverse with per-date qualifying symbol sets
     */
    public FilteredUniverse filter(UniverseDataFrame universe, Set<String> excluded) {
        String[] allSymbols = universe.getSymbols();
        long[] dates = universe.getDates();
        int dateCount = dates.length;

        // Step 1: Compute per-scrip rolling average turnover
        Map<String, float[]> avgTurnoverBySymbol = new LinkedHashMap<>();

        for (String symbol : allSymbols) {
            if (excluded.contains(symbol)) continue;
            Bars bars = universe.getBars(symbol);
            if (bars == null) continue;
            int size = bars.size();

            // Turnover = Close × Volume (per bar)
            float[] turnover = new float[size];
            for (int i = 0; i < size; i++) {
                turnover[i] = bars.getClose(i) * bars.getVolume(i);
            }

            // Rolling average over the lookback window
            float[] avgTurnover = IndicatorFunctions.sma(turnover, size, config.lookbackDays);
            avgTurnoverBySymbol.put(symbol, avgTurnover);
        }

        // Step 2: For each date, rank symbols by average turnover and keep top N
        Map<Integer, Set<String>> qualifyingByDate = new LinkedHashMap<>();
        Set<String> candidates = avgTurnoverBySymbol.keySet();

        for (int d = 0; d < dateCount; d++) {
            List<SymbolScore> scores = new ArrayList<>();

            for (String symbol : candidates) {
                int barIdx = universe.getBarIndex(symbol, d);
                if (barIdx < 0) continue;

                float avgTurnover = avgTurnoverBySymbol.get(symbol)[barIdx];
                if (Float.isNaN(avgTurnover)) continue;

                scores.add(new SymbolScore(symbol, avgTurnover));
            }

            // Cross-sectional ranking: keep top N by average turnover
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

        log.info("Turnover filter: {} dates with qualifying symbols, avg universe size = {}",
                qualifyingByDate.size(),
                qualifyingByDate.isEmpty() ? 0 :
                        qualifyingByDate.values().stream().mapToInt(Set::size).average().orElse(0));

        return new FilteredUniverse(universe, qualifyingByDate, excluded);
    }

    private record SymbolScore(String symbol, float score) {}

    /**
     * Configuration for the turnover-based universe filter.
     */
    @Getter
    @Builder
    public static class FilterConfig {
        /** Lookback window in trading days for computing average turnover (default: 200). */
        @Builder.Default
        private final int lookbackDays = 200;

        /** Top N symbols to keep after turnover ranking per date (default: 1000). */
        @Builder.Default
        private final int topN = 500;
    }
}
