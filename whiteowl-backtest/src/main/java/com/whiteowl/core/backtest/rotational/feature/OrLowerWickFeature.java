package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: OR lower wick as a percentage of OR close.
 *
 * <p>{@code OR_LOWER_WICK_PCT = (min(orOpen, orClose) - orLow) / orClose * 100}</p>
 *
 * <p>Measures price rejection from the bottom of the opening range. A large lower
 * wick indicates buying pressure during the OR -- price probed lower but was
 * pushed back up.</p>
 */
public final class OrLowerWickFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_LOWER_WICK_PCT";
    }

    @Override
    public double compute(TradeContext ctx) {
        Bars intradayBars = ctx.intradayBars();
        int dayStart = ctx.intradayDayStart();
        int dayCount = ctx.intradayDayCount();
        int orBarCount = ctx.orBarCount();

        if (intradayBars == null || dayStart < 0 || dayCount < orBarCount || orBarCount <= 0) {
            return Double.NaN;
        }

        float orLow = ctx.trade().orLow();
        float orOpen = intradayBars.getOpen(dayStart);
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);

        if (orClose <= 0 || orLow <= 0) return Double.NaN;

        float bodyBottom = Math.min(orOpen, orClose);
        return ((bodyBottom - orLow) / orClose) * 100.0;
    }
}
