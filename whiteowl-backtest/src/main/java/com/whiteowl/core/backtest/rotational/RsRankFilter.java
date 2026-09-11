package com.whiteowl.core.backtest.rotational;

/**
 * Entry filter: require the cross-sectional RS rank percentile during the
 * opening range to be within a min/max range.
 *
 * <p>RS rank is a percentile (0–100) where 100 = strongest relative strength
 * across the universe during the OR period.</p>
 */
public final class RsRankFilter implements EntryFilter {

    private final Double min;
    private final Double max;

    /**
     * @param min minimum RS rank percentile (null = no minimum)
     * @param max maximum RS rank percentile (null = no maximum)
     */
    public RsRankFilter(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean accept(EntryFilterContext ctx) {
        float rank = ctx.rsRank();
        if (Float.isNaN(rank)) return false;
        if (min != null && rank < min) return false;
        if (max != null && rank > max) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("RS[%s, %s]",
                min != null ? String.format("%.0f", min) : "0",
                max != null ? String.format("%.0f", max) : "100");
    }
}
