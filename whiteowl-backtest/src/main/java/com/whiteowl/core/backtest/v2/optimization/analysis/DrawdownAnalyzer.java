package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.model.EquityCurve;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Phase 7 — drawdown statistics over an equity curve.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DrawdownAnalyzer {

    public record Result(double maxDrawdown, double avgDrawdown, double medianDrawdown,
                         double p95Drawdown, double p99Drawdown,
                         double maxDrawdownDurationDays, double avgDrawdownDurationDays,
                         int drawdownEpisodes) {
    }

    public static Result analyze(EquityCurve curve) {
        if (curve == null || curve.getSize() < 2) {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0);
        }
        float[] v = curve.getValues();
        long[] ts = curve.getTimestamps();
        int n = curve.getSize();
        List<Double> depths = new ArrayList<>();
        double peak = v[0];
        double curMax = 0;
        List<Long> durationsMs = new ArrayList<>();
        long curDurStart = -1;
        boolean inDd = false;
        for (int i = 1; i < n; i++) {
            if (v[i] >= peak) {
                if (inDd) {
                    depths.add(curMax);
                    durationsMs.add(ts[i] - curDurStart);
                    inDd = false;
                }
                peak = v[i];
            } else {
                if (!inDd) {
                    curDurStart = ts[i];
                    curMax = 0;
                    inDd = true;
                }
                curMax = Math.max(curMax, (peak - v[i]) / peak * 100.0);
            }
        }
        if (inDd) {
            depths.add(curMax);
            durationsMs.add(ts[n - 1] - curDurStart);
        }
        double[] d = depths.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double max = d.length == 0 ? 0 : d[d.length - 1];
        double avg = d.length == 0 ? 0 : Arrays.stream(d).average().orElse(0);
        double median = d.length == 0 ? 0 : q(d, 0.5);
        long durSum = durationsMs.stream().mapToLong(Long::longValue).sum();
        double msPerDay = 86_400_000.0;
        return new Result(max, avg, median, q(d, 0.95), q(d, 0.99),
                durationsMs.stream().mapToLong(Long::longValue).max().orElse(0) / msPerDay,
                d.length == 0 ? 0 : durSum / msPerDay / d.length,
                d.length);
    }

    private static double q(double[] s, double p) {
        if (s.length == 0) return 0;
        double idx = p * (s.length - 1);
        int lo = (int) idx;
        int hi = Math.min(lo + 1, s.length - 1);
        return s[lo] + (s[hi] - s[lo]) * (idx - lo);
    }

}
