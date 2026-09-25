package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardResult;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shared object threaded through every phase of a research job. Each phase
 * reads what earlier phases produced and appends its own artifacts; the report
 * generator consumes only this object.
 *
 * <p>Mutation methods are synchronized — phases may append from worker
 * threads.</p>
 */
@Getter
public final class OptimizationContext {

    private final ResearchJob job = new ResearchJob();
    private final String strategyId;
    private String strategyName;
    private final List<String> scripIds;
    private final List<Timeframe> timeframes;
    private final ResearchConfiguration configuration;
    private final DataSplit[] dataSplits;

    // ── Phase 1: core optimization ───────────────────────────────────────
    private final List<OptimizationResult> optimizationResults = new ArrayList<>();

    // ── Phase 2: robust parameter selection (per timeframe) ──────────────
    private final Map<Timeframe, SelectionResult> selections = new EnumMap<>(Timeframe.class);

    // ── Phase 3: walk-forward ────────────────────────────────────────────
    private final Map<Timeframe, WalkForwardResult> walkForwardResults =
            new EnumMap<>(Timeframe.class);

    // ── Phase 4: exit optimization (per entry combination) ───────────────
    private final Map<String, ExitOptimizationResult> exitResults =
            new LinkedHashMap<>();

    // ── Phase 5: filter/feature analysis ─────────────────────────────────
    private final List<com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis>
            filterAnalyses = new ArrayList<>();
    private TradeRun finalTradeRun;

    // ── Phase 6: timeframe selection ─────────────────────────────────────
    private com.whiteowl.core.backtest.v2.optimization.analysis.TimeframeSelector.Result
            timeframeSelection;

    // ── Phase 7: exploratory analysis ────────────────────────────────────
    private final Map<String, List<com.whiteowl.core.backtest.v2.optimization.analysis.BucketStats>>
            calendarAnalyses = new LinkedHashMap<>();
    private com.whiteowl.core.backtest.v2.optimization.analysis.StreakAnalyzer.Result streakResult;
    private com.whiteowl.core.backtest.v2.optimization.analysis.AutocorrelationAnalyzer.Result
            autocorrelationResult;
    private com.whiteowl.core.backtest.v2.optimization.analysis.RMultipleAnalyzer.Result rMultipleResult;
    private com.whiteowl.core.backtest.v2.optimization.analysis.DrawdownAnalyzer.Result drawdownResult;
    private final List<com.whiteowl.core.backtest.v2.optimization.analysis.RegimeAnalyzer.Result>
            regimeResults = new ArrayList<>();

    // ── Phase 8: validation & final selection ────────────────────────────
    private com.whiteowl.core.backtest.v2.optimization.analysis.MonteCarloAnalyzer.Result
            monteCarloResult;
    private com.whiteowl.core.backtest.v2.optimization.analysis.CostSensitivityAnalyzer.Result
            costSensitivityResult;
    private FinalConfiguration finalConfiguration;

    // ── Cross-phase ──────────────────────────────────────────────────────
    private final List<ResearchWarning> warnings = new ArrayList<>();
    private final Map<String, String> auditData = new LinkedHashMap<>();

    public OptimizationContext(String strategyId, List<String> scripIds,
                                List<Timeframe> timeframes,
                                ResearchConfiguration configuration,
                                DataSplit[] dataSplits) {
        this.strategyId = strategyId;
        this.scripIds = List.copyOf(scripIds);
        this.timeframes = List.copyOf(timeframes);
        this.configuration = configuration;
        this.dataSplits = dataSplits;
    }

    public static OptimizationContext of(String strategyId, List<String> scripIds,
                                          List<Timeframe> timeframes,
                                          ResearchConfiguration configuration) {
        return new OptimizationContext(strategyId, scripIds, timeframes, configuration, null);
    }

    public synchronized void addOptimizationResults(List<OptimizationResult> results) {
        optimizationResults.addAll(results);
    }

    public synchronized void recordSelection(Timeframe timeframe, SelectionResult selection) {
        selections.put(timeframe, selection);
    }

    public synchronized void recordWalkForward(Timeframe timeframe, WalkForwardResult result) {
        walkForwardResults.put(timeframe, result);
    }

    /** Record an exit-optimization outcome, keyed by {@code tf + combo id}. */
    public synchronized void recordExitResult(Timeframe timeframe, ExitOptimizationResult result) {
        exitResults.put(timeframe.getCode() + "|" + result.entryCombination().id(), result);
    }

    public synchronized void addFilterAnalyses(
            List<com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis> analyses) {
        filterAnalyses.addAll(analyses);
    }

    public synchronized void setFinalTradeRun(TradeRun run) {
        this.finalTradeRun = run;
    }

    public synchronized void setTimeframeSelection(
            com.whiteowl.core.backtest.v2.optimization.analysis.TimeframeSelector.Result r) {
        this.timeframeSelection = r;
    }

    public synchronized void addCalendarAnalysis(String name,
            List<com.whiteowl.core.backtest.v2.optimization.analysis.BucketStats> buckets) {
        calendarAnalyses.put(name, buckets);
    }

    public synchronized void setStreakResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.StreakAnalyzer.Result r) {
        this.streakResult = r;
    }

    public synchronized void setAutocorrelationResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.AutocorrelationAnalyzer.Result r) {
        this.autocorrelationResult = r;
    }

    public synchronized void setRMultipleResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.RMultipleAnalyzer.Result r) {
        this.rMultipleResult = r;
    }

    public synchronized void setDrawdownResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.DrawdownAnalyzer.Result r) {
        this.drawdownResult = r;
    }

    public synchronized void addRegimeResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.RegimeAnalyzer.Result r) {
        regimeResults.add(r);
    }

    public synchronized void setMonteCarloResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.MonteCarloAnalyzer.Result r) {
        this.monteCarloResult = r;
    }

    public synchronized void setCostSensitivityResult(
            com.whiteowl.core.backtest.v2.optimization.analysis.CostSensitivityAnalyzer.Result r) {
        this.costSensitivityResult = r;
    }

    public synchronized void setFinalConfiguration(FinalConfiguration c) {
        this.finalConfiguration = c;
    }

    public synchronized void setStrategyName(String name) {
        this.strategyName = name;
    }

    /** Display name for reports — falls back to the strategy id. */
    public String displayName() {
        return strategyName != null ? strategyName : strategyId;
    }

    public synchronized void warn(ResearchWarning warning) {
        warnings.add(warning);
    }

    public synchronized void warnAll(List<ResearchWarning> warnings) {
        this.warnings.addAll(warnings);
    }

    public synchronized void audit(String key, String value) {
        auditData.put(key, value);
    }

    /** Results for one timeframe in deterministic grid order. */
    public synchronized List<OptimizationResult> resultsFor(Timeframe timeframe) {
        List<OptimizationResult> out = new ArrayList<>();
        for (OptimizationResult r : optimizationResults) {
            if (r.timeframe() == timeframe) {
                out.add(r);
            }
        }
        return out;
    }

}
