package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Entry condition: enter immediately when price crosses the opening range boundary.
 *
 * <ul>
 *   <li>LONG: bar high &gt; OR-high &rarr; enter at OR-high</li>
 *   <li>SHORT: bar low &lt; OR-low &rarr; enter at OR-low</li>
 * </ul>
 */
public final class BreakoutEntry implements EntryCondition {

    @Override
    public EntrySignal check(Bars bars, int barIdx, float orHigh, float orLow) {
        if (bars.getHigh(barIdx) > orHigh) {
            return new EntrySignal(RotationalTrade.Side.LONG, orHigh);
        }
        if (bars.getLow(barIdx) < orLow) {
            return new EntrySignal(RotationalTrade.Side.SHORT, orLow);
        }
        return null;
    }

    @Override
    public String toString() {
        return "BREAKOUT";
    }
}
