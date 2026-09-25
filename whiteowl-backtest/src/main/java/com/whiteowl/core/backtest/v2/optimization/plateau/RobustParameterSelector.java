package com.whiteowl.core.backtest.v2.optimization.plateau;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ParameterCombination;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ResearchWarning;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 — turns a grid of {@link OptimizationResult}s into a robust
 * parameter selection.
 *
 * <p>For each optimizable parameter a sensitivity curve is built by holding
 * all other parameters at the reference combination (the best-Sortino result)
 * and varying only that parameter. {@link PlateauDetector} finds stable
 * regions on each curve. The final combination is the best-Sortino result
 * whose every plateaued parameter lies inside its region; if none qualifies,
 * the global argmax is chosen and flagged unstable.</p>
 *
 * <p>Fixed (non-optimizable) inputs contribute no curve and no constraint.</p>
 */
@Slf4j
public final class RobustParameterSelector {

    private final ResearchConfiguration config;

    public RobustParameterSelector(ResearchConfiguration config) {
        this.config = config;
    }

    public SelectionResult select(List<OptimizationResult> results,
                                   List<StrategyInput> inputs) {
        List<OptimizationResult> usable = results.stream()
                .filter(r -> r.status() != OptimizationResult.Status.FAILED)
                .filter(r -> r.metrics() != null && !Float.isNaN(r.metrics().sortinoRatio()))
                .toList();
        if (usable.isEmpty()) {
            throw new IllegalStateException("No successful optimization results to select from");
        }

        OptimizationResult reference = usable.stream()
                .max(Comparator.comparingDouble(r -> r.metrics().sortinoRatio()))
                .orElseThrow();

        List<StrategyInput> optimizable = inputs.stream()
                .filter(i -> i.hasMin() && i.hasMax())
                .toList();

        Map<String, PlateauResult> plateaus = new LinkedHashMap<>();
        List<ResearchWarning> warnings = new ArrayList<>();
        for (StrategyInput input : optimizable) {
            List<CurvePoint> curve = buildCurve(usable, input.getName(), reference);
            PlateauResult pr = PlateauDetector.detect(input.getName(), curve, config);
            plateaus.put(input.getName(), pr);
            if (!pr.stable()) {
                warnings.add(ResearchWarning.HIGH_PARAMETER_SENSITIVITY);
            }
            log.debug("Plateau [{}]: {}", input.getName(), pr.reason());
        }

        // Candidate = usable result inside every detected plateau.
        List<OptimizationResult> candidates = new ArrayList<>();
        for (OptimizationResult r : usable) {
            boolean inside = true;
            for (Map.Entry<String, PlateauResult> e : plateaus.entrySet()) {
                PlateauRegion region = e.getValue().region();
                if (region == null) continue;
                Number v = r.combination().get(e.getKey());
                if (v == null || !region.contains(v.doubleValue())) {
                    inside = false;
                    break;
                }
            }
            if (inside) candidates.add(r);
        }

        OptimizationResult selected;
        if (!candidates.isEmpty()) {
            selected = candidates.stream()
                    .max(Comparator.comparingDouble(r -> r.metrics().sortinoRatio()))
                    .orElseThrow();
        } else {
            selected = reference;
            warnings.add(ResearchWarning.HIGH_PARAMETER_SENSITIVITY);
        }
        if (selected.metrics().totalTrades() < config.getMinTradesForStrategy()) {
            warnings.add(ResearchWarning.LOW_TRADE_COUNT);
        }

        Map<String, ParameterSelection> perParameter = new LinkedHashMap<>();
        for (StrategyInput input : optimizable) {
            PlateauResult pr = plateaus.get(input.getName());
            PlateauRegion region = pr.region();
            Number sel = selected.combination().get(input.getName());
            String reason = pr.stable()
                    ? pr.reason()
                    : "unstable curve — " + pr.reason();
            perParameter.put(input.getName(), new ParameterSelection(
                    input.getName(),
                    sel != null ? sel.doubleValue() : Double.NaN,
                    region != null ? region.lowerBound() : Double.NaN,
                    region != null ? region.upperBound() : Double.NaN,
                    region != null ? region.robustnessScore() : Double.NaN,
                    pr.stable(), reason));
        }

        return new SelectionResult(perParameter, plateaus, selected.combination(),
                selected, warnings);
    }

    /**
     * One-dimensional metric-vs-parameter curve: all results whose other
     * parameters equal the reference combination's values.
     */
    private List<CurvePoint> buildCurve(List<OptimizationResult> usable,
                                       String parameter, OptimizationResult reference) {
        List<CurvePoint> curve = new ArrayList<>();
        for (OptimizationResult r : usable) {
            boolean match = true;
            for (Map.Entry<String, Number> e : reference.combination().values().entrySet()) {
                if (e.getKey().equals(parameter)) continue;
                Number v = r.combination().get(e.getKey());
                if (v == null
                        || Double.compare(v.doubleValue(), e.getValue().doubleValue()) != 0) {
                    match = false;
                    break;
                }
            }
            if (match) {
                Number v = r.combination().get(parameter);
                if (v != null) {
                    curve.add(new CurvePoint(v.doubleValue(),
                            r.metrics().sortinoRatio(), r.metrics().totalTrades()));
                }
            }
        }
        return curve;
    }

}
