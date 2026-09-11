package com.whiteowl.core.backtest.rotational;

import java.util.List;

/**
 * Performance metrics for the rotational backtest.
 */
public final class RotationalMetrics {

    private final double sharpe;
    private final double sortino;
    private final double cagr;
    private final double maxDrawdown;
    private final double calmar;
    private final double profitFactor;
    private final double winRate;
    private final int totalTrades;
    private final double longPnl;
    private final double shortPnl;
    private final double avgDailyTurnover;

    public RotationalMetrics(List<RotationalTrade> trades, PortfolioTracker portfolio,
                             double initialCapital) {
        this.totalTrades = trades.size();

        // Win/loss
        int wins = 0;
        double grossProfit = 0, grossLoss = 0;
        double longSum = 0, shortSum = 0;

        for (RotationalTrade t : trades) {
            if (t.netPnl() > 0) {
                wins++;
                grossProfit += t.netPnl();
            } else {
                grossLoss += Math.abs(t.netPnl());
            }
            if (t.side() == RotationalTrade.Side.LONG) longSum += t.netPnl();
            else shortSum += t.netPnl();
        }

        this.winRate = (totalTrades > 0) ? (double) wins / totalTrades : 0;
        this.profitFactor = (grossLoss > 0) ? grossProfit / grossLoss : 0;
        this.longPnl = longSum;
        this.shortPnl = shortSum;

        // Daily returns
        double[] dailyReturns = portfolio.dailyReturns();
        int tradingDays = dailyReturns.length;

        // Sharpe (annualized, 252 trading days)
        if (tradingDays > 1) {
            double mean = mean(dailyReturns);
            double std = std(dailyReturns, mean);
            this.sharpe = (std > 0) ? mean / std * Math.sqrt(252) : 0;

            // Sortino (downside deviation)
            double downsideDev = downsideDeviation(dailyReturns, 0);
            this.sortino = (downsideDev > 0) ? mean / downsideDev * Math.sqrt(252) : 0;
        } else {
            this.sharpe = 0;
            this.sortino = 0;
        }

        // CAGR
        double finalEquity = portfolio.getEquity();
        double years = tradingDays / 252.0;
        this.cagr = (years > 0 && initialCapital > 0)
                ? Math.pow(finalEquity / initialCapital, 1.0 / years) - 1.0 : 0;

        this.maxDrawdown = portfolio.getMaxDrawdown();
        this.calmar = (maxDrawdown < 0) ? cagr / Math.abs(maxDrawdown) : 0;

        // Turnover: average daily traded notional / equity
        if (tradingDays > 0 && !portfolio.getEquityCurve().isEmpty()) {
            double totalNotional = trades.stream()
                    .mapToDouble(t -> t.entryPrice() * t.shares()).sum();
            double avgEquity = portfolio.getEquityCurve().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(initialCapital);
            this.avgDailyTurnover = (avgEquity > 0) ? totalNotional / tradingDays / avgEquity : 0;
        } else {
            this.avgDailyTurnover = 0;
        }
    }

    // Getters
    public double getSharpe() { return sharpe; }
    public double getSortino() { return sortino; }
    public double getCagr() { return cagr; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public double getCalmar() { return calmar; }
    public double getProfitFactor() { return profitFactor; }
    public double getWinRate() { return winRate; }
    public int getTotalTrades() { return totalTrades; }
    public double getLongPnl() { return longPnl; }
    public double getShortPnl() { return shortPnl; }
    public double getAvgDailyTurnover() { return avgDailyTurnover; }

    @Override
    public String toString() {
        return String.format("""
                Sharpe: %.3f, Sortino: %.3f, Calmar: %.3f
                CAGR: %.2f%%, Max DD: %.2f%%
                Win rate: %.1f%%, Profit factor: %.2f
                Trades: %d (Long PnL: %.0f, Short PnL: %.0f)
                Daily turnover: %.2f%%""",
                sharpe, sortino, calmar,
                cagr * 100, maxDrawdown * 100,
                winRate * 100, profitFactor,
                totalTrades, longPnl, shortPnl,
                avgDailyTurnover * 100);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double std(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }

    private static double downsideDeviation(double[] returns, double threshold) {
        double sumSq = 0;
        int count = 0;
        for (double r : returns) {
            if (r < threshold) {
                sumSq += (r - threshold) * (r - threshold);
                count++;
            }
        }
        return (count > 0) ? Math.sqrt(sumSq / count) : 0;
    }
}
