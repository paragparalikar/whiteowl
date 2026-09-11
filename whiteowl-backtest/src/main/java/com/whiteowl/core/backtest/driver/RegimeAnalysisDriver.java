package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.backtest.rotational.regime.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Runs the rotational ORB backtest and collects regime features for each trading day,
 * then exports a combined CSV for analysis: date, regime features, daily P&L, trade count, etc.
 *
 * <p>This driver also computes and prints a regime-conditional performance summary,
 * splitting results by terciles of each regime feature.</p>
 */
public final class RegimeAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(RegimeAnalysisDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void main(String[] args) throws Exception {

        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Regime Feature Analysis — Rotational ORB Strategy");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");

        // ── Build config (frozen params, 0.20% slippage) ─────────────────
        RotationalBacktestConfig config = RotationalBacktestConfig.builder()
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

        // ── Load data ────────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();

        // Load universe intraday + daily
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());

        // Load Nifty 50 daily bars for regime features
        Bars niftyDailyBars = barsRepo.load("NSE:NIFTY 50", Timeframe.DAILY);
        log.info("Loaded Nifty 50 daily bars: {} bars", niftyDailyBars.size());

        // Try to load Nifty 50 intraday bars (for continuation rate)
        Bars niftyIntradayBars = null;
        try {
            if (barsRepo.exists("NSE:NIFTY 50", Timeframe.FIVE_MINUTE)) {
                niftyIntradayBars = barsRepo.load("NSE:NIFTY 50", Timeframe.FIVE_MINUTE);
                log.info("Loaded Nifty 50 intraday bars: {} bars", niftyIntradayBars.size());
            }
        } catch (IOException e) {
            log.warn("No Nifty 50 intraday bars found, skipping continuation rate");
        }

        // ── Compute regime features ──────────────────────────────────────
        log.info("");
        log.info("Computing regime features (backward-looking, no lookahead)...");
        RegimeFeatureCollector collector = new RegimeFeatureCollector();
        collector.preCompute(niftyDailyBars, niftyIntradayBars, dailyBars);

        // ── Run backtest ─────────────────────────────────────────────────
        log.info("");
        log.info("Running backtest...");
        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
        log.info("Backtest complete: {} trades", result.getTradeLog().size());

        // ── Join regime features with daily P&L ──────────────────────────
        PortfolioTracker portfolio = result.getPortfolio();
        List<Long> dates = portfolio.getDates();
        List<Double> dailyPnl = portfolio.getDailyPnl();

        // Count trades per day
        Map<LocalDate, Integer> tradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> longTradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> shortTradesPerDay = new LinkedHashMap<>();
        Map<LocalDate, Double> longPnlPerDay = new LinkedHashMap<>();
        Map<LocalDate, Double> shortPnlPerDay = new LinkedHashMap<>();
        for (RotationalTrade trade : result.getTradeLog()) {
            LocalDate d = Instant.ofEpochMilli(trade.date()).atZone(IST).toLocalDate();
            tradesPerDay.merge(d, 1, Integer::sum);
            if (trade.side() == RotationalTrade.Side.LONG) {
                longTradesPerDay.merge(d, 1, Integer::sum);
                longPnlPerDay.merge(d, trade.netPnl(), Double::sum);
            } else {
                shortTradesPerDay.merge(d, 1, Integer::sum);
                shortPnlPerDay.merge(d, trade.netPnl(), Double::sum);
            }
        }

        // Build combined daily records
        List<DayRecord> dayRecords = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = Instant.ofEpochMilli(dates.get(i)).atZone(IST).toLocalDate();
            RegimeFeatures features = collector.getFeatures(date);
            if (features == null) continue;

            double pnl = dailyPnl.get(i);
            int trades = tradesPerDay.getOrDefault(date, 0);
            int longs = longTradesPerDay.getOrDefault(date, 0);
            int shorts = shortTradesPerDay.getOrDefault(date, 0);
            double lPnl = longPnlPerDay.getOrDefault(date, 0.0);
            double sPnl = shortPnlPerDay.getOrDefault(date, 0.0);

            dayRecords.add(new DayRecord(date, features, pnl, trades, longs, shorts, lPnl, sPnl));
        }

        // ── Export CSV ───────────────────────────────────────────────────
        Path outputDir = Path.of(System.getProperty("user.home"), ".whiteowl", "output");
        Files.createDirectories(outputDir);
        Path csvPath = outputDir.resolve("regime_analysis.csv");

        try (BufferedWriter w = Files.newBufferedWriter(csvPath)) {
            w.write("date," + RegimeFeatures.csvHeader()
                    + ",daily_pnl,trades,long_trades,short_trades,long_pnl,short_pnl");
            w.newLine();
            for (DayRecord rec : dayRecords) {
                w.write(String.format("%s,%s,%.2f,%d,%d,%d,%.2f,%.2f",
                        rec.date.format(DATE_FMT), rec.features.toCsvRow(),
                        rec.dailyPnl, rec.trades, rec.longTrades, rec.shortTrades,
                        rec.longPnl, rec.shortPnl));
                w.newLine();
            }
        }
        log.info("Regime analysis CSV exported to: {}", csvPath);

        // ── Regime-conditional analysis ───────────────────────────────────
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  REGIME-CONDITIONAL PERFORMANCE ANALYSIS");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");

        analyzeByFeature(dayRecords, "continuation_rate_20d", r -> r.features.continuationRate());
        analyzeByFeature(dayRecords, "realized_vol_20d", r -> r.features.realizedVol());
        analyzeByFeature(dayRecords, "cross_sectional_vol_20d", r -> r.features.crossSectionalVol());
        analyzeByFeature(dayRecords, "gap_fill_rate_20d", r -> r.features.gapFillRate());
        analyzeByFeature(dayRecords, "nifty_sma_slope_20d", r -> r.features.smaSlope());

        // ── Correlation matrix ───────────────────────────────────────────
        log.info("");
        log.info("  FEATURE CORRELATIONS WITH DAILY P&L:");
        log.info("  ─────────────────────────────────────");
        printCorrelation(dayRecords, "continuation_rate_20d", r -> r.features.continuationRate());
        printCorrelation(dayRecords, "realized_vol_20d", r -> r.features.realizedVol());
        printCorrelation(dayRecords, "cross_sectional_vol_20d", r -> r.features.crossSectionalVol());
        printCorrelation(dayRecords, "gap_fill_rate_20d", r -> r.features.gapFillRate());
        printCorrelation(dayRecords, "nifty_sma_slope_20d", r -> r.features.smaSlope());

        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  REGIME ANALYSIS COMPLETE");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
    }

    // ── Analysis helpers ─────────────────────────────────────────────────

    /**
     * Split days into terciles by a regime feature, then compute per-tercile
     * Sharpe, total P&L, win rate, avg daily P&L, and trade count.
     */
    private static void analyzeByFeature(List<DayRecord> records, String featureName,
                                          java.util.function.ToDoubleFunction<DayRecord> extractor) {
        // Filter out NaN values
        List<DayRecord> valid = records.stream()
                .filter(r -> !Double.isNaN(extractor.applyAsDouble(r)))
                .sorted(Comparator.comparingDouble(extractor::applyAsDouble))
                .toList();

        if (valid.size() < 9) {
            log.info("  {} — insufficient data for tercile analysis", featureName);
            return;
        }

        int tercileSize = valid.size() / 3;
        List<DayRecord> low = valid.subList(0, tercileSize);
        List<DayRecord> mid = valid.subList(tercileSize, 2 * tercileSize);
        List<DayRecord> high = valid.subList(2 * tercileSize, valid.size());

        log.info("");
        log.info("  ┌─────────────────────────────────────────────────────────────────────────────────────────┐");
        log.info("  │  {} (n={})", String.format("%-70s", featureName), valid.size());
        log.info("  ├────────────┬────────────────┬──────────┬──────────┬──────────┬─────────┬────────────────┤");
        log.info("  │  Tercile   │  Feature Range │  Days    │  Sharpe  │  Total $ │  Win%   │  Avg Daily $   │");
        log.info("  ├────────────┼────────────────┼──────────┼──────────┼──────────┼─────────┼────────────────┤");

        printTercileLine("Low", low, extractor);
        printTercileLine("Mid", mid, extractor);
        printTercileLine("High", high, extractor);

        log.info("  └────────────┴────────────────┴──────────┴──────────┴──────────┴─────────┴────────────────┘");
    }

    private static void printTercileLine(String label, List<DayRecord> records,
                                          java.util.function.ToDoubleFunction<DayRecord> extractor) {
        double minVal = records.stream().mapToDouble(extractor::applyAsDouble).min().orElse(0);
        double maxVal = records.stream().mapToDouble(extractor::applyAsDouble).max().orElse(0);
        int days = records.size();
        double totalPnl = records.stream().mapToDouble(r -> r.dailyPnl).sum();
        long winDays = records.stream().filter(r -> r.dailyPnl > 0).count();
        double winRate = days > 0 ? 100.0 * winDays / days : 0;
        double avgDailyPnl = days > 0 ? totalPnl / days : 0;

        // Sharpe: mean(daily P&L) / std(daily P&L) * sqrt(252)
        double[] pnls = records.stream().mapToDouble(r -> r.dailyPnl).toArray();
        double mean = Arrays.stream(pnls).average().orElse(0);
        double variance = Arrays.stream(pnls).map(p -> (p - mean) * (p - mean)).average().orElse(0);
        double std = Math.sqrt(variance);
        double sharpe = std > 0 ? mean / std * Math.sqrt(252) : 0;

        String totalPnlStr = String.format("%,.0f", totalPnl);
        if (totalPnl > 0) totalPnlStr = "+" + totalPnlStr;
        String avgPnlStr = String.format("%,.0f", avgDailyPnl);
        if (avgDailyPnl > 0) avgPnlStr = "+" + avgPnlStr;
        log.info(String.format("  │  %-8s  │  %5.2f - %5.2f  │  %-6d  │  %+6.2f  │  %10s │  %5.1f  │  %12s  │",
                label, minVal, maxVal, days, sharpe, totalPnlStr, winRate, avgPnlStr));
    }

    private static void printCorrelation(List<DayRecord> records, String featureName,
                                          java.util.function.ToDoubleFunction<DayRecord> extractor) {
        List<DayRecord> valid = records.stream()
                .filter(r -> !Double.isNaN(extractor.applyAsDouble(r)))
                .toList();

        if (valid.size() < 10) {
            log.info("  {} — insufficient data", featureName);
            return;
        }

        double[] x = valid.stream().mapToDouble(extractor::applyAsDouble).toArray();
        double[] y = valid.stream().mapToDouble(r -> r.dailyPnl).toArray();

        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);
        double cov = 0, varX = 0, varY = 0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            cov += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        double corr = (varX > 0 && varY > 0) ? cov / Math.sqrt(varX * varY) : 0;
        log.info(String.format("  %-30s  r = %+.4f  (n=%d)", featureName, corr, valid.size()));
    }

    // ── Data record ──────────────────────────────────────────────────────

    private record DayRecord(
            LocalDate date,
            RegimeFeatures features,
            double dailyPnl,
            int trades,
            int longTrades,
            int shortTrades,
            double longPnl,
            double shortPnl
    ) {}
}
