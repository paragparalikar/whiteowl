package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7 — conditional next-trade probabilities after win/loss streaks, plus
 * the WIN/LOSS transition matrix. Observed dependencies are descriptive —
 * no causality is implied.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StreakAnalyzer {

    /** Conditional stats for "next trade after a streak of k outcomes". */
    public record StreakBucket(int streakLength, int samples,
                               double probNextWin, double avgNextR,
                               double medianNextR) {
    }

    public record TransitionMatrix(double pWinGivenWin, double pLossGivenWin,
                                   double pWinGivenLoss, double pLossGivenLoss,
                                   double unconditionalWin, int samples) {
    }

    public record Result(List<StreakBucket> afterWinStreaks,
                         List<StreakBucket> afterLossStreaks,
                         TransitionMatrix transitions) {
    }

    public static Result analyze(List<TradeObservation> tradesChronological, int maxStreak) {
        List<StreakBucket> winBuckets = new ArrayList<>();
        List<StreakBucket> lossBuckets = new ArrayList<>();
        for (int k = 1; k <= maxStreak; k++) {
            winBuckets.add(nextTradeStats(tradesChronological, k, true));
            lossBuckets.add(nextTradeStats(tradesChronological, k, false));
        }
        return new Result(winBuckets, lossBuckets, transitions(tradesChronological));
    }

    /** Stats of the trade that *follows* a streak of exactly {@code k} wins/losses. */
    private static StreakBucket nextTradeStats(List<TradeObservation> trades,
                                                int k, boolean wins) {
        List<Double> nextRs = new ArrayList<>();
        int nextWins = 0;
        int n = trades.size();
        for (int i = 0; i + 1 < n; i++) {
            int streak = 0;
            int j = i;
            while (j >= 0 && trades.get(j).win() == wins) {
                streak++;
                j--;
            }
            if (streak == k) {
                TradeObservation next = trades.get(i + 1);
                if (next.win()) nextWins++;
                if (!Float.isNaN(next.rMultiple())) nextRs.add((double) next.rMultiple());
            }
        }
        int samples = nextRs.size();
        if (samples == 0) {
            return new StreakBucket(k, 0, Double.NaN, Double.NaN, Double.NaN);
        }
        double[] sorted = nextRs.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double mean = nextRs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = samples % 2 == 1 ? sorted[samples / 2]
                : (sorted[samples / 2 - 1] + sorted[samples / 2]) / 2;
        // count trades where streak==k even if R was NaN
        int total = 0;
        for (int i = 0; i + 1 < n; i++) {
            int streak = 0, j = i;
            while (j >= 0 && trades.get(j).win() == wins) { streak++; j--; }
            if (streak == k) total++;
        }
        return new StreakBucket(k, total, (double) nextWins / total * 100, mean, median);
    }

    private static TransitionMatrix transitions(List<TradeObservation> trades) {
        int ww = 0, wl = 0, lw = 0, ll = 0, wins = 0;
        for (int i = 0; i + 1 < trades.size(); i++) {
            boolean cur = trades.get(i).win();
            boolean nxt = trades.get(i + 1).win();
            if (cur && nxt) ww++;
            else if (cur) wl++;
            else if (nxt) lw++;
            else ll++;
        }
        for (TradeObservation t : trades) {
            if (t.win()) wins++;
        }
        int wTot = ww + wl, lTot = lw + ll;
        return new TransitionMatrix(
                wTot > 0 ? (double) ww / wTot : Double.NaN,
                wTot > 0 ? (double) wl / wTot : Double.NaN,
                lTot > 0 ? (double) lw / lTot : Double.NaN,
                lTot > 0 ? (double) ll / lTot : Double.NaN,
                trades.isEmpty() ? 0 : (double) wins / trades.size(),
                trades.size());
    }

}
