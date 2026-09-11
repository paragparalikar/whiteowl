package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: OR body (absolute) as a percentage of OR close.
 *
 * <p>{@code OR_BODY_PCT = |orClose - orOpen| / orClose * 100}</p>
 *
 * <p>Measures the magnitude of the directional move during the opening range.
 * A large body indicates strong conviction in one direction; a small body
 * (relative to wicks) indicates indecision.</p>
 */
public final class OrBodyFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_BODY_PCT";
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

        float orOpen = intradayBars.getOpen(dayStart);
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);

        if (orClose <= 0) return Double.NaN;

        return (Math.abs(orClose - orOpen) / orClose) * 100.0;
    }
}
