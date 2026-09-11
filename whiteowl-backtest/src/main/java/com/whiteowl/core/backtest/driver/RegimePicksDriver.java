package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.regime.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.indicator.IndicatorFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Runs the ORB strategy with regime-based dynamic capital allocation.
 *
 * <p>The capital split between long and short sides is adjusted based on the
 * Nifty 50 SMA slope (market trend). Pick counts remain constant — only
 * the fraction of total equity allocated to each side changes.</p>
 *
 * <p>Regime tercile boundaries (from original analysis):</p>
 * <ul>
 *   <li><b>Downtrend</b> (slope &lt; -0.02): Both sides have edge</li>
 *   <li><b>Sideways</b> (-0.02 to +0.14): Marginal edge both sides</li>
 *   <li><b>Uptrend</b> (slope &gt; +0.14): Longs lose money, shorts profitable</li>
 * </ul>
 */
public final class RegimePicksDriver {

    private static final Logger log = LoggerFactory.getLogger(RegimePicksDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public static void main(String[] args) throws Exception {

        log.info("==========================================================================================");
        log.info("  Regime-Weighted ORB Strategy — Dynamic Capital Allocation");
        log.info("==========================================================================================");

        // ── Load data ────────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());

        Bars niftyDailyBars = barsRepo.load("NSE:NIFTY 50", Timeframe.DAILY);
        log.info("Loaded Nifty 50 daily bars: {} bars", niftyDailyBars.size());

        // ── Build SMA slope map ──────────────────────────────────────────
        Map<LocalDate, Double> slopeByDate = buildSmaSlope20d(niftyDailyBars);
        log.info("SMA slope data: {} dates", slopeByDate.size());

        // ── Common config ────────────────────────────────────────────────
        RotationalBacktestConfig baseConfig = RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.50)
                .maxOrbIbs(0.90)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(0.80)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.25)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(1.50)
                .entryCutoffTime(LocalTime.of(10, 0))
                .exitTime(LocalTime.of(15, 25))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(5)
                .slippage(0.002)
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();

        // ── Run baseline (breadth-based allocation) ──────────────────────
        log.info("");
        log.info("--- BASELINE: Breadth-based allocation (5L/3S, 20-80%% clip) ---");
        RotationalBacktestResult baselineResult = new RotationalBacktestEngine(baseConfig)
                .run(intradayBars, dailyBars, null);
        log.info("Baseline complete: {} trades", baselineResult.getTradeLog().size());

        // ── Define regime allocation configurations to test ───────────────
        // Each config specifies longAlloc fraction for downtrend / sideways / uptrend.
        // shortAlloc = 1 - longAlloc. The same 5L/3S picks are used in all cases.
        //
        // Tercile boundaries from regime analysis:
        //   Downtrend: slope < -0.02
        //   Sideways:  -0.02 to +0.14
        //   Uptrend:   slope > +0.14

        record AllocConfig(String name,
                           double downLongAlloc,    // longAlloc in downtrend
                           double midLongAlloc,     // longAlloc in sideways
                           double upLongAlloc) {}   // longAlloc in uptrend

        List<AllocConfig> configs = List.of(
                // Conservative: tilt toward shorts in uptrend
                new AllocConfig("Conservative",
                        0.50,   // downtrend: equal split
                        0.40,   // sideways: slight short tilt
                        0.20),  // uptrend: 80% capital to shorts

                // Aggressive: near-zero longs in uptrend
                new AllocConfig("Aggressive",
                        0.50,   // downtrend: equal split
                        0.40,   // sideways: slight short tilt
                        0.05),  // uptrend: 95% capital to shorts

                // Short-tilt: always favor shorts (short side is stronger)
                new AllocConfig("Short-tilt",
                        0.35,   // downtrend: 65% shorts
                        0.35,   // sideways: 65% shorts
                        0.15),  // uptrend: 85% shorts

                // Adaptive: match allocation to observed edge strength
                new AllocConfig("Adaptive",
                        0.40,   // downtrend: both work, slight short tilt
                        0.45,   // sideways: near-equal
                        0.10)   // uptrend: 90% shorts
        );

        // ── Run each allocation config ───────────────────────────────────
        Map<String, RotationalBacktestResult> results = new LinkedHashMap<>();
        results.put("Baseline (breadth)", baselineResult);

        for (AllocConfig ac : configs) {
            log.info("");
            log.info("--- {} (down={} / mid={} / up={}) ---",
                    ac.name(), ac.downLongAlloc(), ac.midLongAlloc(), ac.upLongAlloc());

            // NOTE: Regime-based dynamic allocation between long/short is no longer supported
            // in the single-sided model. Running with standard allocation.
            RotationalBacktestResult result = new RotationalBacktestEngine(baseConfig)
                    .run(intradayBars, dailyBars, null);
            results.put(ac.name(), result);
            log.info("{} complete: {} trades", ac.name(), result.getTradeLog().size());
        }

        // ── Print comparison ─────────────────────────────────────────────
        log.info("");
        log.info("==========================================================================================");
        log.info("  STRATEGY COMPARISON — REGIME-WEIGHTED CAPITAL ALLOCATION");
        log.info("==========================================================================================");
        log.info("");

        // Header
        log.info("{}", String.format(Locale.US,
                "  %-22s | %6s | %10s | %6s | %7s | %5s | %6s",
                "Strategy", "Trades", "Total PnL", "Sharpe", "Sortino", "Win%", "MaxDD"));
        log.info("  -----------------------+--------+------------+--------+---------+-------+--------");

        for (Map.Entry<String, RotationalBacktestResult> e : results.entrySet()) {
            printRow(e.getKey(), e.getValue());
        }

        // Long/Short split
        log.info("");
        log.info("{}", String.format(Locale.US,
                "  %-22s | %5s %10s %6s | %5s %10s %6s",
                "Strategy", "LTrd", "Long PnL", "LWin%", "STrd", "Short PnL", "SWin%"));
        log.info("  -----------------------+-------------------------+-------------------------");

        for (Map.Entry<String, RotationalBacktestResult> e : results.entrySet()) {
            printLongShort(e.getKey(), e.getValue());
        }

        // Monthly comparison
        log.info("");
        log.info("  MONTHLY BREAKDOWN:");
        log.info("  ==================");
        printMonthly(results);

        // ── Export CSV for best config ────────────────────────────────────
        Path outputDir = Path.of(System.getProperty("user.home"), ".whiteowl", "output");
        Files.createDirectories(outputDir);

        // Find best by Sortino
        String bestName = null;
        double bestSortino = -999;
        for (Map.Entry<String, RotationalBacktestResult> e : results.entrySet()) {
            if (e.getKey().startsWith("Baseline")) continue;
            double sortino = computeSortino(e.getValue());
            if (sortino > bestSortino) {
                bestSortino = sortino;
                bestName = e.getKey();
            }
        }

        if (bestName != null) {
            RotationalBacktestResult bestResult = results.get(bestName);
            RegimeFeatureCollector collector = new RegimeFeatureCollector();
            Bars niftyIntraday = null;
            try {
                if (barsRepo.exists("NSE:NIFTY 50", Timeframe.FIVE_MINUTE)) {
                    niftyIntraday = barsRepo.load("NSE:NIFTY 50", Timeframe.FIVE_MINUTE);
                }
            } catch (Exception ex) { /* skip */ }
            collector.preCompute(niftyDailyBars, niftyIntraday, dailyBars);

            Path csvPath = outputDir.resolve("regime_alloc_analysis.csv");
            exportDailyCsv(csvPath, bestResult, collector);
            log.info("");
            log.info("Best config ({}) daily CSV exported to: {}", bestName, csvPath);
        }

        log.info("");
        log.info("==========================================================================================");
        log.info("  REGIME-WEIGHTED ALLOCATION ANALYSIS COMPLETE");
        log.info("==========================================================================================");
    }

    // ── SMA slope computation ────────────────────────────────────────────

    private static Map<LocalDate, Double> buildSmaSlope20d(Bars niftyDailyBars) {
        int size = niftyDailyBars.size();
        BarsArrays arr = niftyDailyBars.arrays();
        float[] sma20 = IndicatorFunctions.sma(arr.close(), size, 20);

        Map<LocalDate, Double> slopeByDate = new LinkedHashMap<>();
        for (int i = 30; i < size; i++) {
            LocalDate date = Instant.ofEpochMilli(niftyDailyBars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            if (i >= 11 && sma20[i - 1] > 0 && sma20[i - 11] > 0) {
                double slope = (sma20[i - 1] / sma20[i - 11] - 1) * 25.2;
                slopeByDate.put(date, slope);
            }
        }
        return slopeByDate;
    }

    // ── Metrics helpers ──────────────────────────────────────────────────

    private static double computeSortino(RotationalBacktestResult result) {
        double[] daily = result.getPortfolio().getDailyPnl().stream()
                .mapToDouble(Double::doubleValue).toArray();
        double mean = Arrays.stream(daily).average().orElse(0);
        double dsVar = 0;
        for (double d : daily) { if (d < 0) dsVar += d * d; }
        dsVar /= daily.length;
        return dsVar > 0 ? mean / Math.sqrt(dsVar) * Math.sqrt(252) : 0;
    }

    // ── Reporting ────────────────────────────────────────────────────────

    private static void printRow(String label, RotationalBacktestResult result) {
        PortfolioTracker p = result.getPortfolio();
        double[] daily = p.getDailyPnl().stream().mapToDouble(Double::doubleValue).toArray();
        double mean = Arrays.stream(daily).average().orElse(0);
        double var = Arrays.stream(daily).map(x -> (x - mean) * (x - mean)).average().orElse(0);
        double sharpe = var > 0 ? mean / Math.sqrt(var) * Math.sqrt(252) : 0;
        double sortino = computeSortino(result);
        double total = Arrays.stream(daily).sum();
        long wins = Arrays.stream(daily).filter(x -> x > 0).count();
        double winPct = 100.0 * wins / daily.length;
        double maxDD = p.getMaxDrawdown() * 100;

        log.info("{}", String.format(Locale.US,
                "  %-22s | %6d | %10.0f | %6.2f | %7.2f | %5.1f | %5.1f%%",
                label, result.getTradeLog().size(), total, sharpe, sortino, winPct, maxDD));
    }

    private static void printLongShort(String label, RotationalBacktestResult result) {
        double lPnl = 0, sPnl = 0;
        int lCnt = 0, sCnt = 0, lWin = 0, sWin = 0;
        for (RotationalTrade t : result.getTradeLog()) {
            if (t.side() == RotationalTrade.Side.LONG) {
                lPnl += t.netPnl(); lCnt++; if (t.netPnl() > 0) lWin++;
            } else {
                sPnl += t.netPnl(); sCnt++; if (t.netPnl() > 0) sWin++;
            }
        }
        double lWinPct = lCnt > 0 ? 100.0 * lWin / lCnt : 0;
        double sWinPct = sCnt > 0 ? 100.0 * sWin / sCnt : 0;

        log.info("{}", String.format(Locale.US,
                "  %-22s | %5d %10.0f %5.1f%% | %5d %10.0f %5.1f%%",
                label, lCnt, lPnl, lWinPct, sCnt, sPnl, sWinPct));
    }

    private static void printMonthly(Map<String, RotationalBacktestResult> results) {
        Map<String, Map<String, Double>> allMonthly = new LinkedHashMap<>();
        TreeSet<String> months = new TreeSet<>();
        for (Map.Entry<String, RotationalBacktestResult> e : results.entrySet()) {
            Map<String, Double> monthly = new LinkedHashMap<>();
            for (RotationalTrade t : e.getValue().getTradeLog()) {
                String m = Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthly.merge(m, t.netPnl(), Double::sum);
                months.add(m);
            }
            allMonthly.put(e.getKey(), monthly);
        }

        StringBuilder hdr = new StringBuilder(String.format("  %-8s", "Month"));
        for (String name : results.keySet()) {
            hdr.append(String.format(" | %12s", name.length() > 12 ? name.substring(0, 12) : name));
        }
        log.info("{}", hdr.toString());

        for (String month : months) {
            StringBuilder row = new StringBuilder(String.format("  %-8s", month));
            for (String name : results.keySet()) {
                double val = allMonthly.get(name).getOrDefault(month, 0.0);
                row.append(String.format(Locale.US, " | %12.0f", val));
            }
            log.info("{}", row.toString());
        }
    }

    private static void exportDailyCsv(Path csvPath, RotationalBacktestResult result,
                                        RegimeFeatureCollector collector) throws Exception {
        PortfolioTracker p = result.getPortfolio();
        List<Long> dates = p.getDates();
        List<Double> dailyPnl = p.getDailyPnl();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        Map<LocalDate, Integer> tradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> longTradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> shortTradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Double> longPnlPerDay = new LinkedHashMap<>();
        Map<LocalDate, Double> shortPnlPerDay = new LinkedHashMap<>();
        for (RotationalTrade t : result.getTradeLog()) {
            LocalDate d = Instant.ofEpochMilli(t.date()).atZone(IST).toLocalDate();
            tradesPerDay.merge(d, 1, Integer::sum);
            if (t.side() == RotationalTrade.Side.LONG) {
                longTradesPerDay.merge(d, 1, Integer::sum);
                longPnlPerDay.merge(d, t.netPnl(), Double::sum);
            } else {
                shortTradesPerDay.merge(d, 1, Integer::sum);
                shortPnlPerDay.merge(d, t.netPnl(), Double::sum);
            }
        }

        try (BufferedWriter w = Files.newBufferedWriter(csvPath)) {
            w.write("date," + RegimeFeatures.csvHeader()
                    + ",daily_pnl,trades,long_trades,short_trades,long_pnl,short_pnl");
            w.newLine();
            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = Instant.ofEpochMilli(dates.get(i)).atZone(IST).toLocalDate();
                RegimeFeatures features = collector.getFeatures(date);
                String featCsv = features != null ? features.toCsvRow() : "NaN,NaN,NaN,NaN,NaN";
                double pnl = dailyPnl.get(i);
                int trades = tradesPerDay.getOrDefault(date, 0);
                int lt = longTradesPerDay.getOrDefault(date, 0);
                int st = shortTradesPerDay.getOrDefault(date, 0);
                double lp = longPnlPerDay.getOrDefault(date, 0.0);
                double sp = shortPnlPerDay.getOrDefault(date, 0.0);
                w.write(String.format(Locale.US, "%s,%s,%.2f,%d,%d,%d,%.2f,%.2f",
                        date.format(dateFmt), featCsv, pnl, trades, lt, st, lp, sp));
                w.newLine();
            }
        }
    }
}
