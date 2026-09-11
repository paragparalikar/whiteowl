package com.whiteowl.core.backtest.rotational;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks portfolio state through the backtest: equity curve, drawdown, daily P&L.
 */
@Getter
public final class PortfolioTracker {

    private double equity;
    private double peakEquity;
    private double maxDrawdown;

    private final List<Long> dates = new ArrayList<>();
    private final List<Double> equityCurve = new ArrayList<>();
    private final List<Double> dailyPnl = new ArrayList<>();
    private final List<Double> drawdownCurve = new ArrayList<>();

    public PortfolioTracker(double initialCapital) {
        this.equity = initialCapital;
        this.peakEquity = initialCapital;
    }

    /**
     * Record one day's trades and update portfolio state.
     *
     * @param date   epoch millis
     * @param trades all trades executed on this day
     */
    public void recordDay(long date, List<RotationalTrade> trades) {
        double dayPnl = 0;
        for (RotationalTrade t : trades) {
            dayPnl += t.netPnl();
        }

        equity += dayPnl;
        peakEquity = Math.max(peakEquity, equity);
        double drawdown = (peakEquity > 0) ? (equity - peakEquity) / peakEquity : 0;
        maxDrawdown = Math.min(maxDrawdown, drawdown);

        dates.add(date);
        equityCurve.add(equity);
        dailyPnl.add(dayPnl);
        drawdownCurve.add(drawdown);
    }

    /**
     * Compute daily returns from the equity curve.
     */
    public double[] dailyReturns() {
        if (equityCurve.size() < 2) return new double[0];
        double[] returns = new double[equityCurve.size() - 1];
        for (int i = 1; i < equityCurve.size(); i++) {
            double prev = equityCurve.get(i - 1);
            returns[i - 1] = (prev > 0) ? (equityCurve.get(i) - prev) / prev : 0;
        }
        return returns;
    }
}
