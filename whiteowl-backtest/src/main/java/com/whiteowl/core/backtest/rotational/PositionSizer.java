package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.IndicatorFunctions;

/**
 * Computes position sizes within each side (long/short).
 *
 * <p>Sizing logic:</p>
 * <ul>
 *   <li>Base: equal weight within each side ({@code sideCapital / numPicks})</li>
 *   <li>Optional ATR(14) volatility scaling: {@code position_size = base / ATR(14)}
 *       so more volatile stocks get smaller positions</li>
 *   <li>Normalize so total per-side exposure matches the allocation</li>
 * </ul>
 */
public final class PositionSizer {

    private final boolean atrScaling;

    public PositionSizer(boolean atrScaling) {
        this.atrScaling = atrScaling;
    }

    /**
     * Compute share count for a position.
     *
     * @param sideCapital   total capital allocated to this side (long or short)
     * @param numPicks      number of picks on this side
     * @param bars          the scrip's bars
     * @param barIndex      current bar index
     * @param entryPrice    expected entry price (open)
     * @return number of shares to trade
     */
    public double computeShares(double sideCapital, int numPicks,
                                Bars bars, int barIndex, double entryPrice) {
        if (numPicks <= 0 || entryPrice <= 0 || sideCapital <= 0) return 0;

        double perStockCapital = sideCapital / numPicks;

        if (!atrScaling || barIndex < 14) {
            // Simple equal weight
            return perStockCapital / entryPrice;
        }

        // ATR volatility scaling
        float[] atr = IndicatorFunctions.atr(
                bars.arrays().high(), bars.arrays().low(), bars.arrays().close(),
                barIndex + 1, 14);

        float atrValue = atr[barIndex];
        if (Float.isNaN(atrValue) || atrValue <= 0) {
            return perStockCapital / entryPrice;
        }

        // Inverse volatility: allocate more to low-vol stocks
        // Target risk per position: perStockCapital * 2% (notional risk budget)
        double riskBudget = perStockCapital * 0.02;
        double shares = riskBudget / atrValue;

        // Cap at the equal-weight amount to avoid over-concentration
        double maxShares = perStockCapital / entryPrice;
        return Math.min(shares, maxShares);
    }
}
