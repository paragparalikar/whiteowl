package com.whiteowl.core.backtest.rotational;

/**
 * Entry filter: require the relative volume during the opening range to be
 * within a min/max range.
 *
 * <p>RVOL = today's OR volume / average OR volume over the past N days.
 * A high RVOL indicates unusual participation at the open.</p>
 */
public final class OrbRvolFilter implements EntryFilter {

    private final Double min;
    private final Double max;

    /**
     * @param min minimum RVOL (null = no minimum)
     * @param max maximum RVOL (null = no maximum)
     */
    public OrbRvolFilter(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean accept(EntryFilterContext ctx) {
        float rvol = ctx.orbRvol();
        if (Float.isNaN(rvol)) return false;
        if (min != null && rvol < min) return false;
        if (max != null && rvol > max) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("RVOL[%s, %s]",
                min != null ? String.format("%.2f", min) : "-∞",
                max != null ? String.format("%.2f", max) : "+∞");
    }
}
