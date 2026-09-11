package com.whiteowl.core.backtest.rotational;

/**
 * Entry filter: require the IBS (Internal Bar Strength) of the opening range
 * to be within a min/max range.
 *
 * <p>IBS = (close_of_last_OR_bar - OR_low) / (OR_high - OR_low).</p>
 * <ul>
 *   <li>IBS near 1.0: OR closed near its high (bullish bias)</li>
 *   <li>IBS near 0.0: OR closed near its low (bearish bias)</li>
 * </ul>
 */
public final class OrbIbsFilter implements EntryFilter {

    private final Double min;
    private final Double max;

    /**
     * @param min minimum IBS (null = no minimum)
     * @param max maximum IBS (null = no maximum)
     */
    public OrbIbsFilter(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean accept(EntryFilterContext ctx) {
        float ibs = ctx.orbIbs();
        if (Float.isNaN(ibs)) return false;
        if (min != null && ibs < min) return false;
        if (max != null && ibs > max) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("IBS[%s, %s]",
                min != null ? String.format("%.2f", min) : "0",
                max != null ? String.format("%.2f", max) : "1");
    }
}
