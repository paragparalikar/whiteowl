package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.BarsLoader;
import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationRequest;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitGrid;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizer;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.plateau.ParameterSelection;
import com.whiteowl.core.backtest.v2.optimization.plateau.RobustParameterSelector;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardConfig;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardEngine;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardWindowResult;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * End-to-end driver for the optimization engine (Phases 0–4 of the research
 * spec): parameter grid → performance metrics → robust selection →
 * walk-forward → exit optimization.
 *
 * <p>Usage: {@code StrategyOptimizationDriver <scripId> [timeframe]}
 * e.g. {@code StrategyOptimizationDriver NIFTY 15min}</p>
 */
public final class StrategyOptimizationDriver {

    public static void main(String[] args) throws Exception {
        String scripId = args.length > 0 ? args[0] : "NIFTY";
        Timeframe timeframe = args.length > 1 ? parseTimeframe(args[1]) : Timeframe.FIFTEEN_MINUTE;

        String source = readResource("/strategies/Supertrend_Nifty50.groovy");
        StrategySpec strategy = StrategySpec.script(source);
        List<StrategyInput> inputs = strategy.declaredInputs();
        System.out.println("Declared inputs: " + inputs);

        ResearchConfiguration config = ResearchConfiguration.builder().build();
        BarsRepository repo = new FileBarsRepository();
        BarsLoader loader = BarsLoader.fromRepository(repo);

        OptimizationContext context = OptimizationContext.of(
                strategy.strategyId(), List.of(scripId), List.of(timeframe), config);
        context.getJob().setState(com.whiteowl.core.backtest.v2.optimization.ResearchJob.State.RUNNING);

        // ── Phase 1: grid × metrics ──────────────────────────────────────
        OptimizationEngine engine = new OptimizationEngine(config);
        OptimizationRequest request = OptimizationRequest.builder()
                .strategy(strategy)
                .scrip(scripId)
                .timeframe(timeframe)
                .barsLoader(loader)
                .parameters(inputs)
                .listener((done, total, r) -> System.out.printf(
                        "\r[Phase 1] %d/%d evaluated", done, total))
                .build();
        List<OptimizationResult> results = engine.optimize(request, context);
        System.out.println();

        results.stream()
                .filter(r -> r.status() == OptimizationResult.Status.OK)
                .sorted(Comparator.comparingDouble(
                        (OptimizationResult r) -> r.metrics().sortinoRatio()).reversed())
                .limit(10)
                .forEach(r -> System.out.printf("  %-28s Sortino=%.2f trades=%d netPnl=%.0f%n",
                        r.combination().id(), r.metrics().sortinoRatio(),
                        r.metrics().totalTrades(), r.metrics().netProfit()));

        // ── Phase 2: robust selection ────────────────────────────────────
        RobustParameterSelector selector = new RobustParameterSelector(config);
        SelectionResult selection = selector.select(context.resultsFor(timeframe), inputs);
        context.recordSelection(timeframe, selection);
        System.out.println("\n[Phase 2] Selected: " + selection.combination().id());
        for (ParameterSelection p : selection.perParameter().values()) {
            System.out.printf("  %-15s selected=%.4g plateau=[%.4g, %.4g] stable=%s%n",
                    p.parameter(), p.selectedValue(), p.regionLower(), p.regionUpper(),
                    p.stable());
        }
        System.out.println("  Warnings: " + selection.warnings());

        // ── Phase 3: walk-forward ────────────────────────────────────────
        BarsArrays full = loader.load(scripId, timeframe);
        if (full != null && full.size() > 0) {
            WalkForwardEngine wf = new WalkForwardEngine(config);
            WalkForwardResult wfResult = wf.run(strategy, Map.of(scripId, full),
                    timeframe, inputs, List.of(),
                    WalkForwardConfig.ofMonths(4, 2, 2), null, context);
            System.out.printf("%n[Phase 3] windows=%d WFE=%.2f meanIS=%.2f meanOOS=%.2f%n",
                    wfResult.windows().size(), wfResult.walkForwardEfficiency(),
                    wfResult.meanInSampleSortino(), wfResult.meanOutOfSampleSortino());
            for (WalkForwardWindowResult w : wfResult.windows()) {
                if (w.failed()) continue;
                System.out.printf("  window %d: %s IS=%.2f OOS=%.2f trades=%d%n",
                        w.windowIndex(), w.combination().id(),
                        w.inSample().sortinoRatio(), w.outOfSample().sortinoRatio(),
                        w.outOfSample().totalTrades());
            }
            wfResult.parameterDrift().forEach((name, d) -> System.out.printf(
                    "  drift %-15s mean=%.3g sd=%.3g cv=%.3f %s%n",
                    name, d.mean(), d.stdDev(), d.coefficientOfVariation(),
                    d.stable() ? "STABLE" : "UNSTABLE"));

            // ── Phase 4: exit optimization on selected entry params ──────
            ExitOptimizer exits = new ExitOptimizer(config, engine);
            ExitOptimizationResult exitResult = exits.optimize(strategy,
                    selection.combination(),
                    List.of(new ScripSlice(scripId, full)), timeframe,
                    ExitGrid.builder().build());
            System.out.printf("%n[Phase 4] exit=%s Sortino=%.2f trades=%d%n",
                    exitResult.policy(), exitResult.metrics().sortinoRatio(),
                    exitResult.metrics().totalTrades());
        }

        System.out.println("\nContext warnings: " + context.getWarnings());
        context.getJob().setState(com.whiteowl.core.backtest.v2.optimization.ResearchJob.State.COMPLETED);
    }

    private static Timeframe parseTimeframe(String code) {
        for (Timeframe tf : Timeframe.values()) {
            if (tf.getCode().equalsIgnoreCase(code) || tf.getLabel().equalsIgnoreCase(code)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe: " + code);
    }

    private static String readResource(String path) throws Exception {
        try (InputStream in = StrategyOptimizationDriver.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
