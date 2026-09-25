package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.model.TradeRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Phase 8 — Monte Carlo analysis on the trade sequence: random-order
 * reshuffling plus bootstrap resampling, measuring whether the historical
 * trade ordering was unusually favorable.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MonteCarloAnalyzer {

    public record Result(int simulations,
                         double historicalMaxDrawdown,
                         double medianMaxDrawdown,
                         double p95MaxDrawdown,
                         double p99MaxDrawdown,
                         double medianNetProfit,
                         double p5NetProfit,
                         double probabilityOfLoss,
                         long seed) {
    }

    /**
     * @param tradePnls    net P&L per trade (chronological)
     * @param initialCapital starting equity for each simulated path
     * @param simulations  number of bootstrap/reshuffle runs
     * @param seed         fixed seed for determinism
     */
    public static Result analyze(float[] tradePnls, float initialCapital,
                                  int simulations, long seed) {
        if (tradePnls.length == 0) {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, seed);
        }
        double histMaxDd = maxDrawdownPct(tradePnls, initialCapital);
        double[] dds = new double[simulations];
        double[] nets = new double[simulations];
        Random rnd = new Random(seed);
        for (int s = 0; s < simulations; s++) {
            // Bootstrap: sample trades with replacement.
            float[] sim = new float[tradePnls.length];
            for (int i = 0; i < sim.length; i++) {
                sim[i] = tradePnls[rnd.nextInt(tradePnls.length)];
            }
            dds[s] = maxDrawdownPct(sim, initialCapital);
            double sum = 0;
            for (float p : sim) sum += p;
            nets[s] = sum;
        }
        Arrays.sort(dds);
        Arrays.sort(nets);
        int losses = 0;
        for (double v : nets) if (v < 0) losses++;
        return new Result(simulations, histMaxDd,
                q(dds, 0.5), q(dds, 0.95), q(dds, 0.99),
                q(nets, 0.5), q(nets, 0.05),
                (double) losses / simulations * 100.0, seed);
    }

    public static float[] pnlOf(List<TradeRecord> trades) {
        float[] out = new float[trades.size()];
        for (int i = 0; i < trades.size(); i++) {
            out[i] = trades.get(i).getNetPnl();
        }
        return out;
    }

    private static double maxDrawdownPct(float[] pnls, double initial) {
        double eq = initial, peak = initial, maxDd = 0;
        for (float p : pnls) {
            eq = Math.max(0, eq + p);   // account cannot go below zero
            peak = Math.max(peak, eq);
            maxDd = Math.max(maxDd, (peak - eq) / peak * 100.0);
            if (eq <= 0) {
                break;                 // account dead — path ends at ruin
            }
        }
        return maxDd;
    }

    private static double q(double[] s, double p) {
        if (s.length == 0) return 0;
        double idx = p * (s.length - 1);
        int lo = (int) idx;
        int hi = Math.min(lo + 1, s.length - 1);
        return s[lo] + (s[hi] - s[lo]) * (idx - lo);
    }

}
