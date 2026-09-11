package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: configurable profit target based on stop distance, OR range, or ATR.
 *
 * <p>The target distance is computed as {@code multiplier * basisValue}, where
 * the basis depends on the {@link RotationalBacktestConfig.TargetBasis}:</p>
 * <ul>
 *   <li>{@code STOP_DISTANCE} — target = multiplier * stopDistance (R:R ratio).
 *       Requires a reference to the stop loss exit to compute the stop distance.</li>
 *   <li>{@code OR_RANGE} — target = multiplier * (OR_high - OR_low)</li>
 *   <li>{@code ATR} — target = multiplier * ATR(14)</li>
 * </ul>
 */
public final class ConfigurableTargetExit implements ExitRule {

    private final RotationalBacktestConfig.TargetBasis basis;
    private final double multiplier;
    private final ConfigurableStopLossExit stopRef;  // needed for STOP_DISTANCE basis
    private double targetPrice;

    /**
     * @param basis      what the target distance is measured in
     * @param multiplier target multiplier
     * @param stopRef    reference to the stop loss exit (needed for STOP_DISTANCE basis, may be null otherwise)
     */
    public ConfigurableTargetExit(RotationalBacktestConfig.TargetBasis basis,
                                  double multiplier,
                                  ConfigurableStopLossExit stopRef) {
        this.basis = basis;
        this.multiplier = multiplier;
        this.stopRef = stopRef;
    }

    @Override
    public void init(ExitContext ctx) {
        double basisValue = switch (basis) {
            case STOP_DISTANCE -> {
                if (stopRef == null) {
                    yield ctx.orRange(); // fallback if no stop reference
                }
                yield stopRef.computeStopDistance(ctx);
            }
            case OR_RANGE -> ctx.orRange();
            case ATR -> Float.isNaN(ctx.dailyAtr()) ? ctx.orRange() : ctx.dailyAtr();
        };

        double targetDist = multiplier * basisValue;
        if (ctx.side() == RotationalTrade.Side.LONG) {
            targetPrice = ctx.entryPrice() + targetDist;
        } else {
            targetPrice = ctx.entryPrice() - targetDist;
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
        return String.format("Target(%.1fx %s)", multiplier, basis);
    }
}
