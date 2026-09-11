package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: OR upper wick as a percentage of OR close.
 *
 * <p>{@code OR_UPPER_WICK_PCT = (orHigh - max(orOpen, orClose)) / orClose * 100}</p>
 *
 * <p>Measures price rejection from the top of the opening range. A large upper
 * wick indicates selling pressure during the OR -- price probed higher but was
 * pushed back down.</p>
 */
public final class OrUpperWickFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_UPPER_WICK_PCT";
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

        float orHigh = ctx.trade().orHigh();
        float orOpen = intradayBars.getOpen(dayStart);
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);

        if (orClose <= 0 || orHigh <= 0) return Double.NaN;

        float bodyTop = Math.max(orOpen, orClose);
        return ((orHigh - bodyTop) / orClose) * 100.0;
    }
}
