package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

/**
 * Trade feature: distance between OR high and OR close, expressed as
 * a percentage of OR close.
 *
 * <p>{@code OR_HIGH_CLOSE_PCT = (orHigh - orClose) / orClose * 100}</p>
 *
 * <p>This measures how far the OR high is from where the OR candle closed.
 * A small value means the OR closed near its high (bullish close), while
 * a larger value means price pulled back significantly from the OR high
 * during the opening range.</p>
 *
 * <p>This is relevant for limit order fill analysis: if we place a limit
 * buy at OR high after seeing the OR close, a smaller distance means the
 * limit price is closer to the current market price (higher fill probability),
 * while a larger distance means price needs to rally more to reach the limit.</p>
 */
public final class OrHighToCloseDistanceFeature implements TradeFeature {

    @Override
    public String name() {
        return "OR_HIGH_CLOSE_PCT";
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

        // OR high from the trade record
        float orHigh = ctx.trade().orHigh();
        if (orHigh <= 0) return Double.NaN;

        // OR close is the close of the last bar in the opening range
        int lastOrBarIndex = dayStart + orBarCount - 1;
        float orClose = intradayBars.getClose(lastOrBarIndex);
        if (orClose <= 0) return Double.NaN;

        // Distance as percentage of OR close
        return ((orHigh - orClose) / orClose) * 100.0;
    }
}
