package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * A pluggable entry condition for ORB trades.
 *
 * <p>The {@link OrbSimulator} calls {@link #check} on every bar after the
 * opening range is established (up to the entry cutoff time). The first
 * non-null {@link EntrySignal} determines the trade's entry side and price.</p>
 *
 * <h3>Implementing a new entry condition</h3>
 * <ol>
 *   <li>Create a class that implements {@code EntryCondition}.</li>
 *   <li>Wire it into the config via
 *       {@link RotationalBacktestConfig.RotationalBacktestConfigBuilder#entryCondition}.</li>
 * </ol>
 *
 * @see BreakoutEntry
 * @see CandleCloseEntry
 */
public interface EntryCondition {

    /**
     * Evaluate whether a trade should be entered on the given bar.
     *
     * @param bars   the scrip's intraday bars
     * @param barIdx the bar index to evaluate (after the opening range)
     * @param orHigh opening range high
     * @param orLow  opening range low
     * @return an {@link EntrySignal} if entry is triggered, or {@code null} if not
     */
    EntrySignal check(Bars bars, int barIdx, float orHigh, float orLow);

    /**
     * Signal returned when an entry condition fires.
     *
     * @param side       LONG or SHORT
     * @param entryPrice the price at which to enter the trade
     */
    record EntrySignal(RotationalTrade.Side side, double entryPrice) {}
}
