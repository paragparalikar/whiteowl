package com.whiteowl.core.backtest.v2.optimization.plateau;

import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 2 — plateau detection on a one-dimensional parameter curve.
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Sort points by parameter value.</li>
 *   <li>Compute the global best over eligible points (non-NaN metric and
 *       {@code tradeCount >= minTradesPerCombination}).</li>
 *   <li>Smooth the metric curve with a leave-one-out neighborhood average —
 *       each point becomes the mean of its {@code smoothingWindow} neighbors
 *       on each side, excluding itself (truncated at the edges). Isolated
 *       spikes cannot smooth themselves into a plateau.</li>
 *   <li>Plateau membership: eligible points whose smoothed metric is within
 *       {@code plateauThreshold} of the best —
 *       {@code smoothed >= best − (1 − threshold)·|best|}.</li>
 *   <li>A qualifying region is a contiguous run of members with at least
 *       {@code minPlateauPoints} points and raw-metric spread
 *       {@code max − min <= maxPlateauSpreadFraction·|max|}.</li>
 *   <li>Selection: argmax of the smoothed curve over eligible points — the
 *       parameter value whose neighborhood is collectively strongest.</li>
 *   <li>Plateau regions are still detected and scored
 *       ({@link RobustnessScore}) — as the diagnostic of whether the
 *       landscape actually contains a stable zone. {@code stable} is true
 *       only when the smoothed-argmax selection lies inside a qualifying
 *       plateau; otherwise the selection is flagged low-confidence.</li>
 * </ol>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlateauDetector {

    private static final double EPS = 1e-9;

    public static PlateauResult detect(String parameterName, List<CurvePoint> points,
                                        ResearchConfiguration cfg) {
        List<CurvePoint> curve = points.stream()
                .sorted(Comparator.comparingDouble(CurvePoint::parameterValue))
                .toList();
        if (curve.isEmpty()) {
            return new PlateauResult(parameterName, List.of(), null, Double.NaN,
                    Double.NaN, Double.NaN, false, "no data points");
        }

        // Global best over eligible points only (kept for diagnostics).
        int bestIdx = -1;
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < curve.size(); i++) {
            CurvePoint p = curve.get(i);
            if (eligible(p, cfg) && p.metric() > best) {
                best = p.metric();
                bestIdx = i;
            }
        }
        if (bestIdx < 0) {
            return new PlateauResult(parameterName, curve, null, Double.NaN,
                    Double.NaN, Double.NaN, false,
                    "no point meets the minimum trade count of "
                            + cfg.getMinTradesPerCombination());
        }
        double bestValue = curve.get(bestIdx).parameterValue();
        double cutoff = best - (1.0 - cfg.getPlateauThreshold()) * Math.max(EPS, Math.abs(best));

        double[] smoothed = smooth(curve, cfg.getSmoothingWindow());

        // Selection: argmax of the leave-one-out smoothed curve — the value
        // whose neighborhood is collectively strongest. The point cannot
        // vote for itself, so isolated spikes cannot win.
        int selIdx = -1;
        double selScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < curve.size(); i++) {
            if (eligible(curve.get(i), cfg) && smoothed[i] > selScore) {
                selScore = smoothed[i];
                selIdx = i;
            }
        }
        double selected = curve.get(selIdx).parameterValue();

        // Maximal contiguous runs of members.
        List<PlateauRegion> regions = new ArrayList<>();
        int i = 0;
        while (i < curve.size()) {
            if (eligible(curve.get(i), cfg) && smoothed[i] >= cutoff) {
                int start = i;
                while (i < curve.size() && eligible(curve.get(i), cfg) && smoothed[i] >= cutoff) {
                    i++;
                }
                List<CurvePoint> members = curve.subList(start, i);
                PlateauRegion region = toRegion(members, best, curve.size(), cfg);
                if (region != null) {
                    regions.add(region);
                }
            } else {
                i++;
            }
        }

        if (regions.isEmpty()) {
            return new PlateauResult(parameterName, curve, null, selected,
                    bestValue, best, false,
                    String.format("no stable plateau found (isolated optimum %.4g at %s); "
                                    + "selected %s = argmax of neighborhood-smoothed curve "
                                    + "(%.4g) — low confidence",
                            best, fmt(bestValue), fmt(selected), selScore));
        }

        PlateauRegion region = regions.stream()
                .max(Comparator.comparingDouble(PlateauRegion::robustnessScore))
                .orElseThrow();
        boolean inRegion = region.points().stream()
                .anyMatch(p -> p.parameterValue() == selected);
        String reason = String.format(
                "stable plateau [%s, %s] (n=%d, mean=%.4g, sd=%.4g, score=%.3f); "
                        + "selected %s = argmax of neighborhood-smoothed curve (%.4g)%s",
                fmt(region.lowerBound()), fmt(region.upperBound()), region.size(),
                region.meanMetric(), region.stdDevMetric(), region.robustnessScore(),
                fmt(selected), selScore,
                inRegion ? " — inside plateau" : " — outside plateau, verify landscape");
        return new PlateauResult(parameterName, curve, region, selected,
                bestValue, best, inRegion, reason);
    }

    /** Build a region if it satisfies minimum-size and maximum-spread rules. */
    private static PlateauRegion toRegion(List<CurvePoint> members, double globalBest,
                                           int totalPoints, ResearchConfiguration cfg) {
        if (members.size() < cfg.getMinPlateauPoints()) {
            return null;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        int minTrades = Integer.MAX_VALUE;
        for (CurvePoint p : members) {
            min = Math.min(min, p.metric());
            max = Math.max(max, p.metric());
            sum += p.metric();
            minTrades = Math.min(minTrades, p.tradeCount());
        }
        double spreadCap = cfg.getMaxPlateauSpreadFraction() * Math.max(EPS, Math.abs(max));
        if (max - min > spreadCap) {
            return null;
        }
        double mean = sum / members.size();
        double sumSq = 0;
        for (CurvePoint p : members) {
            double d = p.metric() - mean;
            sumSq += d * d;
        }
        double std = members.size() > 1 ? Math.sqrt(sumSq / (members.size() - 1)) : 0;
        PlateauRegion region = new PlateauRegion(
                members.get(0).parameterValue(),
                members.get(members.size() - 1).parameterValue(),
                List.copyOf(members), mean, std, minTrades, 0);
        double score = RobustnessScore.compute(region, globalBest, totalPoints, cfg);
        return new PlateauRegion(region.lowerBound(), region.upperBound(), region.points(),
                mean, std, minTrades, score);
    }

    private static boolean eligible(CurvePoint p, ResearchConfiguration cfg) {
        return !Double.isNaN(p.metric())
                && p.tradeCount() >= cfg.getMinTradesPerCombination();
    }

    /**
     * Leave-one-out neighborhood smoothing: the transformed value at each
     * position is the average of up to {@code radius} neighbors on each side,
     * excluding the point's own value. At the edges only the neighbors that
     * exist are averaged. This deliberately dampens isolated spikes — a
     * single lucky parameter value cannot lift itself into a plateau.
     */
    private static double[] smooth(List<CurvePoint> curve, int radius) {
        int n = curve.size();
        double[] out = new double[n];
        if (radius <= 0) {
            for (int i = 0; i < n; i++) out[i] = curve.get(i).metric();
            return out;
        }
        for (int i = 0; i < n; i++) {
            double sum = 0;
            int cnt = 0;
            for (int j = Math.max(0, i - radius); j <= Math.min(n - 1, i + radius); j++) {
                if (j == i) continue;
                double m = curve.get(j).metric();
                if (!Double.isNaN(m)) {
                    sum += m;
                    cnt++;
                }
            }
            out[i] = cnt == 0 ? curve.get(i).metric() : sum / cnt;
        }
        return out;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }

}
