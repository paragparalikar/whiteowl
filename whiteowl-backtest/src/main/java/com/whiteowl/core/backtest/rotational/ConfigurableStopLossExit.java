package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: configurable stop loss based on OR range or ATR.
 *
 * <p>The stop distance is computed as {@code multiplier * basisValue}, where the
 * basis is either the opening range width or the prior-day ATR(14), as determined
 * by the {@link RotationalBacktestConfig.StopBasis} setting.</p>
 *
 * <ul>
 *   <li>LONG: stop = entry - stopDistance; fires when bar low &le; stop</li>
 *   <li>SHORT: stop = entry + stopDistance; fires when bar high &ge; stop</li>
 * </ul>
 *
 * <p>With {@code StopBasis.OR_RANGE} and {@code multiplier = 1.0}, this is equivalent
 * to the old {@code OrbStopLossExit} (stop at the opposite OR boundary).</p>
 */
public final class ConfigurableStopLossExit implements ExitRule {

    private final RotationalBacktestConfig.StopBasis basis;
    private final double multiplier;
    private double stopPrice;

    public ConfigurableStopLossExit(RotationalBacktestConfig.StopBasis basis, double multiplier) {
        this.basis = basis;
        this.multiplier = multiplier;
    }

    @Override
    public void init(ExitContext ctx) {
        double basisValue = switch (basis) {
            case OR_RANGE -> ctx.orRange();
            case ATR -> Float.isNaN(ctx.dailyAtr()) ? ctx.orRange() : ctx.dailyAtr();
        };
        double stopDist = multiplier * basisValue;

        if (ctx.side() == RotationalTrade.Side.LONG) {
            stopPrice = ctx.entryPrice() - stopDist;
        } else {
            stopPrice = ctx.entryPrice() + stopDist;
        }
    }

    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        if (ctx.side() == RotationalTrade.Side.LONG) {
            if (bars.getLow(barIdx) <= stopPrice) {
                return new ExitSignal(stopPrice, ExitReason.STOP_LOSS);
            }
        } else {
            if (bars.getHigh(barIdx) >= stopPrice) {
                return new ExitSignal(stopPrice, ExitReason.STOP_LOSS);
            }
        }
        return null;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    /**
     * Compute the stop distance for a given context (useful for target calculations).
     */
    public double computeStopDistance(ExitContext ctx) {
        double basisValue = switch (basis) {
            case OR_RANGE -> ctx.orRange();
            case ATR -> Float.isNaN(ctx.dailyAtr()) ? ctx.orRange() : ctx.dailyAtr();
        };
        return multiplier * basisValue;
    }

    @Override
    public String toString() {
        return String.format("Stop(%.1fx %s)", multiplier, basis);
    }
}
