package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Universe filter that ranks stocks by a composite score of characteristics
 * discovered to predict ORB trade performance.
 *
 * <p>The composite score is built from four percentile-ranked characteristics
 * (equal weight), computed over a rolling lookback window of daily bars:</p>
 * <ul>
 *   <li><b>Avg Spread %</b> — (high − low) / close. Higher is better.</li>
 *   <li><b>Avg ATR(14) % of Price</b> — ATR(14) / close. Higher is better.</li>
 *   <li><b>Median Close Price</b> — lower is better (rank inverted).</li>
 *   <li><b>Avg Daily Volume</b> — lower is better (rank inverted).</li>
 * </ul>
 *
 * <p>A minimum turnover hard filter is applied first to ensure tradability.
 * Then the composite score is cross-sectionally ranked per date and the top N
 * stocks are retained.</p>
 *
 * <p>Designed to be a drop-in replacement for {@link TurnoverUniverseFilter}
 * in ORB backtests.</p>
 */
@Slf4j
public final class OrbCharacteristicFilter {

    private static final int ATR_PERIOD = 14;

    private final FilterConfig config;

    public OrbCharacteristicFilter() {
        this(FilterConfig.builder().build());
    }

    public OrbCharacteristicFilter(FilterConfig config) {
        this.config = config;
    }

    /**
     * Score all symbols by the composite ORB characteristic metric and return
     * a ranked list (best first). This is a static, whole-history ranking —
     * it computes averages over the entire bar history available.
     *
     * @param allBars  daily bars by scrip ID
     * @return list of scored symbols, sorted by composite score descending
     */
    public List<ScoredSymbol> scoreAndRank(Map<String, Bars> allBars) {
        List<RawStats> rawList = new ArrayList<>();

        for (var entry : allBars.entrySet()) {
            String symbol = entry.getKey();
            Bars bars = entry.getValue();
            int size = bars.size();
            if (size < config.minBars) continue;

            float[] atr14 = IndicatorFunctions.atr(
                    bars.arrays().high(), bars.arrays().low(),
                    bars.arrays().close(), size, ATR_PERIOD);

            double sumSpread = 0, sumAtrPct = 0, sumVolume = 0, sumTurnover = 0;
            int validDays = 0;
            double[] closes = new double[size];

            for (int i = 1; i < size; i++) {
                float high = bars.getHigh(i);
                float low = bars.getLow(i);
                float close = bars.getClose(i);
                long volume = bars.getVolume(i);

                if (close <= 0 || high <= low) continue;
                closes[validDays] = close;
                validDays++;

                sumSpread += (high - low) / close * 100.0;
                sumTurnover += (double) close * volume / 100_000.0;
                sumVolume += volume;
                if (atr14 != null && i < atr14.length && atr14[i] > 0)
                    sumAtrPct += atr14[i] / close * 100.0;
            }

            if (validDays < config.minBars) continue;

            double avgTurnover = sumTurnover / validDays;
            if (avgTurnover < config.minTurnoverLakhs) continue;

            double[] validCloses = Arrays.copyOf(closes, validDays);
            Arrays.sort(validCloses);

            rawList.add(new RawStats(symbol,
                    sumSpread / validDays,
                    sumAtrPct / validDays,
                    validCloses[validDays / 2],
                    sumVolume / validDays,
                    avgTurnover));
        }

        int n = rawList.size();
        log.info("OrbCharacteristicFilter: {} / {} stocks passed liquidity floor (>= {}L avg daily turnover)",
                n, allBars.size(), (long) config.getMinTurnoverLakhs());
        if (n == 0) return Collections.emptyList();

        // Sort by each characteristic to compute percentile ranks
        double[] spreadRank = percentileRank(rawList, Comparator.comparingDouble(RawStats::avgSpreadPct));
        double[] atrRank = percentileRank(rawList, Comparator.comparingDouble(RawStats::avgAtrPct));
        // Inverted: lower price is better → sort descending gives high rank to low price
        double[] priceRank = percentileRank(rawList, Comparator.comparingDouble(RawStats::medianPrice).reversed());
        // Inverted: lower volume is better
        double[] volumeRank = percentileRank(rawList, Comparator.comparingDouble(RawStats::avgVolume).reversed());

        // Composite score = weighted average of percentile ranks
        List<ScoredSymbol> scored = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            RawStats r = rawList.get(i);
            double composite = config.wSpread * spreadRank[i]
                    + config.wAtr * atrRank[i]
                    + config.wPrice * priceRank[i]
                    + config.wVolume * volumeRank[i];
            scored.add(new ScoredSymbol(r.symbol(), composite,
                    r.avgSpreadPct(), r.avgAtrPct(), r.medianPrice(),
                    r.avgVolume(), r.avgTurnover()));
        }

        scored.sort(Comparator.comparingDouble(ScoredSymbol::compositeScore).reversed());
        return scored;
    }

    /**
     * Percentile-rank the list by the given comparator.
     * Returns values in [0, 1] where 1 = best rank.
     */
    private static double[] percentileRank(List<RawStats> list, Comparator<RawStats> cmp) {
        int n = list.size();
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> cmp.compare(list.get(a), list.get(b)));

        double[] ranks = new double[n];
        for (int rank = 0; rank < n; rank++) {
            ranks[indices[rank]] = (double) (rank + 1) / n;
        }
        return ranks;
    }

    // ── Records ──────────────────────────────────────────────────────

    private record RawStats(
            String symbol,
            double avgSpreadPct,
            double avgAtrPct,
            double medianPrice,
            double avgVolume,
            double avgTurnover
    ) {}

    public record ScoredSymbol(
            String symbol,
            double compositeScore,
            double avgSpreadPct,
            double avgAtrPct,
            double medianPrice,
            double avgVolume,
            double avgTurnoverLakhs
    ) {}

    /**
     * Configuration for the ORB characteristic filter.
     */
    @Getter
    @Builder
    public static class FilterConfig {
        /** Minimum number of valid daily bars required (default: 100). */
        @Builder.Default
        private final int minBars = 100;

        /**
         * Minimum average daily turnover in lakhs to ensure tradability.
         * Default: 5000L = 50 Cr (SEBI minimum liquidity threshold for institutional trading).
         * Stocks below this are illiquid and incur excessive impact cost.
         */
        @Builder.Default
        private final double minTurnoverLakhs = 5000;

        // ── Composite score weights (should sum to 1.0) ──────────

        /** Weight for Avg Spread % rank (default: 0.30). */
        @Builder.Default
        private final double wSpread = 0.30;

        /** Weight for Avg ATR % of Price rank (default: 0.30). */
        @Builder.Default
        private final double wAtr = 0.30;

        /** Weight for Median Price rank (inverted; default: 0.20). */
        @Builder.Default
        private final double wPrice = 0.20;

        /** Weight for Avg Volume rank (inverted; default: 0.20). */
        @Builder.Default
        private final double wVolume = 0.20;
    }
}
