package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * A pluggable intraday exit condition for ORB trades.
 *
 * <p>The {@link OrbSimulator} evaluates a list of {@code ExitRule}s in priority
 * order on every bar after entry. The first rule whose {@link #check} returns
 * a non-null {@link ExitSignal} determines the trade's exit price and
 * {@link ExitReason}.</p>
 *
 * <h3>Implementing a new exit rule</h3>
 * <ol>
 *   <li>Create a class that implements {@code ExitRule}.</li>
 *   <li>Add a corresponding constant to {@link ExitReason} if needed.</li>
 * </ol>
 *
 * @see ConfigurableStopLossExit
 * @see TrailingStopExit
 * @see ConfigurableTargetExit
 * @see TimedExit
 * @see MarketCloseExit
 */
public interface ExitRule {

    /**
     * Called once after entry is confirmed, before bar-by-bar scanning begins.
     * Use this to pre-compute levels (stop price, target price, etc.) that
     * depend on the entry context.
     *
     * <p>Default implementation is a no-op.</p>
     *
     * @param ctx the entry context for this trade
     */
    default void init(ExitContext ctx) {}

    /**
     * Evaluate whether this rule triggers an exit on the given bar.
     *
     * @param bars     the scrip's intraday bars
     * @param barIdx   the bar index to evaluate
     * @param ctx      the entry context for this trade
     * @return an {@link ExitSignal} if this rule fires, or {@code null} if not
     */
    ExitSignal check(Bars bars, int barIdx, ExitContext ctx);

    /**
     * Signal returned when an exit rule fires.
     *
     * @param exitPrice the price at which the trade exits
     * @param reason    why the trade was exited
     */
    record ExitSignal(double exitPrice, ExitReason reason) {}

    /**
     * Context provided to exit rules, describing the current trade's entry.
     *
     * @param side       LONG or SHORT
     * @param entryPrice the price at which the trade was entered
     * @param orHigh     opening range high
     * @param orLow      opening range low
     * @param dailyAtr   prior-day ATR(14), or {@code Float.NaN} if unavailable
     */
    record ExitContext(RotationalTrade.Side side, double entryPrice,
                       float orHigh, float orLow, float dailyAtr) {

        /** OR range (high - low). */
        public float orRange() {
            return orHigh - orLow;
        }
    }
}
