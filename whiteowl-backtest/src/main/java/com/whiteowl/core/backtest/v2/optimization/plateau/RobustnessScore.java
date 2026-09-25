package com.whiteowl.core.backtest.v2.optimization.plateau;

import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Composite robustness score for a plateau region. Sortino remains the primary
 * performance metric — this score only distinguishes regions of similar
 * performance. The formula is deterministic and fully documented:
 *
 * <pre>
 *   performance = clamp01(regionMean / |globalBest|)
 *   flatness    = 1 − clamp01(regionStdDev / |regionMean|)
 *   width       = regionSize / totalCurvePoints
 *   adequacy    = clamp01(regionMinTrades / minTradesPerCombination)
 *
 *   score = wPerf·performance + wStab·flatness + wWidth·width + wTrade·adequacy
 * </pre>
 *
 * Weights live in {@link ResearchConfiguration}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RobustnessScore {

    private static final double EPS = 1e-9;

    public static double compute(PlateauRegion region, double globalBestMetric,
                                  int totalCurvePoints, ResearchConfiguration cfg) {
        double performance = clamp01(region.meanMetric() / Math.max(EPS, Math.abs(globalBestMetric)));
        double flatness = 1.0 - clamp01(region.stdDevMetric() / Math.max(EPS, Math.abs(region.meanMetric())));
        double width = totalCurvePoints <= 0 ? 0.0
                : region.size() / (double) totalCurvePoints;
        double adequacy = clamp01(region.minTradeCount()
                / (double) Math.max(1, cfg.getMinTradesPerCombination()));
        return cfg.getWeightPerformance() * performance
                + cfg.getWeightStability() * flatness
                + cfg.getWeightWidth() * width
                + cfg.getWeightTradeCount() * adequacy;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1.0);
    }

}
