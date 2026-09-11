package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: OR Internal Bar Strength (IBS).
 *
 * <p>{@code OR_IBS = (orClose - orLow) / (orHigh - orLow)}</p>
 *
 * <p>Ranges from 0 to 1:
 * <ul>
 *   <li>IBS near 1.0: OR closed near its high (bullish close)</li>
 *   <li>IBS near 0.0: OR closed near its low (bearish close)</li>
 * </ul>
 *
 * <p>Note: The strategy already filters on IBS <= 0.90, so all trades
 * will have IBS in [0, 0.90]. This feature lets us analyze whether
 * further IBS segmentation improves performance.</p>
 */
public final class OrIbsFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_IBS";
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

        if (orHigh <= 0 || orLow <= 0 || orClose <= 0) return Double.NaN;

        float spread = orHigh - orLow;
        if (spread <= 0) return Double.NaN;

        return (orClose - orLow) / spread;
    }
}
