package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: profit target at a configurable multiple of the stop-loss distance.
 *
 * <p>The stop-loss distance is measured from entry to the OR boundary:</p>
 * <ul>
 *   <li>LONG: stopDist = entry - OR_low &rarr; target = entry + multiple * stopDist</li>
 *   <li>SHORT: stopDist = OR_high - entry &rarr; target = entry - multiple * stopDist</li>
 * </ul>
 *
 * <p>Example: {@code new TargetMultipleExit(2.0)} sets target at 2x risk (1:2 R:R).</p>
 */
public final class TargetMultipleExit implements ExitRule {

    private final double multiple;
    private double targetPrice;

    /**
     * @param multiple reward-to-risk multiple (e.g. 2.0 for 2x stop distance)
     */
    public TargetMultipleExit(double multiple) {
        if (multiple <= 0) {
            throw new IllegalArgumentException("Target multiple must be > 0, got: " + multiple);
        }
        this.multiple = multiple;
    }

    @Override
    public void init(ExitContext ctx) {
        if (ctx.side() == RotationalTrade.Side.LONG) {
            double stopDist = ctx.entryPrice() - ctx.orLow();
            targetPrice = ctx.entryPrice() + multiple * stopDist;
        } else {
            double stopDist = ctx.orHigh() - ctx.entryPrice();
            targetPrice = ctx.entryPrice() - multiple * stopDist;
        }
    }

    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        if (ctx.side() == RotationalTrade.Side.LONG) {
            if (bars.getHigh(barIdx) >= targetPrice) {
                return new ExitSignal(targetPrice, ExitReason.TARGET);
            }
        } else {
            if (bars.getLow(barIdx) <= targetPrice) {
                return new ExitSignal(targetPrice, ExitReason.TARGET);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Target(" + multiple + "x)";
    }
}
