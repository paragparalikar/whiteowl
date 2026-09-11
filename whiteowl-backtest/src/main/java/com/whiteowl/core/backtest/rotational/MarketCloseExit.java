package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: close the position at market close (last bar of the day).
 *
 * <p>This rule never fires during bar-by-bar scanning; it acts as the
 * default fallback in {@link OrbSimulator} when no other exit rule triggers
 * before the end of the day.</p>
 *
 * <p>Including this in the exit rules list is optional and serves as
 * documentation of intent. The simulator always falls back to market-close
 * exit when no rule fires.</p>
 */
public final class MarketCloseExit implements ExitRule {

    /**
     * Always returns {@code null} — this rule does not fire on individual bars.
     * The simulator handles the market-close fallback explicitly.
     */
    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        return null;
    }

    @Override
    public String toString() {
        return "MarketClose";
    }
}
