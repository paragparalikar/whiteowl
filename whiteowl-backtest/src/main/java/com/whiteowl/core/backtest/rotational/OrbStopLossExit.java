package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: stop loss at the opening range boundary.
 *
 * <ul>
 *   <li>LONG trades: stop at OR-low (exit if bar low &le; OR-low)</li>
 *   <li>SHORT trades: stop at OR-high (exit if bar high &ge; OR-high)</li>
 * </ul>
 */
public final class OrbStopLossExit implements ExitRule {

    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        if (ctx.side() == RotationalTrade.Side.LONG) {
            if (bars.getLow(barIdx) <= ctx.orLow()) {
                return new ExitSignal(ctx.orLow(), ExitReason.STOP_LOSS);
            }
        } else {
            if (bars.getHigh(barIdx) >= ctx.orHigh()) {
                return new ExitSignal(ctx.orHigh(), ExitReason.STOP_LOSS);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "OrbStopLoss";
    }
}
