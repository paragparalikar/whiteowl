package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;

/**
 * Trade feature: overnight gap expressed as a multiple of ATR(14).
 *
 * <p>{@code GAP_ATR = (today's open - prior close) / ATR(14)[prior day]}</p>
 *
 * <p>Positive values indicate a gap-up, negative values a gap-down.
 * This measures how large the opening gap is relative to the stock's recent
 * volatility, which can indicate momentum or mean-reversion tendencies.</p>
 *
 * <p>Both open and ATR use the prior day's close — no lookahead.</p>
 */
public final class GapAtrFeature implements TradeFeature {

    private static final int ATR_PERIOD = 14;

    @Override
    public String name() {
        return "GAP_ATR";
    }

    @Override
    public double compute(TradeContext ctx) {
        Bars dailyBars = ctx.dailyBars();
        int dayIndex = ctx.dayIndex();
        if (dailyBars == null || dayIndex < ATR_PERIOD) return Double.NaN;

        float todayOpen = dailyBars.getOpen(dayIndex);
        float priorClose = dailyBars.getClose(dayIndex - 1);
        if (priorClose <= 0) return Double.NaN;

        float[] atr = IndicatorFunctions.atr(
                dailyBars.arrays().high(),
                dailyBars.arrays().low(),
                dailyBars.arrays().close(),
                dayIndex, ATR_PERIOD);

        float priorAtr = atr[dayIndex - 1];
        if (Float.isNaN(priorAtr) || priorAtr <= 0) return Double.NaN;

        return (todayOpen - priorClose) / priorAtr;
    }
}
