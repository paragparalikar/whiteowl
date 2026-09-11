package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: OR spread (full range) as a percentage of OR close.
 *
 * <p>{@code OR_SPREAD_PCT = (orHigh - orLow) / orClose * 100}</p>
 *
 * <p>Measures the total price range during the opening range. A wide spread
 * indicates high volatility during the OR; a narrow spread indicates a
 * tight/compressed OR.</p>
 */
public final class OrSpreadFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_SPREAD_PCT";
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
        float orLow = ctx.trade().orLow();
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);

        if (orClose <= 0 || orHigh <= 0 || orLow <= 0) return Double.NaN;

        return ((orHigh - orLow) / orClose) * 100.0;
    }
}
