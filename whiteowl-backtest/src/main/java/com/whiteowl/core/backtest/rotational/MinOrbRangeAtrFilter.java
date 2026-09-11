package com.whiteowl.core.backtest.rotational;

/**
 * Entry filter: reject trades where the opening range is narrower than
 * a minimum multiple of ATR(14).
 *
 * <p>Example: {@code new MinOrbRangeAtrFilter(1.5)} requires the OR
 * (high - low) to be at least 1.5x the prior-day ATR(14).</p>
 */
public final class MinOrbRangeAtrFilter implements EntryFilter {

    private final double minAtrMultiple;

    /**
     * @param minAtrMultiple minimum OR range as a multiple of ATR(14).
     *                       E.g. 1.5 means OR must be >= 1.5 * ATR.
     */
    public MinOrbRangeAtrFilter(double minAtrMultiple) {
        if (minAtrMultiple < 0) {
            throw new IllegalArgumentException("minAtrMultiple must be >= 0, got: " + minAtrMultiple);
        }
        this.minAtrMultiple = minAtrMultiple;
    }

    @Override
    public boolean accept(EntryFilterContext ctx) {
        double orRangeAtr = ctx.orRangeAtr();
        if (Double.isNaN(orRangeAtr)) return false; // no ATR data — skip
        return orRangeAtr >= minAtrMultiple;
    }

    @Override
    public String toString() {
        return String.format("MinOR(%.1fx ATR)", minAtrMultiple);
    }
}
