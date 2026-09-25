package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.metrics.MetricsCalculator;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.backtest.v2.model.TradeRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Numbers-only performance snapshot for one evaluated combination. Derived
 * from the existing {@link MetricsCalculator} / {@link BacktestReport} — the
 * trade list and equity curve are not retained, so thousands of these can be
 * held in memory.
 */
public record OptimizationMetrics(
        int totalTrades,
        int winningTrades,
        int losingTrades,
        float winRate,
        float averageWin,
        float averageLoss,
        float largestWin,
        float largestLoss,
        float profitFactor,
        float payoffRatio,
        float expectancy,
        float netProfit,
        float netProfitPercent,
        float cagr,
        float maxDrawdown,
        float maxDrawdownDurationDays,
        float sharpeRatio,
        float sortinoRatio,
        float calmarRatio,
        int maxConsecutiveWins,
        int maxConsecutiveLosses,
        float averageTradeDurationDays,
        float longestTradeDurationDays,
        float medianTradePnlPercent) {

    private static final float MS_PER_DAY = 86_400_000f;

    /** Compute metrics over one or more per-scrip results. */
    public static OptimizationMetrics of(List<ScripResult> results, float initialCapital) {
        List<TradeRecord> trades = new ArrayList<>();
        for (ScripResult r : results) {
            trades.addAll(r.getTrades());
        }
        trades.sort(Comparator.comparingLong(TradeRecord::getEntryTimestamp));

        BacktestReport report = MetricsCalculator.compute(results,
                BacktestConfig.builder().initialCapital(initialCapital).build());

        long longestMs = 0L;
        float[] pcts = new float[trades.size()];
        for (int i = 0; i < trades.size(); i++) {
            TradeRecord t = trades.get(i);
            longestMs = Math.max(longestMs, t.getExitTimestamp() - t.getEntryTimestamp());
            pcts[i] = t.getNetPnlPercent();
        }
        return new OptimizationMetrics(
                report.getTotalTrades(), report.getWinningTrades(), report.getLosingTrades(),
                report.getWinRate(), report.getAverageWin(), report.getAverageLoss(),
                report.getLargestWin(), report.getLargestLoss(),
                report.getProfitFactor(), report.getPayoffRatio(), report.getExpectancy(),
                report.getNetProfit(), report.getNetProfitPercent(),
                report.getCagr(), report.getMaxDrawdown(), report.getMaxDrawdownDurationDays(),
                report.getSharpeRatio(), report.getSortinoRatio(), report.getCalmarRatio(),
                report.getMaxConsecutiveWins(), report.getMaxConsecutiveLosses(),
                report.getAverageTradeDurationDays(),
                longestMs / MS_PER_DAY, median(pcts));
    }

    private static float median(float[] values) {
        if (values.length == 0) return 0f;
        float[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2f;
    }

}
