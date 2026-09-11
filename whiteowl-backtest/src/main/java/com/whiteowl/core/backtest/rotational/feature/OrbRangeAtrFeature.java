package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;

/**
 * Trade feature: opening range width expressed as a multiple of ATR(14).
 *
 * <p>{@code OR_RANGE_ATR = (orHigh - orLow) / ATR(14)[prior day]}</p>
 *
 * <p>This measures how wide the opening range is relative to the stock's
 * recent volatility. Useful for filtering trades: very narrow ORs may produce
 * whipsaw entries, while very wide ORs may have stops too far away.</p>
 *
 * <p>ATR is computed from the scrip's daily bars, using the day <em>before</em>
 * the trade date (no lookahead).</p>
 */
public final class OrbRangeAtrFeature implements TradeFeature {

    private static final int ATR_PERIOD = 14;

    @Override
    public String name() {
        return "OR_RANGE_ATR";
    }

    @Override
    public double compute(TradeContext ctx) {
        Bars dailyBars = ctx.dailyBars();
        int dayIndex = ctx.dayIndex();
        if (dailyBars == null || dayIndex < ATR_PERIOD) return Double.NaN;

        float orRange = ctx.trade().orHigh() - ctx.trade().orLow();
        if (orRange <= 0) return Double.NaN;

        float[] atr = IndicatorFunctions.atr(
                dailyBars.arrays().high(),
                dailyBars.arrays().low(),
                dailyBars.arrays().close(),
                dayIndex, ATR_PERIOD);

        float priorAtr = atr[dayIndex - 1];
        if (Float.isNaN(priorAtr) || priorAtr <= 0) return Double.NaN;

        return orRange / priorAtr;
    }
}
