package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

/**
 * Exit rule: trailing stop that ratchets in the favorable direction.
 *
 * <p>The trailing stop distance is computed as {@code multiplier * basisValue},
 * where the basis is either the opening range width or ATR(14).</p>
 *
 * <p>After entry, the stop tracks the highest high (for longs) or lowest low
 * (for shorts) seen so far, and the stop level is always:</p>
 * <ul>
 *   <li>LONG: stopLevel = highestHigh - trailDist (ratchets up, never down)</li>
 *   <li>SHORT: stopLevel = lowestLow + trailDist (ratchets down, never up)</li>
 * </ul>
 *
 * <p>The trailing stop level is initially set based on the entry price and only
 * begins trailing once the trade moves in the favorable direction.</p>
 */
public final class TrailingStopExit implements ExitRule {

    private final RotationalBacktestConfig.StopBasis basis;
    private final double multiplier;
    private double trailDist;
    private double extremePrice;  // highest high (long) or lowest low (short)
    private double stopLevel;

    public TrailingStopExit(RotationalBacktestConfig.StopBasis basis, double multiplier) {
        this.basis = basis;
        this.multiplier = multiplier;
    }

    @Override
    public void init(ExitContext ctx) {
        double basisValue = switch (basis) {
            case OR_RANGE -> ctx.orRange();
            case ATR -> Float.isNaN(ctx.dailyAtr()) ? ctx.orRange() : ctx.dailyAtr();
        };
        trailDist = multiplier * basisValue;

        if (ctx.side() == RotationalTrade.Side.LONG) {
            extremePrice = ctx.entryPrice();
            stopLevel = extremePrice - trailDist;
        } else {
            extremePrice = ctx.entryPrice();
            stopLevel = extremePrice + trailDist;
        }
    }

    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        if (ctx.side() == RotationalTrade.Side.LONG) {
            // Update extreme (highest high)
            double high = bars.getHigh(barIdx);
            if (high > extremePrice) {
                extremePrice = high;
                stopLevel = extremePrice - trailDist;
            }
            // Check stop
            if (bars.getLow(barIdx) <= stopLevel) {
                return new ExitSignal(stopLevel, ExitReason.TRAILING_STOP);
            }
        } else {
            // Update extreme (lowest low)
            double low = bars.getLow(barIdx);
            if (low < extremePrice) {
                extremePrice = low;
                stopLevel = extremePrice + trailDist;
            }
            // Check stop
            if (bars.getHigh(barIdx) >= stopLevel) {
                return new ExitSignal(stopLevel, ExitReason.TRAILING_STOP);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("TrailingStop(%.1fx %s)", multiplier, basis);
    }
}
