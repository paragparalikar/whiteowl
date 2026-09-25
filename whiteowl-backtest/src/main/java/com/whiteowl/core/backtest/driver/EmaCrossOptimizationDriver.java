package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.optimization.BarsLoader;
import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.OptimizationEngine;
import com.whiteowl.core.backtest.v2.optimization.OptimizationRequest;
import com.whiteowl.core.backtest.v2.optimization.OptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.ParameterConstraint;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.backtest.v2.optimization.FinalConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ScripSlice;
import com.whiteowl.core.backtest.v2.optimization.TradeRun;
import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.backtest.v2.optimization.analysis.AutocorrelationAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.CalendarAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.CostSensitivityAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.DrawdownAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalysis;
import com.whiteowl.core.backtest.v2.optimization.analysis.FilterAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.MonteCarloAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.RMultipleAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.StreakAnalyzer;
import com.whiteowl.core.backtest.v2.optimization.analysis.TimeframeSelector;
import com.whiteowl.core.backtest.v2.optimization.analysis.TradeObservation;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitGrid;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizationResult;
import com.whiteowl.core.backtest.v2.optimization.exit.ExitOptimizer;
import com.whiteowl.core.backtest.v2.optimization.plateau.RobustParameterSelector;
import com.whiteowl.core.backtest.v2.optimization.report.HtmlReportGenerator;
import com.whiteowl.core.backtest.v2.optimization.plateau.SelectionResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardConfig;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardEngine;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardResult;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardWindowResult;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Exercises the optimization pipeline end-to-end on the EMA-crossover
 * timeboxed strategy:
 *
 * <pre>
 *   Phase 1  grid × {1m,3m,5m,10m,15m} → metrics table
 *   Phase 2  robust (plateau) selection per timeframe
 *   Phase 3  walk-forward on the best timeframe
 *   Phase 4  MAE/MFE-driven exit optimization on the selected entry params
 * </pre>
 *
 * Higher timeframes are aggregated in-memory from 1-minute bars (epoch-aligned
 * buckets, same convention as IntradayBarAggregator).
 *
 * <p>Usage: {@code EmaCrossOptimizationDriver [scripId]} — default
 * {@code NSE:ACC} (the NIFTY 50 index has daily bars only in the local
 * store; ACC has ~92k real 1-minute bars).</p>
 */
public final class EmaCrossOptimizationDriver {

    private static final Timeframe[] TIMEFRAMES = {
            Timeframe.ONE_MINUTE, Timeframe.THREE_MINUTE, Timeframe.FIVE_MINUTE,
            Timeframe.TEN_MINUTE, Timeframe.FIFTEEN_MINUTE};

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    public static void main(String[] args) throws Exception {
        String scripId = args.length > 0 ? args[0] : "NSE:ACC";

        BarsRepository repo = new FileBarsRepository();
        Bars oneMin = repo.load(scripId, Timeframe.ONE_MINUTE);

        List<Timeframe> timeframes;
        Map<Timeframe, BarsArrays> barsByTf = new EnumMap<>(Timeframe.class);
        if (oneMin != null && oneMin.size() > 0) {
            System.out.printf("Loaded %d 1-min bars for %s (%s → %s)%n",
                    oneMin.size(), scripId,
                    FMT.format(Instant.ofEpochMilli(oneMin.getTimestamp(0))),
                    FMT.format(Instant.ofEpochMilli(oneMin.getTimestamp(oneMin.size() - 1))));
            barsByTf.put(Timeframe.ONE_MINUTE, oneMin.arrays());
            for (Timeframe tf : TIMEFRAMES) {
                if (tf == Timeframe.ONE_MINUTE) continue;
                BarsArrays agg = aggregate(oneMin.arrays(), tf.getSeconds() * 1000L);
                barsByTf.put(tf, agg);
                System.out.printf("  aggregated %-12s → %d bars%n", tf.getCode(), agg.size());
            }
            timeframes = List.of(TIMEFRAMES);
        } else {
            // Fallback: instrument has no intraday data — try daily.
            Bars daily = repo.load(scripId, Timeframe.DAILY);
            if (daily == null || daily.size() == 0) {
                System.out.println("No bars for " + scripId);
                return;
            }
            System.out.printf("No intraday bars for %s — running on %d DAILY bars "
                    + "(time-of-day gates cannot fire on daily bars)%n",
                    scripId, daily.size());
            barsByTf.put(Timeframe.DAILY, daily.arrays());
            timeframes = List.of(Timeframe.DAILY);
        }
        BarsLoader loader = (id, tf) -> barsByTf.get(tf);

        String source = readResource("/strategies/EmaCrossTimeboxed.groovy");
        StrategySpec strategy = StrategySpec.script(source);
        List<StrategyInput> inputs = strategy.declaredInputs();
        System.out.println("Declared inputs:");
        inputs.forEach(i -> System.out.printf("  %-10s def=%s min=%s max=%s step=%s%n",
                i.getName(), i.getDefaultValue(), i.getMinValue(), i.getMaxValue(), i.getStep()));

        ResearchConfiguration config = ResearchConfiguration.builder().build();
        OptimizationContext context = new OptimizationContext(
                strategy.strategyId(), List.of(scripId), timeframes, config, null);
        OptimizationEngine engine = new OptimizationEngine(config);
        List<ParameterConstraint> constraints =
                List.of(ParameterConstraint.lessThan("shortEma", "longEma"));

        // ── Phase 1 ──────────────────────────────────────────────────────
        OptimizationRequest request = OptimizationRequest.builder()
                .strategy(strategy)
                .scrip(scripId)
                .timeframes(timeframes)
                .barsLoader(loader)
                .parameters(inputs)
                .constraints(constraints)
                .build();
        engine.optimize(request, context);
        System.out.println("\n═══ Phase 1 — grid results ═══");
        for (Timeframe tf : timeframes) {
            List<OptimizationResult> tfResults = context.resultsFor(tf);
            long ok = tfResults.stream()
                    .filter(r -> r.status() == OptimizationResult.Status.OK).count();
            System.out.printf("%-4s : %d combos (%d ok)%n", tf.getCode(), tfResults.size(), ok);
            tfResults.stream()
                    .filter(r -> r.status() == OptimizationResult.Status.OK)
                    .sorted(Comparator.comparingDouble(
                            (OptimizationResult r) -> r.metrics().sortinoRatio()).reversed())
                    .limit(5)
                    .forEach(r -> System.out.printf(
                            "    %-34s Sortino=%6.2f trades=%4d winRate=%5.1f%% netPnl=%9.0f maxDD=%6.2f%%%n",
                            r.combination().id(), r.metrics().sortinoRatio(),
                            r.metrics().totalTrades(), r.metrics().winRate(),
                            r.metrics().netProfit(), r.metrics().maxDrawdown()));
        }

        // ── Phase 2 ──────────────────────────────────────────────────────
        RobustParameterSelector selector = new RobustParameterSelector(config);
        Map<Timeframe, SelectionResult> selections = new EnumMap<>(Timeframe.class);
        System.out.println("\n═══ Phase 2 — robust selection per timeframe ═══");
        for (Timeframe tf : timeframes) {
            List<OptimizationResult> tfResults = context.resultsFor(tf);
            try {
                SelectionResult sel = selector.select(tfResults, inputs);
                selections.put(tf, sel);
                context.recordSelection(tf, sel);
                context.warnAll(sel.warnings());
                System.out.printf("%-4s → %s  (Sortino=%.2f, trades=%d)%n",
                        tf.getCode(), sel.combination().id(),
                        sel.selectedResult().metrics().sortinoRatio(),
                        sel.selectedResult().metrics().totalTrades());
                sel.perParameter().forEach((name, p) -> System.out.printf(
                        "       %-10s sel=%-6.4g plateau=[%s, %s] score=%s stable=%s%n",
                        name, p.selectedValue(),
                        Double.isNaN(p.regionLower()) ? "-" : String.format("%.4g", p.regionLower()),
                        Double.isNaN(p.regionUpper()) ? "-" : String.format("%.4g", p.regionUpper()),
                        Double.isNaN(p.robustnessScore()) ? "-" : String.format("%.3f", p.robustnessScore()),
                        p.stable()));
            } catch (IllegalStateException e) {
                System.out.printf("%-4s → selection failed: %s%n", tf.getCode(), e.getMessage());
            }
        }

        // ── Phase 3: walk-forward on the best-selected timeframe ─────────
        java.util.concurrent.atomic.AtomicReference<Timeframe> finalTf =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<StandardExitPolicy> finalPolicy =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<
                com.whiteowl.core.backtest.v2.optimization.ParameterCombination> finalCombo =
                new java.util.concurrent.atomic.AtomicReference<>();
        selections.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue()
                        .selectedResult().metrics().sortinoRatio()))
                .ifPresent(best -> {
                    Timeframe tf = best.getKey();
                    System.out.printf("%n═══ Phase 3 — walk-forward on %s ═══%n", tf.getCode());
                    WalkForwardEngine wf = new WalkForwardEngine(config);
                    WalkForwardResult wfResult = wf.run(strategy,
                            Map.of(scripId, barsByTf.get(tf)), tf, inputs, constraints,
                            WalkForwardConfig.ofMonths(4, 1, 1), null, context);
                    for (WalkForwardWindowResult w : wfResult.windows()) {
                        if (w.failed()) {
                            System.out.printf("  w%d FAILED %s%n", w.windowIndex(), w.errorMessage());
                            continue;
                        }
                        System.out.printf("  w%d %s→%s | %-30s IS=%6.2f OOS=%6.2f oosTrades=%d%n",
                                w.windowIndex(),
                                FMT.format(Instant.ofEpochMilli(w.trainStart())),
                                FMT.format(Instant.ofEpochMilli(w.testEnd())),
                                w.combination().id(),
                                w.inSample().sortinoRatio(),
                                w.outOfSample().sortinoRatio(),
                                w.outOfSample().totalTrades());
                    }
                    wfResult.parameterDrift().forEach((name, d) -> System.out.printf(
                            "  drift %-10s cv=%.3f %s%n", name, d.coefficientOfVariation(),
                            d.stable() ? "STABLE" : "UNSTABLE"));
                    System.out.printf("  WFE=%.2f meanIS=%.2f meanOOS=%.2f warnings=%s%n",
                            wfResult.walkForwardEfficiency(), wfResult.meanInSampleSortino(),
                            wfResult.meanOutOfSampleSortino(), wfResult.warnings());

                    // ── Phase 4: exits for the selected entry combo ──────
                    System.out.printf("%n═══ Phase 4 — exit optimization on %s @ %s ═══%n",
                            best.getValue().combination().id(), tf.getCode());
                    ExitOptimizer exits = new ExitOptimizer(config, engine);
                    ExitOptimizationResult er = exits.optimize(strategy,
                            best.getValue().combination(),
                            List.of(new ScripSlice(scripId, barsByTf.get(tf))), tf,
                            ExitGrid.builder().build());
                    context.recordExitResult(tf, er);
                    finalTf.set(tf);
                    finalCombo.set(best.getValue().combination());
                    finalPolicy.set(er.policy());
                    System.out.printf("  policy=%s%n  Sortino=%.2f trades=%d maxDD=%.2f%%%n",
                            er.policy(), er.metrics().sortinoRatio(),
                            er.metrics().totalTrades(), er.metrics().maxDrawdown());
                });

        if (finalTf.get() != null) {
            Timeframe tf = finalTf.get();
            List<ScripSlice> slices =
                    List.of(new ScripSlice(scripId, barsByTf.get(tf)));

            // ── Phases 5+7: detailed run (feature observations) ──────────
            TradeRun run = engine.evaluateDetailed(strategy, finalCombo.get(), slices,
                    finalPolicy.get(), false, true,
                    finalPolicy.get() != null && finalPolicy.get().initialStopAtr() != null
                            ? finalPolicy.get().initialStopAtr() : 0);
            context.setFinalTradeRun(run);
            List<TradeObservation> obs = run.observations().stream()
                    .sorted(Comparator.comparingLong(TradeObservation::exitTimestamp))
                    .toList();
            System.out.printf("%n═══ Phases 5–8 on %d trades ═══%n", obs.size());

            // Phase 5: univariate filter analysis
            FilterAnalyzer filterAnalyzer = new FilterAnalyzer(10, 20, 0.0);
            List<FilterAnalysis> filters =
                    filterAnalyzer.analyze(obs, FilterAnalyzer.DEFAULT_FEATURES);
            context.addFilterAnalyses(filters);
            for (FilterAnalysis fa : filters) {
                System.out.printf("  filter %-16s %s | %s%n", fa.feature(),
                        fa.accepted() ? "ACCEPTED " + String.format("[%.3g,%.3g]",
                                fa.regionLower(), fa.regionUpper()) : "rejected",
                        fa.reason());
            }

            // Phase 6: timeframe selection (uses P2 selections + P3 WFE)
            var tfSel = TimeframeSelector.select(context, config);
            context.setTimeframeSelection(tfSel);
            System.out.println("  timeframe → " + tfSel.reason());

            // Phase 7: exploratory analysis
            context.addCalendarAnalysis("Time of day (30m buckets)",
                    CalendarAnalyzer.timeOfDay(obs, 30));
            context.addCalendarAnalysis("Day of week", CalendarAnalyzer.dayOfWeek(obs));
            context.addCalendarAnalysis("Week of month", CalendarAnalyzer.weekOfMonth(obs));
            context.addCalendarAnalysis("Month of year", CalendarAnalyzer.monthOfYear(obs));
            var streak = StreakAnalyzer.analyze(obs, 5);
            context.setStreakResult(streak);
            var acf = AutocorrelationAnalyzer.analyze(obs, 10);
            context.setAutocorrelationResult(acf);
            var rm = RMultipleAnalyzer.analyze(obs, 15);
            context.setRMultipleResult(rm);
            var dd = DrawdownAnalyzer.analyze(run.equityCurve());
            context.setDrawdownResult(dd);
            System.out.printf("  ACF significant=%s skew=%s | drawdown max=%.2f%% | "
                            + "P(W|W)=%.2f%n",
                    acf.significant(), RMultipleAnalyzer.classifySkew(rm.skewness()),
                    dd.maxDrawdown(), streak.transitions().pWinGivenWin());

            // Phase 8: Monte Carlo + cost sensitivity + final config
            var mc = MonteCarloAnalyzer.analyze(MonteCarloAnalyzer.pnlOf(run.trades()),
                    config.getInitialCapital(), 500, 42L);
            context.setMonteCarloResult(mc);
            var cs = CostSensitivityAnalyzer.analyze(strategy, finalCombo.get(), slices,
                    finalPolicy.get(), config, new double[]{0.5, 1.0, 1.5, 2.0});
            context.setCostSensitivityResult(cs);
            System.out.printf("  MC: medDD=%.1f%% p99DD=%.1f%% P(loss)=%.0f%% | "
                            + "costFragile=%s%n",
                    mc.medianMaxDrawdown(), mc.p99MaxDrawdown(),
                    mc.probabilityOfLoss(), cs.fragile());

            // Multiple-testing / complexity awareness: many tried hypotheses
            // inflate the chance of a lucky result even with honest mechanics.
            int hypotheses = context.getOptimizationResults().size()
                    + filters.size() * 10;
            context.audit("hypothesesTested", String.valueOf(hypotheses));
            if (hypotheses > config.getMultipleTestingThreshold()) {
                context.warn(com.whiteowl.core.backtest.v2.optimization
                        .ResearchWarning.POSSIBLE_OVERFITTING);
            }
            if (cs.fragile()) {
                context.warn(com.whiteowl.core.backtest.v2.optimization
                        .ResearchWarning.HIGH_TRANSACTION_COST_SENSITIVITY);
            }
            if (mc.p99MaxDrawdown() > config.getMaxDrawdownThresholdPercent()) {
                context.warn(com.whiteowl.core.backtest.v2.optimization
                        .ResearchWarning.HIGH_DRAWDOWN);
            }

            var fc = new FinalConfiguration(strategy.strategyId(), List.of(scripId),
                    "LONG", tf, finalCombo.get(), finalPolicy.get(),
                    config.getInitialCapital(), config.getCostPercent(),
                    config.getSlippagePercent(), run.metrics(), context.getWarnings());
            context.setFinalConfiguration(fc);
            Path cfgPath = Path.of("final-config-" + scripId.replace(':', '_')
                    .replace(' ', '_') + ".json");
            Files.writeString(cfgPath, fc.toJson());
            System.out.println("  final config: " + cfgPath.toAbsolutePath());
        }

        System.out.println("\nContext warnings: " + context.getWarnings());

        // ── Phase 9: HTML report ─────────────────────────────────────────
        Path report = HtmlReportGenerator.write(context,
                Path.of("optimization-report-" + scripId.replace(':', '_').replace(' ', '_')
                        + ".html"));
        System.out.println("Report written: " + report.toAbsolutePath());
    }

    /** Epoch-aligned aggregation (same convention as IntradayBarAggregator). */
    private static BarsArrays aggregate(BarsArrays src, long intervalMs) {
        List<Float> o = new ArrayList<>(), h = new ArrayList<>(), l = new ArrayList<>(), c = new ArrayList<>();
        List<Long> v = new ArrayList<>(), ts = new ArrayList<>();
        int i = 0, n = src.size();
        while (i < n) {
            long bucket = (src.timestamp()[i] / intervalMs) * intervalMs;
            long day = src.timestamp()[i] / 86_400_000L;
            float bo = src.open()[i], bh = src.high()[i], bl = src.low()[i], bc;
            long bv = 0;
            int j = i;
            for (; j < n; j++) {
                long t = src.timestamp()[j];
                if ((t / intervalMs) * intervalMs != bucket || t / 86_400_000L != day) break;
                bh = Math.max(bh, src.high()[j]);
                bl = Math.min(bl, src.low()[j]);
                bv += src.volume()[j];
            }
            bc = src.close()[j - 1];
            ts.add(bucket); o.add(bo); h.add(bh); l.add(bl); c.add(bc); v.add(bv);
            i = j;
        }
        float[] fo = new float[o.size()], fh = new float[o.size()], fl = new float[o.size()],
                fc = new float[o.size()];
        long[] fv = new long[o.size()], ft = new long[o.size()];
        for (int k = 0; k < o.size(); k++) {
            fo[k] = o.get(k); fh[k] = h.get(k); fl[k] = l.get(k); fc[k] = c.get(k);
            fv[k] = v.get(k); ft[k] = ts.get(k);
        }
        return new BarsArrays(fo, fh, fl, fc, fv, ft, o.size());
    }

    private static String readResource(String path) throws Exception {
        try (InputStream in = EmaCrossOptimizationDriver.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
