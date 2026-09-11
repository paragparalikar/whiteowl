package com.whiteowl.core.backtest.rotational;

/**
 * Entry filter: reject trades where the opening gap (today's open minus
 * prior day's close) is smaller than a minimum multiple of ATR(14).
 *
 * <p>Example: {@code new MinGapAtrFilter(0.80)} requires the gap to be
 * at least 0.80x the prior-day ATR(14). This filters for stocks with
 * meaningful momentum gaps at the open.</p>
 *
 * <p>Note: negative gaps (gap-downs) will be rejected by a positive
 * threshold. To allow gap-downs, use a negative threshold or combine
 * with a separate filter for short-side trades.</p>
 */
public final class MinGapAtrFilter implements EntryFilter {

    private final double minGapAtr;

    /**
     * @param minGapAtr minimum gap as a multiple of ATR(14).
     *                  E.g. 0.80 means gap must be >= 0.80 * ATR.
     */
    public MinGapAtrFilter(double minGapAtr) {
        this.minGapAtr = minGapAtr;
    }

    @Override
    public boolean accept(EntryFilterContext ctx) {
        double gapAtr = ctx.gapAtr();
        if (Double.isNaN(gapAtr)) return false; // no data — skip
        return gapAtr >= minGapAtr;
    }

    @Override
    public String toString() {
        return String.format("MinGap(%.2fx ATR)", minGapAtr);
    }
}
