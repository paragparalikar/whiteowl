package com.whiteowl.core.backtest.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Evaluates weekly systematic allocation strategies across the ETF universe
 * in {@code C:\trading\etfs} (Investing.com weekly OHLC exports).
 *
 * <h3>Strategies (Rs 40,000 deployed at every week's close)</h3>
 * <ol>
 *   <li><b>Equal Split</b> — divided equally across every ETF that reported a
 *       bar that week.</li>
 *   <li><b>Biggest Faller</b> — all into the worst weekly performer; if none
 *       fell, into the smallest gainer.</li>
 *   <li><b>Biggest Gainer</b> — all into the best weekly performer.</li>
 *   <li><b>Top-2 Momentum</b> — 50/50 into the two best weekly performers.</li>
 *   <li><b>4-Week Momentum</b> — all into the best trailing-4-week performer.</li>
 *   <li><b>Trend Filter 10W</b> — equal split across ETFs above their 10-week
 *       SMA; sits out entirely when nothing qualifies.</li>
 *   <li><b>Bottom-2 Dip</b> — 50/50 into the two worst weekly performers.</li>
 *   <li><b>Weighted Dips</b> — spread across all fallers proportional to the
 *       size of the fall; equal split when nothing fell.</li>
 *   <li><b>Below 52W High</b> — all into the ETF furthest below its trailing
 *       52-week high (or full available history if shorter).</li>
 *   <li><b>Inverse Volatility</b> — weighted by 1/(4-week return stdev).</li>
 *   <li><b>Vol-Adj Dip</b> — all into the worst weekly return scaled by the
 *       ETF's 4-week volatility.</li>
 *   <li><b>Rebalance to 1/N</b> — tops up the most underweight ETFs toward an
 *       equal-value target.</li>
 *   <li><b>Core-Satellite</b> — 75% equal split + 25% to the biggest faller.</li>
 * </ol>
 *
 * <h3>Assumptions</h3>
 * <ul>
 *   <li>Fractional units, no costs/slippage, fills at the week close.</li>
 *   <li>Deployment windows: each entry of {@link #WINDOW_WEEKS} is evaluated as
 *       the last N calendar weeks (windows larger than available history clamp
 *       to full history). Signal lookbacks may still use earlier history where
 *       it exists; an ETF is selectable only in weeks where it reports a bar.</li>
 *   <li>If a strategy's signal selects nothing that week (insufficient history,
 *       no qualifier), the week's Rs 40,000 is simply not deployed.</li>
 *   <li>Weekly portfolio returns for Sharpe/Sortino are computed on equity
 *       <i>before</i> the week's fresh Rs 40,000 injection (time-weighted).</li>
 *   <li>Risk-free rate: 6% annual. Annualization: sqrt(52).</li>
 *   <li>IRR column = CAGR on total invested capital; XIRR solves the exact-date
 *       IRR of the weekly outflows plus terminal equity.</li>
 * </ul>
 */
public final class EtfAllocationDriver {

    private static final Logger log = LoggerFactory.getLogger(EtfAllocationDriver.class);

    /**
     * Deployment windows to evaluate (last N calendar weeks each). 52w ~ 1y;
     * data spans ~5y (263w) so windows beyond that all clamp to full history.
     */
    private static final int[] WINDOW_WEEKS = {52, 104, 156, 208, 260, Integer.MAX_VALUE};

    private static final Path DATA_DIR    = Path.of("C:\\trading\\etfs");
    private static final Path OUTPUT_DIR  = Path.of("C:\\trading\\etf-results");
    private static final Path SUMMARY_CSV = OUTPUT_DIR.resolve("etf_allocation_summary.csv");
    private static final String FILE_SUFFIX = " Historical Data.csv";

    private static final double WEEKLY_ALLOCATION = 40_000d;
    private static final double RISK_FREE_ANNUAL  = 0.06d;
    private static final double RISK_FREE_WEEKLY  = RISK_FREE_ANNUAL / 52d;
    private static final double WEEKS_PER_YEAR    = 52d;
    private static final double DAYS_PER_YEAR     = 365d;

    private static final int MOMENTUM_LOOKBACK   = 4;    // weeks
    private static final int TREND_SMA_WEEKS     = 10;
    private static final int VOL_LOOKBACK        = 4;    // weeks of returns
    private static final int HIGH_LOOKBACK       = 52;   // weeks

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** One ETF's weekly close history, oldest to newest. */
    private record EtfSeries(String name, NavigableMap<LocalDate, Double> closes) {

        /** Last {@code max} closes at or before {@code date}, oldest to newest. */
        List<Double> closesThrough(LocalDate date, int max) {
            List<Double> all = new ArrayList<>(closes.headMap(date, true).values());
            return all.subList(Math.max(0, all.size() - max), all.size());
        }

        /** Week-over-week return at {@code date}, or {@code null} if no prior close. */
        Double weeklyReturn(LocalDate date) {
            List<Double> c = closesThrough(date, 2);
            return c.size() < 2 ? null : c.get(1) / c.get(0) - 1d;
        }

        /** Return over the trailing {@code weeks} weeks, or {@code null} if history is short. */
        Double trailingReturn(LocalDate date, int weeks) {
            List<Double> c = closesThrough(date, weeks + 1);
            return c.size() < weeks + 1 ? null : c.get(weeks) / c.get(0) - 1d;
        }

        /** {@code weeks}-week simple moving average, or {@code null} if history is short. */
        Double sma(LocalDate date, int weeks) {
            List<Double> c = closesThrough(date, weeks);
            if (c.size() < weeks) {
                return null;
            }
            return c.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        }

        /** Sample stdev of the last {@code weeks} weekly returns, or {@code null}. */
        Double weeklyVolatility(LocalDate date, int weeks) {
            List<Double> c = closesThrough(date, weeks + 1);
            if (c.size() < weeks + 1) {
                return null;
            }
            double mean = 0d;
            double[] r = new double[weeks];
            for (int i = 0; i < weeks; i++) {
                r[i] = c.get(i + 1) / c.get(i) - 1d;
                mean += r[i];
            }
            mean /= weeks;
            double var = 0d;
            for (double v : r) {
                var += (v - mean) * (v - mean);
            }
            return Math.sqrt(var / (weeks - 1));
        }

        /** Highest close over the trailing {@code weeks} weeks (fewer if history is short). */
        double periodHigh(LocalDate date, int weeks) {
            return closesThrough(date, weeks).stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0d);
        }
    }

    private record CashFlow(LocalDate date, double amount) {}

    private record Metrics(String name, double cagr, double xirr, double maxDrawdown,
                           double sharpe, double sortino, double calmar, double finalEquity,
                           double totalInvested) {}

    /** Everything a strategy needs to decide one deployment's allocation. */
    private record WeekContext(LocalDate date,
                               Map<String, EtfSeries> reporting,   // ETFs with a bar this week
                               Map<String, Double> marketValues,   // held ETF -> current value
                               double portfolioValue,
                               double deployAmount) {}             // capital to place this event

    /** Allocation decision for one week: ETF name -> fraction of the week's capital. */
    private interface AllocationStrategy {
        String name();

        /**
         * @return name -> weight (must sum to 1.0, may only reference ctx.reporting());
         *         empty map = do not invest this week
         */
        Map<String, Double> weights(WeekContext ctx);
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        List<EtfSeries> universe = loadUniverse();
        log.info("Loaded {} ETFs from {}", universe.size(), DATA_DIR);

        List<LocalDate> fullCalendar = buildCalendar(universe);

        List<AllocationStrategy> strategies = List.of(
                new EqualSplit(), new BiggestFaller(), new BiggestGainer(),
                new Top2Momentum(), new Momentum4W(), new TrendFilter(),
                new Bottom2Dip(), new WeightedDips(), new BelowHigh52(),
                new InverseVolatility(), new VolAdjDip(), new RebalanceToEqual(),
                new CoreSatellite());

        // strategy name -> window weeks -> metrics
        Map<String, Map<Integer, Metrics>> byStrategy = new LinkedHashMap<>();
        List<Integer> effectiveWindows = new ArrayList<>();

        for (int window : WINDOW_WEEKS) {
            int start = Math.max(0, fullCalendar.size() - window);
            int weeks = fullCalendar.size() - start;
            if (effectiveWindows.contains(weeks)) {
                continue;                               // window clamps to one already run
            }
            effectiveWindows.add(weeks);
            List<LocalDate> calendar = fullCalendar.subList(start, fullCalendar.size());
            log.info("Window {}w: {} to {}", weeks, calendar.getFirst(), calendar.getLast());

            List<Metrics> results = new ArrayList<>();
            for (AllocationStrategy strategy : strategies) {
                Metrics m = simulate(strategy, universe, calendar, i -> true);
                results.add(m);
                byStrategy.computeIfAbsent(m.name(), k -> new LinkedHashMap<>())
                        .put(weeks, m);
            }
            writeCsv(results, OUTPUT_DIR.resolve("etf_allocation_metrics_" + weeks + "w.csv"));
        }

        writeSummary(byStrategy, effectiveWindows);
        runFrequencyStudy(universe, fullCalendar, effectiveWindows);
        runVolLookbackStudy(universe, fullCalendar, effectiveWindows);
        for (Map.Entry<String, Map<Integer, Metrics>> e : byStrategy.entrySet()) {
            Map<Integer, Metrics> m = e.getValue();
            double meanIrr = m.values().stream().mapToDouble(Metrics::cagr).average().orElse(0);
            double minIrr = m.values().stream().mapToDouble(Metrics::cagr).min().orElse(0);
            double worstDd = m.values().stream().mapToDouble(Metrics::maxDrawdown).min().orElse(0);
            log.info(String.format("%-20s meanIRR=%6.2f%%  minIRR=%6.2f%%  worstDD=%6.2f%%",
                    e.getKey(), meanIrr * 100, minIrr * 100, worstDd * 100));
        }
    }

    /**
     * Deployment-frequency study for Inverse Volatility: weekly vs biweekly vs
     * monthly placements of the same Rs 40,000/week accrual, across all windows.
     */
    private static void runFrequencyStudy(List<EtfSeries> universe, List<LocalDate> fullCalendar,
                                          List<Integer> windows) throws IOException {
        AllocationStrategy strategy = new InverseVolatility();
        List<String> freqs = List.of("weekly", "biweekly", "monthly");
        Path out = OUTPUT_DIR.resolve("etf_invvol_frequency.csv");
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("Window,Frequency,IRR,XIRR,Max DrawDown,Sharpe,Sortino,Calmar,"
                    + "Final Equity,Total Invested\n");
            for (int weeks : windows) {
                List<LocalDate> calendar =
                        fullCalendar.subList(fullCalendar.size() - weeks, fullCalendar.size());
                for (String freq : freqs) {
                    Metrics m = simulate(strategy, universe, calendar, cadence(freq, calendar));
                    w.write(String.format("%dw,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f%n",
                            weeks, freq, m.cagr() * 100, m.xirr() * 100, m.maxDrawdown() * 100,
                            m.sharpe(), m.sortino(), m.calmar(), m.finalEquity(),
                            m.totalInvested()));
                }
            }
        }
        log.info("Frequency study written to {}", out);
    }

    /**
     * Volatility-lookback sweep for Inverse Volatility: 2..10 weeks of weekly
     * returns in the stdev estimate, weekly cadence, across all windows.
     */
    private static void runVolLookbackStudy(List<EtfSeries> universe, List<LocalDate> fullCalendar,
                                            List<Integer> windows) throws IOException {
        Path out = OUTPUT_DIR.resolve("etf_invvol_lookback.csv");
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("Lookback,Window,IRR,XIRR,Max DrawDown,Sharpe,Sortino,Calmar,"
                    + "Final Equity,Total Invested\n");
            for (int lookback = 2; lookback <= 10; lookback++) {
                for (int weeks : windows) {
                    List<LocalDate> calendar =
                            fullCalendar.subList(fullCalendar.size() - weeks, fullCalendar.size());
                    Metrics m = simulate(new InverseVolatility(lookback), universe, calendar,
                            i -> true);
                    w.write(String.format("%dw,%dw,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f%n",
                            lookback, weeks, m.cagr() * 100, m.xirr() * 100,
                            m.maxDrawdown() * 100, m.sharpe(), m.sortino(), m.calmar(),
                            m.finalEquity(), m.totalInvested()));
                }
            }
        }
        log.info("Lookback sweep written to {}", out);
    }

    /** Per-strategy robustness rollup across all windows. */
    private static void writeSummary(Map<String, Map<Integer, Metrics>> byStrategy,
                                     List<Integer> windows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(SUMMARY_CSV)) {
            StringBuilder header = new StringBuilder("Name,Mean IRR,Min IRR,Max IRR,Worst MaxDD,"
                    + "Mean Sharpe,Mean Sortino,Mean Calmar");
            for (int weeks : windows) {
                header.append(",IRR ").append(weeks).append('w');
            }
            w.write(header + "\n");
            for (Map.Entry<String, Map<Integer, Metrics>> e : byStrategy.entrySet()) {
                List<Metrics> ms = new ArrayList<>(e.getValue().values());
                double meanIrr = ms.stream().mapToDouble(Metrics::cagr).average().orElse(0);
                double minIrr = ms.stream().mapToDouble(Metrics::cagr).min().orElse(0);
                double maxIrr = ms.stream().mapToDouble(Metrics::cagr).max().orElse(0);
                double worstDd = ms.stream().mapToDouble(Metrics::maxDrawdown).min().orElse(0);
                double meanSharpe = ms.stream().mapToDouble(Metrics::sharpe).average().orElse(0);
                double meanSortino = ms.stream().mapToDouble(Metrics::sortino).average().orElse(0);
                double meanCalmar = ms.stream().mapToDouble(Metrics::calmar).average().orElse(0);
                StringBuilder row = new StringBuilder(String.format(
                        "%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                        e.getKey(), meanIrr * 100, minIrr * 100, maxIrr * 100, worstDd * 100,
                        meanSharpe, meanSortino, meanCalmar));
                for (int weeks : windows) {
                    row.append(String.format(",%.4f",
                            e.getValue().get(weeks).cagr() * 100));
                }
                w.write(row + "\n");
            }
        }
        log.info("Summary written to {}", SUMMARY_CSV);
    }

    // ── Strategies ────────────────────────────────────────────────────

    /** Rs 40,000 split equally across every ETF reporting a bar this week. */
    private static final class EqualSplit implements AllocationStrategy {
        @Override public String name() { return "Equal Split"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            return equalWeights(ctx.reporting().keySet().stream().toList());
        }
    }

    /** Buys the worst performer of the week; if none fell, the smallest gainer. */
    private static final class BiggestFaller implements AllocationStrategy {
        @Override public String name() { return "Biggest Faller"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            String pick = extremeByWeeklyReturn(ctx, false);
            return pick == null ? Map.of() : Map.of(pick, 1d);
        }
    }

    /** Buys the best performer of the week. */
    private static final class BiggestGainer implements AllocationStrategy {
        @Override public String name() { return "Biggest Gainer"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            String pick = extremeByWeeklyReturn(ctx, true);
            return pick == null ? Map.of() : Map.of(pick, 1d);
        }
    }

    /** 50/50 into the two best weekly performers. */
    private static final class Top2Momentum implements AllocationStrategy {
        @Override public String name() { return "Top-2 Momentum"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            List<String> top = rankedByWeeklyReturn(ctx, true);
            return equalWeights(top.subList(0, Math.min(2, top.size())));
        }
    }

    /** All into the best trailing-4-week performer. */
    private static final class Momentum4W implements AllocationStrategy {
        @Override public String name() { return "4-Week Momentum"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            String pick = ctx.reporting().entrySet().stream()
                    .filter(e -> e.getValue().trailingReturn(ctx.date(), MOMENTUM_LOOKBACK) != null)
                    .max(Comparator.comparingDouble(
                            e -> e.getValue().trailingReturn(ctx.date(), MOMENTUM_LOOKBACK)))
                    .map(Map.Entry::getKey).orElse(null);
            return pick == null ? Map.of() : Map.of(pick, 1d);
        }
    }

    /** Equal split across ETFs trading above their 10-week SMA; else sits out. */
    private static final class TrendFilter implements AllocationStrategy {
        @Override public String name() { return "Trend Filter 10W"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            List<String> above = ctx.reporting().entrySet().stream()
                    .filter(e -> {
                        Double sma = e.getValue().sma(ctx.date(), TREND_SMA_WEEKS);
                        Double close = e.getValue().closes().get(ctx.date());
                        return sma != null && close > sma;
                    })
                    .map(Map.Entry::getKey).toList();
            return equalWeights(above);
        }
    }

    /** 50/50 into the two worst weekly performers. */
    private static final class Bottom2Dip implements AllocationStrategy {
        @Override public String name() { return "Bottom-2 Dip"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            List<String> bottom = rankedByWeeklyReturn(ctx, false);
            return equalWeights(bottom.subList(0, Math.min(2, bottom.size())));
        }
    }

    /** Spread across all fallers proportional to the fall; equal split if none fell. */
    private static final class WeightedDips implements AllocationStrategy {
        @Override public String name() { return "Weighted Dips"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            Map<String, Double> fallers = new LinkedHashMap<>();
            for (Map.Entry<String, EtfSeries> e : ctx.reporting().entrySet()) {
                Double r = e.getValue().weeklyReturn(ctx.date());
                if (r != null && r < 0) {
                    fallers.put(e.getKey(), -r);
                }
            }
            if (fallers.isEmpty()) {
                return equalWeights(ctx.reporting().keySet().stream().toList());
            }
            double total = fallers.values().stream().mapToDouble(Double::doubleValue).sum();
            Map<String, Double> w = new LinkedHashMap<>();
            fallers.forEach((name, drop) -> w.put(name, drop / total));
            return w;
        }
    }

    /** All into the ETF furthest below its trailing 52-week high. */
    private static final class BelowHigh52 implements AllocationStrategy {
        @Override public String name() { return "Below 52W High"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            String pick = ctx.reporting().entrySet().stream()
                    .min(Comparator.comparingDouble(e -> {
                        double high = e.getValue().periodHigh(ctx.date(), HIGH_LOOKBACK);
                        double close = e.getValue().closes().get(ctx.date());
                        return high > 0 ? close / high : 1d;
                    }))
                    .map(Map.Entry::getKey).orElse(null);
            return pick == null ? Map.of() : Map.of(pick, 1d);
        }
    }

    /** Weighted by 1/(N-week weekly-return stdev) — steadier ETFs get more. */
    private static final class InverseVolatility implements AllocationStrategy {
        private final int lookback;

        InverseVolatility() { this(VOL_LOOKBACK); }
        InverseVolatility(int lookback) { this.lookback = lookback; }

        @Override public String name() { return "Inverse Volatility"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            Map<String, Double> invVol = new LinkedHashMap<>();
            for (Map.Entry<String, EtfSeries> e : ctx.reporting().entrySet()) {
                Double vol = e.getValue().weeklyVolatility(ctx.date(), lookback);
                if (vol != null && vol > 0) {
                    invVol.put(e.getKey(), 1d / vol);
                }
            }
            if (invVol.isEmpty()) {
                return Map.of();
            }
            double total = invVol.values().stream().mapToDouble(Double::doubleValue).sum();
            Map<String, Double> w = new LinkedHashMap<>();
            invVol.forEach((name, iv) -> w.put(name, iv / total));
            return w;
        }
    }

    /** All into the worst weekly return scaled by the ETF's 4-week volatility. */
    private static final class VolAdjDip implements AllocationStrategy {
        @Override public String name() { return "Vol-Adj Dip"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            String pick = ctx.reporting().entrySet().stream()
                    .filter(e -> {
                        Double vol = e.getValue().weeklyVolatility(ctx.date(), VOL_LOOKBACK);
                        return vol != null && vol > 0
                                && e.getValue().weeklyReturn(ctx.date()) != null;
                    })
                    .min(Comparator.comparingDouble(e ->
                            e.getValue().weeklyReturn(ctx.date())
                                    / e.getValue().weeklyVolatility(ctx.date(), VOL_LOOKBACK)))
                    .map(Map.Entry::getKey).orElse(null);
            return pick == null ? Map.of() : Map.of(pick, 1d);
        }
    }

    /**
     * Directs the week's capital to the most underweight ETFs so that holdings
     * drift back toward equal value: each reporting ETF's target is
     * (portfolio value + inflow) / N; buys the shortfall, normalized.
     */
    private static final class RebalanceToEqual implements AllocationStrategy {
        @Override public String name() { return "Rebalance to 1/N"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            int n = ctx.reporting().size();
            double target = (ctx.portfolioValue() + ctx.deployAmount()) / n;
            Map<String, Double> deficit = new LinkedHashMap<>();
            double total = 0d;
            for (String name : ctx.reporting().keySet()) {
                double d = Math.max(0d, target - ctx.marketValues().getOrDefault(name, 0d));
                deficit.put(name, d);
                total += d;
            }
            if (total <= 0) {
                return equalWeights(ctx.reporting().keySet().stream().toList());
            }
            Map<String, Double> w = new LinkedHashMap<>();
            double t = total;
            deficit.forEach((name, d) -> w.put(name, d / t));
            return w;
        }
    }

    /** 75% equal split (core) + 25% to the biggest faller (satellite). */
    private static final class CoreSatellite implements AllocationStrategy {
        @Override public String name() { return "Core-Satellite"; }

        @Override
        public Map<String, Double> weights(WeekContext ctx) {
            Map<String, Double> w = equalWeights(ctx.reporting().keySet().stream().toList());
            w.replaceAll((k, v) -> v * 0.75d);
            String pick = extremeByWeeklyReturn(ctx, false);
            if (pick != null) {
                w.merge(pick, 0.25d, Double::sum);
            } else {
                w.replaceAll((k, v) -> v / 0.75d);
            }
            return w;
        }
    }

    // ── Strategy helpers ──────────────────────────────────────────────

    private static Map<String, Double> equalWeights(List<String> names) {
        if (names.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> w = new LinkedHashMap<>();
        double each = 1d / names.size();
        names.forEach(n -> w.put(n, each));
        return w;
    }

    /** Reporting ETFs with a computable weekly return, sorted best-first (or worst-first). */
    private static List<String> rankedByWeeklyReturn(WeekContext ctx, boolean bestFirst) {
        Comparator<Map.Entry<String, EtfSeries>> cmp =
                Comparator.comparingDouble(e -> e.getValue().weeklyReturn(ctx.date()));
        return ctx.reporting().entrySet().stream()
                .filter(e -> e.getValue().weeklyReturn(ctx.date()) != null)
                .sorted(bestFirst ? cmp.reversed() : cmp)
                .map(Map.Entry::getKey).toList();
    }

    private static String extremeByWeeklyReturn(WeekContext ctx, boolean max) {
        List<String> ranked = rankedByWeeklyReturn(ctx, max);
        return ranked.isEmpty() ? null : ranked.getFirst();
    }

    // ── Simulation ────────────────────────────────────────────────────

    private static Metrics simulate(AllocationStrategy strategy, List<EtfSeries> universe,
                                    List<LocalDate> calendar,
                                    java.util.function.IntPredicate shouldDeploy) {
        Map<String, Double> units = new HashMap<>();          // ETF -> units held
        List<CashFlow> cashFlows = new ArrayList<>();
        List<Double> weeklyReturns = new ArrayList<>();       // time-weighted, pre-flow
        List<Double> equityCurve = new ArrayList<>();
        double prevEquity = 0d;
        double pending = 0d;                                  // accrued undeployed capital

        for (int i = 0; i < calendar.size(); i++) {
            LocalDate date = calendar.get(i);
            pending += WEEKLY_ALLOCATION;
            // Mark existing holdings to this week's closes (carry-forward for
            // ETFs that did not print a bar this week).
            Map<String, Double> marketValues = new HashMap<>();
            double equityBeforeFlow = 0d;
            for (EtfSeries etf : universe) {
                Double held = units.get(etf.name());
                if (held == null || held == 0d) {
                    continue;
                }
                Map.Entry<LocalDate, Double> close = etf.closes().floorEntry(date);
                if (close != null) {
                    double value = held * close.getValue();
                    marketValues.put(etf.name(), value);
                    equityBeforeFlow += value;
                }
            }

            if (prevEquity > 0) {
                weeklyReturns.add(equityBeforeFlow / prevEquity - 1d);
            }

            Map<String, EtfSeries> reporting = new LinkedHashMap<>();
            for (EtfSeries etf : universe) {
                if (etf.closes().containsKey(date)) {
                    reporting.put(etf.name(), etf);
                }
            }

            double equity = equityBeforeFlow;
            if (!reporting.isEmpty() && shouldDeploy.test(i)) {
                Map<String, Double> weights = strategy.weights(
                        new WeekContext(date, reporting, marketValues, equityBeforeFlow, pending));
                if (!weights.isEmpty()) {
                    for (Map.Entry<String, Double> w : weights.entrySet()) {
                        double amount = pending * w.getValue();
                        double price = reporting.get(w.getKey()).closes().get(date);
                        units.merge(w.getKey(), amount / price, Double::sum);
                    }
                    cashFlows.add(new CashFlow(date, -pending));
                    equity += pending;
                    pending = 0d;
                }
            }

            equityCurve.add(equity);
            prevEquity = equity;
        }

        double finalEquity = prevEquity;
        LocalDate first = cashFlows.isEmpty() ? calendar.getFirst() : cashFlows.getFirst().date();
        LocalDate last = calendar.getLast();
        cashFlows.add(new CashFlow(last, finalEquity));       // terminal liquidation

        double totalInvested = -cashFlows.stream()
                .filter(cf -> cf.amount() < 0).mapToDouble(CashFlow::amount).sum();
        double years = Math.max(ChronoUnit.DAYS.between(first, last), 1) / DAYS_PER_YEAR;
        double cagr = totalInvested > 0 && finalEquity > 0
                ? Math.pow(finalEquity / totalInvested, 1d / years) - 1d : 0d;
        double xirr = solveXirr(cashFlows);
        double maxDd = maxDrawdown(equityCurve);
        double sharpe = sharpe(weeklyReturns);
        double sortino = sortino(weeklyReturns);
        double calmar = maxDd < 0 ? cagr / -maxDd : 0d;

        return new Metrics(strategy.name(), cagr, xirr, maxDd, sharpe, sortino, calmar,
                finalEquity, totalInvested);
    }

    /**
     * Cadence for deployments: capital accrues Rs 40,000/week and is placed in
     * a lump at each event. Any residual pending is flushed on the final week.
     */
    private static java.util.function.IntPredicate cadence(String freq, List<LocalDate> calendar) {
        int last = calendar.size() - 1;
        return switch (freq) {
            case "weekly"   -> i -> true;
            case "biweekly" -> i -> i % 2 == 0 || i == last;
            case "monthly"  -> i -> i == last
                    || !java.time.YearMonth.from(calendar.get(i))
                            .equals(java.time.YearMonth.from(calendar.get(i + 1)));
            default -> throw new IllegalArgumentException("unknown frequency " + freq);
        };
    }

    // ── Metrics ───────────────────────────────────────────────────────

    /** Peak-to-trough drawdown of the weekly equity curve (negative number). */
    private static double maxDrawdown(List<Double> equity) {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDd = 0d;
        for (double v : equity) {
            peak = Math.max(peak, v);
            if (peak > 0) {
                maxDd = Math.min(maxDd, v / peak - 1d);
            }
        }
        return maxDd;
    }

    /** Annualized Sharpe on weekly time-weighted excess returns. */
    private static double sharpe(List<Double> weeklyReturns) {
        if (weeklyReturns.size() < 2) return 0d;
        double mean = weeklyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double excess = mean - RISK_FREE_WEEKLY;
        double var = 0d;
        for (double r : weeklyReturns) {
            var += (r - mean) * (r - mean);
        }
        double std = Math.sqrt(var / (weeklyReturns.size() - 1));
        return std == 0 ? 0d : excess / std * Math.sqrt(WEEKS_PER_YEAR);
    }

    /** Annualized Sortino; downside deviation vs the weekly risk-free hurdle, all periods. */
    private static double sortino(List<Double> weeklyReturns) {
        if (weeklyReturns.isEmpty()) return 0d;
        double meanExcess = 0d;
        double downsideSq = 0d;
        for (double r : weeklyReturns) {
            double excess = r - RISK_FREE_WEEKLY;
            meanExcess += excess;
            if (excess < 0) {
                downsideSq += excess * excess;
            }
        }
        meanExcess /= weeklyReturns.size();
        double downsideDev = Math.sqrt(downsideSq / weeklyReturns.size());
        return downsideDev == 0 ? 0d : meanExcess / downsideDev * Math.sqrt(WEEKS_PER_YEAR);
    }

    /**
     * Exact-date IRR of the cash-flow stream via bisection. Flows are all
     * outflows followed by a single terminal inflow, so NPV is monotonic in r
     * and a unique root exists in (-1, +inf).
     */
    private static double solveXirr(List<CashFlow> flows) {
        LocalDate t0 = flows.getFirst().date();
        double[] years = flows.stream()
                .mapToDouble(cf -> ChronoUnit.DAYS.between(t0, cf.date()) / DAYS_PER_YEAR)
                .toArray();
        double[] amounts = flows.stream().mapToDouble(CashFlow::amount).toArray();

        java.util.function.DoubleUnaryOperator npv = r -> {
            double sum = 0d;
            for (int i = 0; i < amounts.length; i++) {
                sum += amounts[i] / Math.pow(1d + r, years[i]);
            }
            return sum;
        };

        double lo = -0.9999d;
        double hi = 1d;
        while (npv.applyAsDouble(hi) > 0 && hi < 1e6) {
            hi *= 2d;
        }
        if (npv.applyAsDouble(lo) <= 0 || npv.applyAsDouble(hi) >= 0) {
            return Double.NaN;                                // no sign change => no root
        }
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2d;
            if (npv.applyAsDouble(mid) > 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2d;
    }

    // ── Data loading ──────────────────────────────────────────────────

    private static List<EtfSeries> loadUniverse() throws IOException {
        try (Stream<Path> files = Files.list(DATA_DIR)) {
            return files.filter(f -> f.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted()
                    .map(EtfAllocationDriver::loadEtf)
                    .toList();
        }
    }

    private static EtfSeries loadEtf(Path file) {
        String fileName = file.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - FILE_SUFFIX.length());
        NavigableMap<LocalDate, Double> closes = new TreeMap<>();
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.replace("﻿", "").trim();
                if (line.isEmpty() || line.startsWith("\"Date\"")) {
                    continue;
                }
                // Investing.com format: "dd-MM-yyyy","Price","Open","High","Low","Vol.","Change %"
                String[] f = line.replaceAll("^\"|\"$", "").split("\",\"");
                LocalDate date = LocalDate.parse(f[0], DATE_FMT);
                double close = Double.parseDouble(f[1].replace(",", ""));
                closes.put(date, close);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + file, e);
        }
        return new EtfSeries(name, closes);
    }

    private static List<LocalDate> buildCalendar(List<EtfSeries> universe) {
        return universe.stream()
                .flatMap(etf -> etf.closes().keySet().stream())
                .distinct().sorted().toList();
    }

    private static void writeCsv(List<Metrics> results, Path outputCsv) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outputCsv)) {
            w.write("Name,IRR,XIRR,Max DrawDown,Sharpe,Sortino,Calmar,Final Equity\n");
            for (Metrics m : results) {
                w.write(String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                        m.name(), m.cagr() * 100, m.xirr() * 100, m.maxDrawdown() * 100,
                        m.sharpe(), m.sortino(), m.calmar(), m.finalEquity()));
            }
        }
        log.info("Metrics written to {}", outputCsv);
    }
}
