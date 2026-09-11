package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.metrics.MetricsCalculator;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

/**
 * Full parameter optimization for the ORB strategy on a single scrip.
 * Sweeps all 6 parameters, runs backtests in parallel, ranks by Sortino.
 */
public class OrbOptimizer {

    private static final float INITIAL_CAPITAL = 1_000_000f;
    private static final String SCRIP = "NSE:NIFTY 50";
    private static final Timeframe TF = Timeframe.FIVE_MINUTE;
    private static final int YEARS = 10;

    // Parameter grid
    private static final int[] ORB_END = {5, 10, 15, 20, 30, 45, 60};
    private static final int[] ENTRY_TYPE = {1, 2};
    private static final int[] CANDLE_TF = {3, 5, 10, 15};       // only used when entryType=2
    private static final int[] EXIT_MINS = {240, 270, 300, 315, 345, 375};
    private static final float[] STOP_MULT = {0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f};
    private static final float[] TARGET_MULT = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f};

    // Input name constants (must match Groovy script)
    private static final String K_ORB_END = "ORB End (mins)";
    private static final String K_ENTRY_TYPE = "Entry Type (1=BO 2=Close)";
    private static final String K_CANDLE_TF = "Entry Candle TF (mins)";
    private static final String K_EXIT_MINS = "Exit Time (mins)";
    private static final String K_STOP = "Stop (x range)";
    private static final String K_TARGET = "Target (x stop)";

    record ParamSet(int orbEnd, int entryType, int candleTf, int exitMins,
                    float stopMult, float targetMult) {
        Map<String, Number> toOverrides() {
            Map<String, Number> m = new LinkedHashMap<>();
            m.put(K_ORB_END, orbEnd);
            m.put(K_ENTRY_TYPE, entryType);
            m.put(K_CANDLE_TF, candleTf);
            m.put(K_EXIT_MINS, exitMins);
            m.put(K_STOP, stopMult);
            m.put(K_TARGET, targetMult);
            return m;
        }

        @Override
        public String toString() {
            return String.format("ORB=%d  Type=%d  CandleTF=%d  Exit=%d  Stop=%.2f  Target=%.2f",
                    orbEnd, entryType, candleTf, exitMins, stopMult, targetMult);
        }
    }

    record Result(ParamSet params, float sortino, float sharpe, float cagr,
                  float maxDd, float profitFactor, float winRate, int trades,
                  float calmar) implements Comparable<Result> {
        @Override
        public int compareTo(Result o) {
            return Float.compare(o.sortino, this.sortino); // descending
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  ORB Parameter Optimization — " + SCRIP.replace("NSE:", ""));
        System.out.println("═══════════════════════════════════════════════════════════");

        // Load data
        FileBarsRepository barsRepo = new FileBarsRepository();
        int totalBars = barsRepo.countBars(SCRIP, TF);
        int barsPerYear = 252 * 75;
        int tenYearBars = barsPerYear * YEARS;
        int offset = Math.max(0, totalBars - tenYearBars);
        Bars bars = barsRepo.loadRange(SCRIP, TF, offset, totalBars);
        System.out.printf("  Data: %d bars (last %d years, offset=%d)%n", bars.size(), YEARS, offset);

        // Compile script once
        String scriptSource = loadResource("strategies/ORB_Supertrend_Nifty50.groovy");
        Class<? extends Script> compiled = ScriptCompiler.compileClass(
                scriptSource, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);

        // Build parameter grid
        List<ParamSet> grid = buildGrid();
        System.out.printf("  Parameter combinations: %d%n", grid.size());

        // Run optimization
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.printf("  Threads: %d%n", threads);
        System.out.println("  Running...");
        System.out.println();

        BarsArrays arrays = bars.arrays();
        BacktestConfig configTemplate = BacktestConfig.builder()
                .timeframe(TF)
                .initialCapital(INITIAL_CAPITAL)
                .costPercent(0.01f)
                .slippagePercent(0.02f)
                .volumeParticipationPercent(100f)
                .offset(0)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Result>> futures = new ArrayList<>();
        AtomicInteger done = new AtomicInteger(0);
        int total = grid.size();
        long startMs = System.currentTimeMillis();

        for (ParamSet ps : grid) {
            futures.add(executor.submit(() -> {
                try {
                    Result r = runOne(compiled, arrays, SCRIP, ps, configTemplate);
                    int d = done.incrementAndGet();
                    if (d % 200 == 0 || d == total) {
                        long elapsed = System.currentTimeMillis() - startMs;
                        double pct = (double) d / total * 100;
                        double eta = (elapsed / (double) d) * (total - d) / 1000;
                        System.out.printf("  Progress: %d/%d (%.0f%%)  ETA: %.0fs%n",
                                d, total, pct, eta);
                    }
                    return r;
                } catch (Exception e) {
                    done.incrementAndGet();
                    return null;
                }
            }));
        }

        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) {
            Result r = f.get();
            if (r != null && r.trades > 0) results.add(r);
        }
        executor.shutdown();

        long totalMs = System.currentTimeMillis() - startMs;
        System.out.printf("%n  Completed %d runs in %.1f seconds%n", total, totalMs / 1000.0);

        // Sort by Sortino descending
        Collections.sort(results);

        // Print top 20
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  TOP 20 BY SORTINO");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-4s %-52s %8s %8s %8s %8s %8s %7s %6s%n",
                "#", "Parameters", "Sortino", "Sharpe", "CAGR%", "MaxDD%", "PF", "WinR%", "Trades");
        System.out.println("  " + "─".repeat(110));

        int rank = 0;
        for (Result r : results) {
            if (++rank > 20) break;
            System.out.printf("  %-4d %-52s %8.2f %8.2f %8.2f %8.2f %8.2f %7.1f %6d%n",
                    rank, r.params, r.sortino, r.sharpe, r.cagr, r.maxDd, r.profitFactor,
                    r.winRate, r.trades);
        }
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════════════════");

        // Also print bottom 5 for context
        System.out.println();
        System.out.println("  BOTTOM 5 BY SORTINO");
        System.out.println("  " + "─".repeat(110));
        int bottomStart = Math.max(0, results.size() - 5);
        for (int i = bottomStart; i < results.size(); i++) {
            Result r = results.get(i);
            System.out.printf("  %-4d %-52s %8.2f %8.2f %8.2f %8.2f %8.2f %7.1f %6d%n",
                    i + 1, r.params, r.sortino, r.sharpe, r.cagr, r.maxDd, r.profitFactor,
                    r.winRate, r.trades);
        }
        System.out.println();
    }

    private static Result runOne(Class<? extends Script> compiled, BarsArrays arrays,
                                  String scripId, ParamSet ps, BacktestConfig config) throws Exception {
        Script instance = ScriptCompiler.instantiate(compiled);
        if (!(instance instanceof TradingStrategyBase strategy)) {
            return null;
        }
        strategy.setInputOverrides(ps.toOverrides());

        BacktestEngine engine = new BacktestEngine(
                config.getInitialCapital(),
                config.getCostPercent(),
                config.getSlippagePercent(),
                config.getVolumeParticipationPercent(),
                null
        );
        ScripResult result = engine.run(scripId, arrays, strategy, Map.of(), Map.of());

        if (result.getTrades().isEmpty()) {
            return new Result(ps, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        BacktestReport report = MetricsCalculator.compute(List.of(result), config);
        return new Result(ps, report.getSortinoRatio(), report.getSharpeRatio(),
                report.getCagr(), report.getMaxDrawdown(), report.getProfitFactor(),
                report.getWinRate(), report.getTotalTrades(), report.getCalmarRatio());
    }

    private static List<ParamSet> buildGrid() {
        List<ParamSet> grid = new ArrayList<>();
        for (int orbEnd : ORB_END) {
            for (int et : ENTRY_TYPE) {
                int[] candleTfs = (et == 1) ? new int[]{5} : CANDLE_TF; // TF irrelevant for breakout
                for (int ctf : candleTfs) {
                    for (int exit : EXIT_MINS) {
                        for (float stop : STOP_MULT) {
                            for (float target : TARGET_MULT) {
                                grid.add(new ParamSet(orbEnd, et, ctf, exit, stop, target));
                            }
                        }
                    }
                }
            }
        }
        return grid;
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream is = OrbOptimizer.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
