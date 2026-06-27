package com.whiteowl.core.backtest.v2.metrics;

import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.EquityCurve;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricsCalculator {

    private static final float HUNDRED = 100f;
    private static final float TRADING_DAYS_PER_YEAR = 252f;
    private static final float MS_PER_DAY = 86_400_000f;
    private static final float DAYS_PER_YEAR = 365f;

    public static BacktestReport compute(List<ScripResult> results, BacktestConfig config) {
        List<TradeRecord> allTrades = mergeAndSortTrades(results);
        EquityCurve mergedCurve = mergeEquityCurves(results, config.getInitialCapital());
        float[] drawdownCurve = computeDrawdownCurve(mergedCurve);
        return buildReport(allTrades, mergedCurve, drawdownCurve, config);
    }

    private static List<TradeRecord> mergeAndSortTrades(List<ScripResult> results) {
        List<TradeRecord> all = new ArrayList<>();
        for (ScripResult result : results) {
            all.addAll(result.getTrades());
        }
        all.sort(Comparator.comparingLong(TradeRecord::getEntryTimestamp));
        return all;
    }

    private static EquityCurve mergeEquityCurves(List<ScripResult> results, float initialCapital) {
        List<ScripResult> withCurves = results.stream()
                .filter(r -> r.getEquityCurve() != null && r.getEquityCurve().getSize() > 0)
                .toList();
        if (withCurves.isEmpty()) {
            return new EquityCurve(new float[]{initialCapital}, new long[]{0L}, 1);
        }
        if (withCurves.size() == 1) {
            return withCurves.get(0).getEquityCurve();
        }
        EquityCurve longest = withCurves.get(0).getEquityCurve();
        for (ScripResult r : withCurves) {
            if (r.getEquityCurve().getSize() > longest.getSize()) {
                longest = r.getEquityCurve();
            }
        }
        int size = longest.getSize();
        long[] timestamps = longest.getTimestamps();
        float[] merged = new float[size];
        for (int i = 0; i < size; i++) {
            merged[i] = initialCapital;
        }
        for (ScripResult r : withCurves) {
            EquityCurve curve = r.getEquityCurve();
            float curveInitial = curve.getValues()[0];
            int curveSize = curve.getSize();
            for (int i = 0; i < curveSize; i++) {
                merged[i] += curve.getValues()[i] - curveInitial;
            }
            float lastDelta = curve.getValues()[curveSize - 1] - curveInitial;
            for (int i = curveSize; i < size; i++) {
                merged[i] += lastDelta;
            }
        }
        return new EquityCurve(merged, timestamps, size);
    }

    private static float[] computeDrawdownCurve(EquityCurve curve) {
        int size = curve.getSize();
        float[] dd = new float[size];
        float peak = curve.getValues()[0];
        for (int i = 0; i < size; i++) {
            float val = curve.getValues()[i];
            peak = Math.max(peak, val);
            dd[i] = peak > 0 ? (val - peak) / peak * HUNDRED : 0f;
        }
        return dd;
    }

    private static BacktestReport buildReport(List<TradeRecord> trades, EquityCurve curve,
                                              float[] drawdownCurve, BacktestConfig config) {
        float initialCapital = config.getInitialCapital();
        int total = trades.size();
        if (total == 0) {
            return emptyReport(initialCapital, curve, drawdownCurve, trades);
        }
        int winners = 0;
        int losers = 0;
        float sumWinPct = 0f;
        float sumLossPct = 0f;
        float largestWinPct = 0f;
        float largestLossPct = 0f;
        long totalDurationMs = 0L;
        for (TradeRecord t : trades) {
            float pct = t.getNetPnlPercent();
            if (pct > 0) {
                winners++;
                sumWinPct += pct;
                largestWinPct = Math.max(largestWinPct, pct);
            } else if (pct < 0) {
                losers++;
                sumLossPct += pct;
                largestLossPct = Math.min(largestLossPct, pct);
            }
            totalDurationMs += t.getExitTimestamp() - t.getEntryTimestamp();
        }
        float winRate = (float) winners / total * HUNDRED;
        float avgWinPct = winners > 0 ? sumWinPct / winners : 0f;
        float avgLossPct = losers > 0 ? sumLossPct / losers : 0f;
        float profitFactor = sumLossPct != 0 ? sumWinPct / Math.abs(sumLossPct) : Float.MAX_VALUE;
        float payoffRatio = avgLossPct != 0 ? avgWinPct / Math.abs(avgLossPct) : Float.MAX_VALUE;
        float expectancy = (winRate / HUNDRED * avgWinPct) + ((1f - winRate / HUNDRED) * avgLossPct);
        float finalCapital = curve.getValues()[curve.getSize() - 1];
        float netProfit = finalCapital - initialCapital;
        float netProfitPercent = netProfit / initialCapital * HUNDRED;
        float totalDays = computeTotalDays(curve);
        float cagr = computeCagr(initialCapital, finalCapital, totalDays);
        float maxDd = computeMaxDrawdown(drawdownCurve);
        float maxDdDuration = computeMaxDrawdownDuration(curve);
        float[] dailyReturns = computeDailyReturns(curve);
        float sharpe = computeSharpeRatio(dailyReturns);
        float sortino = computeSortinoRatio(dailyReturns);
        float calmar = maxDd < 0 ? cagr / Math.abs(maxDd) : 0f;
        int maxConsWins = computeMaxConsecutive(trades, true);
        int maxConsLosses = computeMaxConsecutive(trades, false);
        float avgDurationDays = totalDurationMs / (float) total / MS_PER_DAY;
        return BacktestReport.builder()
                .totalTrades(total)
                .winningTrades(winners)
                .losingTrades(losers)
                .winRate(winRate)
                .averageWin(avgWinPct)
                .averageLoss(avgLossPct)
                .largestWin(largestWinPct)
                .largestLoss(largestLossPct)
                .profitFactor(profitFactor)
                .payoffRatio(payoffRatio)
                .expectancy(expectancy)
                .initialCapital(initialCapital)
                .finalCapital(finalCapital)
                .netProfit(netProfit)
                .netProfitPercent(netProfitPercent)
                .cagr(cagr)
                .maxDrawdown(maxDd)
                .maxDrawdownDurationDays(maxDdDuration)
                .sharpeRatio(sharpe)
                .sortinoRatio(sortino)
                .calmarRatio(calmar)
                .maxConsecutiveWins(maxConsWins)
                .maxConsecutiveLosses(maxConsLosses)
                .averageTradeDurationDays(avgDurationDays)
                .equityCurve(curve)
                .drawdownCurve(drawdownCurve)
                .trades(trades)
                .build();
    }

    private static BacktestReport emptyReport(float initialCapital, EquityCurve curve,
                                              float[] drawdownCurve, List<TradeRecord> trades) {
        return BacktestReport.builder()
                .totalTrades(0).winningTrades(0).losingTrades(0).winRate(0f)
                .averageWin(0f).averageLoss(0f).largestWin(0f).largestLoss(0f)
                .profitFactor(0f).payoffRatio(0f).expectancy(0f)
                .initialCapital(initialCapital).finalCapital(initialCapital)
                .netProfit(0f).netProfitPercent(0f).cagr(0f)
                .maxDrawdown(0f).maxDrawdownDurationDays(0f)
                .sharpeRatio(0f).sortinoRatio(0f).calmarRatio(0f)
                .maxConsecutiveWins(0).maxConsecutiveLosses(0)
                .averageTradeDurationDays(0f)
                .equityCurve(curve).drawdownCurve(drawdownCurve).trades(trades)
                .build();
    }

    private static float computeTotalDays(EquityCurve curve) {
        if (curve.getSize() < 2) return 1f;
        long firstTs = curve.getTimestamps()[0];
        long lastTs = curve.getTimestamps()[curve.getSize() - 1];
        float days = (lastTs - firstTs) / MS_PER_DAY;
        return Math.max(days, 1f);
    }

    private static float computeCagr(float initial, float finalCap, float totalDays) {
        if (initial <= 0 || finalCap <= 0) return 0f;
        float years = totalDays / DAYS_PER_YEAR;
        if (years <= 0) return 0f;
        return (float) (Math.pow(finalCap / initial, 1.0 / years) - 1.0) * HUNDRED;
    }

    private static float computeMaxDrawdown(float[] drawdownCurve) {
        float min = 0f;
        for (float dd : drawdownCurve) {
            min = Math.min(min, dd);
        }
        return min;
    }

    private static float computeMaxDrawdownDuration(EquityCurve curve) {
        int size = curve.getSize();
        float[] values = curve.getValues();
        long[] timestamps = curve.getTimestamps();
        float peak = values[0];
        long ddStartTs = -1;
        boolean inDrawdown = false;
        float maxDurationDays = 0f;
        for (int i = 1; i < size; i++) {
            if (values[i] >= peak) {
                if (inDrawdown) {
                    float duration = (timestamps[i] - ddStartTs) / MS_PER_DAY;
                    maxDurationDays = Math.max(maxDurationDays, duration);
                    inDrawdown = false;
                }
                peak = values[i];
            } else if (!inDrawdown) {
                ddStartTs = timestamps[i];
                inDrawdown = true;
            }
        }
        if (inDrawdown) {
            float finalDuration = (timestamps[size - 1] - ddStartTs) / MS_PER_DAY;
            maxDurationDays = Math.max(maxDurationDays, finalDuration);
        }
        return maxDurationDays;
    }

    private static float[] computeDailyReturns(EquityCurve curve) {
        int size = curve.getSize();
        if (size < 2) return new float[0];
        float[] returns = new float[size - 1];
        float[] values = curve.getValues();
        for (int i = 1; i < size; i++) {
            returns[i - 1] = values[i - 1] > 0 ? (values[i] - values[i - 1]) / values[i - 1] : 0f;
        }
        return returns;
    }

    private static float computeSharpeRatio(float[] returns) {
        if (returns.length < 2) return 0f;
        float mean = computeMean(returns);
        float std = computeStdDev(returns, mean);
        if (std == 0) return 0f;
        return mean / std * (float) Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    private static float computeSortinoRatio(float[] returns) {
        if (returns.length < 2) return 0f;
        float mean = computeMean(returns);
        float downsideStd = computeDownsideStdDev(returns);
        if (downsideStd == 0) return 0f;
        return mean / downsideStd * (float) Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    private static float computeMean(float[] values) {
        float sum = 0f;
        for (float v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static float computeStdDev(float[] values, float mean) {
        float sumSq = 0f;
        for (float v : values) {
            float diff = v - mean;
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq / (values.length - 1));
    }

    private static float computeDownsideStdDev(float[] values) {
        float sumSq = 0f;
        int count = 0;
        for (float v : values) {
            if (v < 0) {
                sumSq += v * v;
                count++;
            }
        }
        if (count < 2) return 0f;
        return (float) Math.sqrt(sumSq / (count - 1));
    }

    private static int computeMaxConsecutive(List<TradeRecord> trades, boolean wins) {
        int max = 0;
        int current = 0;
        for (TradeRecord t : trades) {
            boolean match = wins ? t.getNetPnl() > 0 : t.getNetPnl() < 0;
            if (match) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

}
