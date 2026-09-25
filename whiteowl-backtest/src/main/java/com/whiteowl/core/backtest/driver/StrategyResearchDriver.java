package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.optimization.OptimizationContext;
import com.whiteowl.core.backtest.v2.optimization.ParameterConstraint;
import com.whiteowl.core.backtest.v2.optimization.ResearchConfiguration;
import com.whiteowl.core.backtest.v2.optimization.ResearchPipeline;
import com.whiteowl.core.backtest.v2.optimization.StrategySpec;
import com.whiteowl.core.backtest.v2.optimization.report.HtmlReportGenerator;
import com.whiteowl.core.backtest.v2.optimization.walkforward.WalkForwardConfig;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic research driver — runs the full optimization pipeline
 * (Phases 1–8) for ANY strategy against any instruments, then writes the
 * HTML report and a final-configuration JSON export.
 *
 * <p>Usage:
 * <pre>
 *   StrategyResearchDriver
 *       [-strategy res:/strategies/EmaCrossTimeboxed.groovy | file:/path.groovy]
 *       [-scrip NSE:ACC [-scrip NSE:RELIANCE ...]]
 *       [-tf 1min,3min,5min,10min,15min]
 *       [-constraint "shortEma&lt;longEma" ...]
 *       [-wf trainMonths,testMonths,stepMonths]
 *       [-out outputDir]
 * </pre>
 *
 * Bar loading: each requested timeframe is read from the repository; when a
 * higher timeframe is missing but 1-minute bars exist, it is aggregated
 * in-memory (epoch-aligned, same convention as IntradayBarAggregator). If
 * only daily bars exist, the daily timeframe can be requested explicitly.
 */
public final class StrategyResearchDriver {

    private static final String DEFAULT_STRATEGY = "res:/strategies/EmaCrossTimeboxed.groovy";
    private static final Timeframe[] DEFAULT_TFS = {
            Timeframe.ONE_MINUTE, Timeframe.THREE_MINUTE, Timeframe.FIVE_MINUTE,
            Timeframe.TEN_MINUTE, Timeframe.FIFTEEN_MINUTE};

    public static void main(String[] args) throws Exception {
        String strategyPath = DEFAULT_STRATEGY;
        List<String> scripIds = new ArrayList<>();
        List<Timeframe> tfs = List.of(DEFAULT_TFS);
        List<ParameterConstraint> constraints = new ArrayList<>();
        WalkForwardConfig wf = WalkForwardConfig.ofMonths(4, 1, 1);
        Path outDir = Path.of(".");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-strategy" -> strategyPath = args[++i];
                case "-scrip" -> scripIds.addAll(List.of(args[++i].split(",")));
                case "-tf" -> {
                    List<Timeframe> parsed = new ArrayList<>();
                    for (String code : args[++i].split(",")) {
                        parsed.add(parseTimeframe(code.trim()));
                    }
                    tfs = parsed;
                }
                case "-constraint" -> constraints.add(parseConstraint(args[++i]));
                case "-wf" -> {
                    String[] p = args[++i].split(",");
                    wf = WalkForwardConfig.ofMonths(
                            Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                            Integer.parseInt(p[2]));
                }
                case "-out" -> outDir = Path.of(args[++i]);
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
                    printUsage();
                    return;
                }
            }
        }
        if (scripIds.isEmpty()) {
            scripIds.add("NSE:ACC");
        }

        String strategyName = strategyNameOf(strategyPath);
        StrategySpec strategy =
                StrategySpec.script(readSource(strategyPath), strategyName);
        System.out.println("Strategy: " + strategy.strategyId());
        strategy.declaredInputs().forEach(i -> System.out.printf(
                "  input %-16s def=%s min=%s max=%s step=%s%n",
                i.getName(), i.getDefaultValue(), i.getMinValue(),
                i.getMaxValue(), i.getStep()));
        System.out.println("Scrips: " + scripIds + " | TFs: "
                + tfs.stream().map(Timeframe::getCode).toList()
                + " | constraints: " + constraints.size());

        // ── Data loading (direct + aggregate-from-1m fallback) ───────────
        BarsRepository repo = new FileBarsRepository();
        Map<String, Map<Timeframe, BarsArrays>> bars = new LinkedHashMap<>();
        for (String scripId : scripIds) {
            Map<Timeframe, BarsArrays> byTf = new EnumMap<>(Timeframe.class);
            BarsArrays oneMin = null;
            for (Timeframe tf : tfs) {
                Bars b = repo.load(scripId, tf);
                BarsArrays arr = b != null ? b.arrays() : null;
                // Tiny bin files are placeholder artifacts, not real data.
                if (arr != null && arr.size() < 100) arr = null;
                if (tf == Timeframe.ONE_MINUTE) {
                    oneMin = arr;
                }
                byTf.put(tf, arr);
            }
            if (oneMin == null) {
                Bars b = repo.load(scripId, Timeframe.ONE_MINUTE);
                oneMin = b != null && b.size() >= 100 ? b.arrays() : null;
            }
            for (Timeframe tf : tfs) {
                if (byTf.get(tf) == null && oneMin != null
                        && tf != Timeframe.ONE_MINUTE && tf.getSeconds() < 86_400) {
                    BarsArrays agg = aggregate(oneMin, tf.getSeconds() * 1000L);
                    byTf.put(tf, agg);
                    System.out.printf("  %s %s: aggregated %d bars from 1min%n",
                            scripId, tf.getCode(), agg.size());
                }
            }
            byTf.entrySet().removeIf(e -> e.getValue() == null);
            if (byTf.isEmpty()) {
                System.out.println("  " + scripId + ": no data — skipped");
                continue;
            }
            byTf.forEach((tf, a) -> System.out.printf("  %s %s: %d bars%n",
                    scripId, tf.getCode(), a.size()));
            bars.put(scripId, byTf);
        }
        if (bars.isEmpty()) {
            System.out.println("No data available for any scrip.");
            return;
        }
        // Timeframes = intersection across scrips.
        List<Timeframe> commonTfs = tfs.stream()
                .filter(tf -> bars.values().stream().allMatch(m -> m.containsKey(tf)))
                .toList();
        if (commonTfs.isEmpty()) {
            System.out.println("No common timeframe with data across scrips.");
            return;
        }
        Map<String, Map<Timeframe, BarsArrays>> common = new LinkedHashMap<>();
        bars.forEach((id, m) -> {
            Map<Timeframe, BarsArrays> mm = new EnumMap<>(Timeframe.class);
            for (Timeframe tf : commonTfs) mm.put(tf, m.get(tf));
            common.put(id, mm);
        });

        // ── Pipeline ─────────────────────────────────────────────────────
        OptimizationContext ctx = new ResearchPipeline(
                ResearchConfiguration.builder().build())
                .run(strategy, common, constraints, wf);

        printSummary(ctx);

        // ── Outputs ──────────────────────────────────────────────────────
        Files.createDirectories(outDir);
        String name = ctx.displayName().replaceAll("[^A-Za-z0-9_.-]", "_");
        Path report = HtmlReportGenerator.write(ctx,
                outDir.resolve(name + ".html"));
        System.out.println("Report: " + report.toAbsolutePath());
        if (ctx.getFinalConfiguration() != null) {
            Path cfg = outDir.resolve(name + "-final-config.json");
            Files.writeString(cfg, ctx.getFinalConfiguration().toJson());
            System.out.println("Config: " + cfg.toAbsolutePath());
        }
    }

    private static void printSummary(OptimizationContext ctx) {
        System.out.println("\n── Results ──");
        for (Timeframe tf : ctx.getTimeframes()) {
            var rs = ctx.resultsFor(tf);
            long ok = rs.stream().filter(r -> r.metrics() != null).count();
            System.out.printf("  %-5s %d combos evaluated (%d with metrics)%n",
                    tf.getCode(), rs.size(), ok);
        }
        ctx.getSelections().forEach((tf, sel) -> System.out.printf(
                "  %-5s selected %s (Sortino %.2f, %d trades)%n",
                tf.getCode(), sel.combination().id(),
                sel.selectedResult().metrics().sortinoRatio(),
                sel.selectedResult().metrics().totalTrades()));
        ctx.getWalkForwardResults().forEach((tf, r) -> System.out.printf(
                "  %-5s WF: %d windows, WFE %.2f, warnings %s%n",
                tf.getCode(), r.windows().size(), r.walkForwardEfficiency(), r.warnings()));
        ctx.getExitResults().forEach((k, r) -> System.out.printf(
                "  exits %s → %s (Sortino %.2f)%n",
                k, r.policy(), r.metrics().sortinoRatio()));
        if (ctx.getTimeframeSelection() != null) {
            System.out.println("  timeframe: " + ctx.getTimeframeSelection().reason());
        }
        if (ctx.getMonteCarloResult() != null) {
            var mc = ctx.getMonteCarloResult();
            System.out.printf("  MC: medDD %.1f%% p99DD %.1f%% P(loss) %.0f%%%n",
                    mc.medianMaxDrawdown(), mc.p99MaxDrawdown(), mc.probabilityOfLoss());
        }
        if (ctx.getCostSensitivityResult() != null) {
            System.out.println("  costs: "
                    + (ctx.getCostSensitivityResult().fragile() ? "FRAGILE" : "robust"));
        }
        System.out.println("  warnings: " + ctx.getWarnings());
    }

    private static ParameterConstraint parseConstraint(String spec) {
        String trimmed = spec.trim();
        int lt = trimmed.indexOf('<');
        int gt = trimmed.indexOf('>');
        int ne = trimmed.indexOf("!=");
        if (lt > 0 && gt < 0) {
            return ParameterConstraint.lessThan(trimmed.substring(0, lt).trim(),
                    trimmed.substring(lt + 1).trim());
        }
        if (gt > 0) {
            return ParameterConstraint.lessThan(trimmed.substring(gt + 1).trim(),
                    trimmed.substring(0, gt).trim());
        }
        if (ne > 0) {
            String a = trimmed.substring(0, ne).trim();
            String b = trimmed.substring(ne + 2).trim();
            return ParameterConstraint.of(a + " != " + b,
                    p -> !p.get(a).equals(p.get(b)));
        }
        throw new IllegalArgumentException(
                "Constraint must look like 'a<b', 'a>b' or 'a!=b': " + spec);
    }

    private static Timeframe parseTimeframe(String code) {
        for (Timeframe tf : Timeframe.values()) {
            if (tf.getCode().equalsIgnoreCase(code) || tf.name().equalsIgnoreCase(code)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe: " + code);
    }

    /** Epoch-aligned aggregation (same convention as IntradayBarAggregator). */
    private static BarsArrays aggregate(BarsArrays src, long intervalMs) {
        List<Float> o = new ArrayList<>(), h = new ArrayList<>(),
                l = new ArrayList<>(), c = new ArrayList<>();
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
        int sz = o.size();
        float[] fo = new float[sz], fh = new float[sz], fl = new float[sz], fc = new float[sz];
        long[] fv = new long[sz], ft = new long[sz];
        for (int k = 0; k < sz; k++) {
            fo[k] = o.get(k); fh[k] = h.get(k); fl[k] = l.get(k); fc[k] = c.get(k);
            fv[k] = v.get(k); ft[k] = ts.get(k);
        }
        return new BarsArrays(fo, fh, fl, fc, fv, ft, sz);
    }

    /** Display name = script file name without path/extension. */
    private static String strategyNameOf(String path) {
        String p = path;
        int colon = p.indexOf(':');
        if (p.startsWith("res:") || p.startsWith("file:")) {
            p = p.substring(colon + 1);
        }
        int slash = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        String file = slash >= 0 ? p.substring(slash + 1) : p;
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    private static String readSource(String path) throws Exception {
        if (path.startsWith("res:")) {
            try (InputStream in = StrategyResearchDriver.class
                    .getResourceAsStream(path.substring(4))) {
                if (in == null) {
                    throw new IllegalStateException("Missing resource " + path);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        if (path.startsWith("file:")) {
            return Files.readString(Path.of(path.substring(5)));
        }
        return Files.readString(Path.of(path));
    }

    private static void printUsage() {
        System.out.println("""
                Usage: StrategyResearchDriver [-strategy path|res:...|file:...]
                       [-scrip ID[,ID2]] [-tf 1min,3min,...]
                       [-constraint "a<b"] [-wf train,test,step(months)]
                       [-out dir]""");
    }

}
