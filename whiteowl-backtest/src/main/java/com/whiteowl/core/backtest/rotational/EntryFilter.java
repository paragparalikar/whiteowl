package com.whiteowl.core.backtest.rotational;

/**
 * A pre-entry gate evaluated once per day per symbol, after the opening range
 * is computed but before scanning for breakout entries.
 *
 * <p>Use this for conditions that depend on the OR as a whole (e.g. minimum
 * OR width relative to ATR) rather than on individual bars. If {@link #accept}
 * returns {@code false}, the simulator skips the symbol for that day entirely.</p>
 *
 * <h3>Implementing a new filter</h3>
 * <ol>
 *   <li>Create a class that implements {@code EntryFilter}.</li>
 * </ol>
 *
 * @see MinOrbRangeAtrFilter
 * @see MinGapAtrFilter
 * @see OrbRvolFilter
 * @see RsRankFilter
 * @see OrbIbsFilter
 */
public interface EntryFilter {

    /**
     * Evaluate whether a trade should be considered for this symbol on this day.
     *
     * @param ctx context containing the computed OR, symbol, and any
     *            pre-computed per-symbol data (e.g. daily ATR)
     * @return {@code true} if entry scanning should proceed, {@code false} to skip
     */
    boolean accept(EntryFilterContext ctx);

    /**
     * Context passed to entry filters for each day/symbol combination.
     *
     * @param symbol      the scrip ID
     * @param orHigh      opening range high
     * @param orLow       opening range low
     * @param dailyAtr    prior-day ATR(14) for this symbol, or {@code Float.NaN} if unavailable
     * @param todayOpen   today's market open price, or {@code Float.NaN} if unavailable
     * @param priorClose  prior day's closing price, or {@code Float.NaN} if unavailable
     * @param orbIbs      IBS of the opening range: (close_of_last_OR_bar - OR_low) / (OR_high - OR_low),
     *                    or {@code Float.NaN} if unavailable
     * @param orbRvol     relative volume during the OR period (today's OR vol / avg prior OR vol),
     *                    or {@code Float.NaN} if unavailable
     * @param rsRank      cross-sectional RS rank percentile (0-100) during the OR,
     *                    or {@code Float.NaN} if unavailable
     */
    record EntryFilterContext(String symbol, float orHigh, float orLow,
                              float dailyAtr, float todayOpen, float priorClose,
                              float orbIbs, float orbRvol, float rsRank) {

        /** OR range (high - low). */
        public float orRange() {
            return orHigh - orLow;
        }

        /** OR range expressed as a multiple of daily ATR. */
        public double orRangeAtr() {
            if (Float.isNaN(dailyAtr) || dailyAtr <= 0) return Double.NaN;
            return orRange() / dailyAtr;
        }

        /** Gap (today open - prior close) expressed as a multiple of daily ATR. */
        public double gapAtr() {
            if (Float.isNaN(dailyAtr) || dailyAtr <= 0) return Double.NaN;
            if (Float.isNaN(todayOpen) || Float.isNaN(priorClose) || priorClose <= 0) return Double.NaN;
            return (todayOpen - priorClose) / dailyAtr;
        }
    }
}
