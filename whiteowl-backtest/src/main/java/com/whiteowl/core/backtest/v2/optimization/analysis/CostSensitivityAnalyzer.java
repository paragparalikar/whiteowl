package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 8 — transaction-cost sensitivity: the same configuration re-evaluated
 * under scaled slippage assumptions. A strategy that collapses at modestly
 * worse costs is flagged fragile.
 */
public final class CostSensitivityAnalyzer {

    public record Point(double slippageMultiplier, double slippagePercent,
                        OptimizationMetrics metrics) {
    }

    public record Result(List<Point> points, double baselineSortino,
                         double worstSortino, boolean fragile) {
    }

    public static Result analyze(StrategySpec strategy, ParameterCombination combination,
                                  List<ScripSlice> slices, StandardExitPolicy exits,
                                  ResearchConfiguration config, double[] multipliers) {
        List<Point> points = new ArrayList<>();
        for (double mult : multipliers) {
            ResearchConfiguration scaled = config.toBuilder()
                    .slippagePercent(config.getSlippagePercent() * (float) mult)
                    .costPercent(config.getCostPercent() * (float) mult)
                    .build();
            OptimizationEngine engine = new OptimizationEngine(scaled);
            OptimizationMetrics m = engine.evaluateCombination(
                    strategy, combination, slices, exits);
            points.add(new Point(mult,
                    config.getSlippagePercent() * mult, m));
        }
        double base = points.stream()
                .filter(p -> Math.abs(p.slippageMultiplier() - 1.0) < 1e-9)
                .mapToDouble(p -> p.metrics().sortinoRatio())
                .findFirst()
                .orElse(points.isEmpty() ? 0 : points.get(0).metrics().sortinoRatio());
        double worst = points.stream()
                .mapToDouble(p -> p.metrics().sortinoRatio()).min().orElse(0);
        // Fragile = worst-case run flips sign or loses >50% of baseline.
        boolean fragile = base > 0 && (worst < 0 || worst < base * 0.5);
        return new Result(points, base, worst, fragile);
    }

}
