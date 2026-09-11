package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: distance between OR close and OR low (lower wick),
 * expressed as a percentage of OR close.
 *
 * <p>{@code OR_CLOSE_LOW_PCT = (orClose - orLow) / orClose * 100}</p>
 *
 * <p>This measures how far the OR low is below where the OR candle closed.
 * A small value means the OR closed near its low (bearish close), while
 * a larger value means price recovered significantly from the OR low
 * during the opening range.</p>
 *
 * <p>This is relevant for short-side limit order fill analysis: if we place
 * a limit sell at OR low after seeing the OR close, a smaller distance means
 * the limit price is closer to the current market price (higher fill
 * probability), while a larger distance means price needs to drop more to
 * reach the limit.</p>
 */
public final class OrCloseToLowDistanceFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_CLOSE_LOW_PCT";
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

        // OR low from the trade record
        float orLow = ctx.trade().orLow();
        if (orLow <= 0) return Double.NaN;

        // OR close is the close of the last bar in the opening range
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);
        if (orClose <= 0) return Double.NaN;

        // Distance as percentage of OR close
        return ((orClose - orLow) / orClose) * 100.0;
    }
}
