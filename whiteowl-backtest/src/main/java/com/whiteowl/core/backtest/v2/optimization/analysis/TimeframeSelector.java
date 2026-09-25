package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.OptimizationMetrics;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;
import com.whiteowl.core.backtest.v2.optimization.plateau.ParameterSelection;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.bar.model.Timeframe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 6 — timeframe selection. Primary metric is Sortino, but stability is
 * part of the score: parameter-plateau coverage, trade-count adequacy,
 * drawdown, and walk-forward efficiency (when available). A slightly lower
 * Sortino with clearly better stability beats a fragile argmax.
 */
public final class TimeframeSelector {

    public record TimeframeAnalysis(
            Timeframe timeframe,
            double sortino,
            int trades,
            double maxDrawdown,
            double stableParameterShare,   // fraction of params with plateaus
            double walkForwardEfficiency,  // NaN when WF wasn't run
            double robustnessScore,
            boolean adequateSample) {
    }

    public record Result(List<TimeframeAnalysis> perTimeframe,
                         Timeframe selected,
                         String reason) {
    }

    public static Result select(OptimizationContext context, ResearchConfiguration config) {
        List<TimeframeAnalysis> analyses = new ArrayList<>();
        for (var e : context.getSelections().entrySet()) {
            Timeframe tf = e.getKey();
            SelectionResult sel = e.getValue();
            OptimizationMetrics m = sel.selectedResult().metrics();
            long stable = sel.perParameter().values().stream()
                    .filter(ParameterSelection::stable).count();
            double share = sel.perParameter().isEmpty() ? 0
                    : (double) stable / sel.perParameter().size();
            var wf = context.getWalkForwardResults().get(tf);
            double wfe = wf != null ? wf.walkForwardEfficiency() : Double.NaN;
            boolean adequate = m.totalTrades() >= config.getMinTradesForStrategy();
            // score: sortino × (0.5 + 0.5·stabilityShare) × WFE boost
            double score = m.sortinoRatio()
                    * (0.5 + 0.5 * share)
                    * (Double.isNaN(wfe) ? 1.0 : Math.max(0.3, Math.min(1.5, wfe + 0.5)))
                    * (adequate ? 1.0 : 0.5);
            analyses.add(new TimeframeAnalysis(tf, m.sortinoRatio(), m.totalTrades(),
                    m.maxDrawdown(), share, wfe, score, adequate));
        }
        TimeframeAnalysis best = analyses.stream()
                .max(Comparator.comparingDouble(TimeframeAnalysis::robustnessScore))
                .orElse(null);
        String reason = best == null ? "no selections available"
                : String.format("%s selected: robustness score %.3f "
                        + "(sortino %.2f, stable params %.0f%%, trades %d)",
                        best.timeframe().getCode(), best.robustnessScore(),
                        best.sortino(), best.stableParameterShare() * 100, best.trades());
        return new Result(analyses, best == null ? null : best.timeframe(), reason);
    }

}
