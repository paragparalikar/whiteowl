package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Entry condition: wait for a candle to <em>close</em> outside the opening range.
 *
 * <ul>
 *   <li>LONG: candle close &gt; OR-high &rarr; enter at candle close</li>
 *   <li>SHORT: candle close &lt; OR-low &rarr; enter at candle close</li>
 * </ul>
 *
 * <p>The confirmation candle timeframe is implicitly the bar timeframe used
 * by the backtest (e.g. 5-minute bars). If you need a different confirmation
 * timeframe, pass bars of that timeframe to the simulator.</p>
 */
public final class CandleCloseEntry implements EntryCondition {

    @Override
    public EntrySignal check(Bars bars, int barIdx, float orHigh, float orLow) {
        float close = bars.getClose(barIdx);
        if (close > orHigh) {
            return new EntrySignal(RotationalTrade.Side.LONG, close);
        }
        if (close < orLow) {
            return new EntrySignal(RotationalTrade.Side.SHORT, close);
        }
        return null;
    }

    @Override
    public String toString() {
        return "CANDLE_CLOSE";
    }
}
