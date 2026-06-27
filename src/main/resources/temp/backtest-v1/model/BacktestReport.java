package com.whiteowl.core.backtest.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Locale;

@Getter
@Builder
public final class BacktestReport {

    private final int totalTrades;
    private final int winningTrades;
    private final int losingTrades;
    private final float winRate;
    private final float averageWin;
    private final float averageLoss;
    private final float largestWin;
    private final float largestLoss;
    private final float profitFactor;
    private final float payoffRatio;
    private final float expectancy;
    private final float initialCapital;
    private final float finalCapital;
    private final float netProfit;
    private final float netProfitPercent;
    private final float cagr;
    private final float maxDrawdown;
    private final float maxDrawdownDurationDays;
    private final float sharpeRatio;
    private final float sortinoRatio;
    private final float calmarRatio;
    private final int maxConsecutiveWins;
    private final int maxConsecutiveLosses;
    private final float averageTradeDurationDays;
    private final EquityCurve equityCurve;
    private final float[] drawdownCurve;
    private final List<TradeRecord> trades;

    private static final String REPORT_FORMAT = """
            
            ══════════════════════════════════════════
                      BACKTEST REPORT
            ══════════════════════════════════════════
            Total Trades          : %d
            Winning Trades        : %d
            Losing Trades         : %d
            Win Rate              : %.2f%%
            Max Consecutive Wins  : %d
            Max Consecutive Losses: %d
            ──────────────────────────────────────────
            Average Win           : %.2f%%
            Average Loss          : %.2f%%
            Largest Win           : %.2f%%
            Largest Loss          : %.2f%%
            Profit Factor         : %.2f
            Payoff Ratio          : %.2f
            Expectancy            : %.2f%%
            Avg Trade Duration    : %.1f days
            ──────────────────────────────────────────
            Initial Capital       : %.2f
            Final Capital         : %.2f
            Net Profit            : %.2f (%.2f%%)
            CAGR                  : %.2f%%
            ──────────────────────────────────────────
            Max Drawdown          : %.2f%%
            Max Drawdown Duration : %.0f days
            Sharpe Ratio          : %.2f
            Sortino Ratio         : %.2f
            Calmar Ratio          : %.2f
            ══════════════════════════════════════════
            """;

    @Override
    public String toString() {
        return String.format(Locale.US, REPORT_FORMAT,
                totalTrades, winningTrades, losingTrades, winRate,
                maxConsecutiveWins, maxConsecutiveLosses,
                averageWin, averageLoss, largestWin, largestLoss,
                profitFactor, payoffRatio, expectancy, averageTradeDurationDays,
                initialCapital, finalCapital, netProfit, netProfitPercent, cagr,
                maxDrawdown, maxDrawdownDurationDays,
                sharpeRatio, sortinoRatio, calmarRatio);
    }

}
