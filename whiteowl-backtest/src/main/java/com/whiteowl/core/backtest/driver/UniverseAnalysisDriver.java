package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.indicator.IndicatorFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Analyzes the composite ORB strategy's trades to identify what kind of stocks
 * perform best, enabling universe optimization.
 *
 * <p>For each stock in the universe, computes stock-level characteristics from
 * daily bars (averaged over the backtest period), then cross-tabulates against
 * per-symbol trade performance using decile-based bucketing. Generates an HTML
 * report showing which stock characteristics predict trading success.</p>
 */
public final class UniverseAnalysisDriver {

    private static final Logger log = LoggerFactory.getLogger(UniverseAnalysisDriver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final double SLIPPAGE = 0.001;
    private static final double INITIAL_CAPITAL = 1_000_000;

    // ── Data records ─────────────────────────────────────────────────

    record StockCharacteristics(
            String symbol,
            double avgDailyTurnoverLakhs,
            double avgSpreadPct,
            double avgOvernightGapPct,
            double avgBodySpreadRatio,
            double avgAtrPctOfPrice,
            double avgDailyVolume,
            double medianClosePrice,
            int tradingDays
    ) {}

    record SymbolPerformance(
            String symbol,
            int trades, int wins,
            double totalPnl, double avgPnl, double winRate, double profitFactor,
            double longPnl, double shortPnl, int longTrades, int shortTrades,
            double grossProfit, double grossLoss
    ) {}

    record Bucket(
            String label, double lo, double hi,
            int symbols, int trades, int wins,
            double totalPnl, double avgPnlPerTrade, double winRate,
            double profitFactor
    ) {}

    record CharacteristicAnalysis(
            String name, String unit,
            List<Bucket> buckets,
            double bestMinThreshold, double bestMaxThreshold,
            int bestRangeSymbols, int bestRangeTrades,
            double bestRangeAvgPnl, double bestRangeWinRate
    ) {}

    record JoinedStock(StockCharacteristics chars, SymbolPerformance perf) {}

    // ── Sub-strategy configs ─────────────────────────────────────────

    private static RotationalBacktestConfig longAlignedConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe").openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30)).exitTime(LocalTime.of(15, 15))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.65)
                .trailingStopEnabled(true).trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.25).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.25)
                .slippage(SLIPPAGE).initialCapital(INITIAL_CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.LONG)
                .minGapAtr(0.30).maxGapAtr(1.30).minOrbRvol(0.50).maxOrbIbs(0.90)
                .picks(6).build();
    }

    private static RotationalBacktestConfig shortAlignedConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe").openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(11, 0)).exitTime(null)
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(2.00)
                .trailingStopEnabled(true).trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.00).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(3.50)
                .slippage(SLIPPAGE).initialCapital(INITIAL_CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(-1.55).maxGapAtr(-0.45).minOrbRvol(0.50).maxOrbIbs(0.60)
                .picks(2).build();
    }

    private static RotationalBacktestConfig shortOppositeConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe").openingRangeMinutes(5).barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30)).exitTime(LocalTime.of(15, 20))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE).stopMultiplier(0.80)
                .trailingStopEnabled(false).targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE).targetMultiplier(4.75)
                .slippage(SLIPPAGE).initialCapital(INITIAL_CAPITAL).maxReEntries(0).atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(0.48).maxGapAtr(1.40).minOrbRvol(0.50).maxOrbIbs(0.90)
                .picks(9).build();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Universe Analysis — Stock Characteristics vs Trade Performance");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // ── Load data ─────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());
        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());
        log.info("");

        // ── Run composite backtest ────────────────────────────────
        log.info("Running composite backtest with Gap+RVOL ranker...");
        var subStrategies = List.of(
                new CompositeOrbStrategy.SubStrategy("Long-Aligned", longAlignedConfig()),
                new CompositeOrbStrategy.SubStrategy("Short-Aligned", shortAlignedConfig()),
                new CompositeOrbStrategy.SubStrategy("Short-Opposite", shortOppositeConfig()));
        var strategy = new CompositeOrbStrategy(subStrategies, 17, SLIPPAGE, INITIAL_CAPITAL);
        var ranker = new ConfigurableRanker(true, true, false);
        var engine = new CompositeBacktestEngine(strategy);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, ranker);
        log.info("Backtest: {} trades, Sortino={}, CAGR={}%",
                result.getMetrics().getTotalTrades(),
                fmt(result.getMetrics().getSortino()),
                fmt(result.getMetrics().getCagr() * 100));

        // ── Phase 1: Compute stock-level characteristics ──────────
        log.info("");
        log.info("Phase 1: Computing Stock Characteristics from Daily Bars");
        Map<String, StockCharacteristics> charMap = new LinkedHashMap<>();
        for (var entry : dailyBars.entrySet()) {
            StockCharacteristics sc = computeCharacteristics(entry.getKey(), entry.getValue());
            if (sc != null) charMap.put(entry.getKey(), sc);
        }
        log.info("Computed characteristics for {} / {} stocks", charMap.size(), dailyBars.size());

        // ── Phase 2: Per-symbol trade performance ─────────────────
        log.info("");
        log.info("Phase 2: Per-Symbol Trade Performance");
        Map<String, SymbolPerformance> perfMap = computeSymbolPerformance(result.getTradeLog());
        log.info("Symbols with trades: {} / {} universe", perfMap.size(), charMap.size());
        Set<String> neverTraded = new LinkedHashSet<>(charMap.keySet());
        neverTraded.removeAll(perfMap.keySet());
        log.info("Symbols never traded: {}", neverTraded.size());

        // ── Build joined dataset ──────────────────────────────────
        List<JoinedStock> joined = new ArrayList<>();
        for (var e : perfMap.entrySet()) {
            StockCharacteristics sc = charMap.get(e.getKey());
            if (sc != null) joined.add(new JoinedStock(sc, e.getValue()));
        }
        log.info("Joined dataset: {} stocks", joined.size());

        // ── Phase 3: Decile-based analysis per characteristic ─────
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 3: Stock Characteristics vs Trade Performance (Decile Buckets)");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        record CharDef(String name, String unit, ToDoubleFunction<StockCharacteristics> extractor) {}

        List<CharDef> characteristics = List.of(
                new CharDef("Avg Turnover", "L", StockCharacteristics::avgDailyTurnoverLakhs),
                new CharDef("Avg Spread %", "%", StockCharacteristics::avgSpreadPct),
                new CharDef("Avg O/N Gap %", "%", StockCharacteristics::avgOvernightGapPct),
                new CharDef("Avg Body/Spread", "ratio", StockCharacteristics::avgBodySpreadRatio),
                new CharDef("Avg ATR % Price", "%", StockCharacteristics::avgAtrPctOfPrice),
                new CharDef("Avg Volume", "K", sc -> sc.avgDailyVolume() / 1000.0),
                new CharDef("Median Price", "INR", StockCharacteristics::medianClosePrice));

        List<CharacteristicAnalysis> analyses = new ArrayList<>();

        for (CharDef cd : characteristics) {
            CharacteristicAnalysis ca = analyzeCharacteristic(cd.name(), cd.unit(), cd.extractor(), joined);
            analyses.add(ca);
        }

        // ── Phase 4: Top & Bottom stocks ──────────────────────────
        log.info("");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 4: Top & Bottom Performing Stocks");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        List<JoinedStock> byPnl = new ArrayList<>(joined);
        byPnl.sort((a, b) -> Double.compare(b.perf().totalPnl(), a.perf().totalPnl()));

        log.info("── Top 20 Stocks by Total PnL ──");
        log.info("{}", String.format("%-16s %6s %6s %10s %8s %8s %10s %8s %8s %8s",
                "Symbol", "Trades", "Wins", "Total PnL", "Avg PnL", "Win%",
                "Turnover", "Spread%", "Gap%", "Price"));
        for (int i = 0; i < Math.min(20, byPnl.size()); i++) {
            JoinedStock js = byPnl.get(i);
            log.info("{}", String.format("%-16s %6d %6d %10.0f %8.0f %7.1f%% %9.0fL %7.2f%% %7.2f%% %8.0f",
                    trunc(js.chars().symbol(), 16),
                    js.perf().trades(), js.perf().wins(), js.perf().totalPnl(),
                    js.perf().avgPnl(), js.perf().winRate(),
                    js.chars().avgDailyTurnoverLakhs(), js.chars().avgSpreadPct(),
                    js.chars().avgOvernightGapPct(), js.chars().medianClosePrice()));
        }
        log.info("");
        log.info("── Bottom 20 Stocks by Total PnL ──");
        for (int i = Math.max(0, byPnl.size() - 20); i < byPnl.size(); i++) {
            JoinedStock js = byPnl.get(i);
            log.info("{}", String.format("%-16s %6d %6d %10.0f %8.0f %7.1f%% %9.0fL %7.2f%% %7.2f%% %8.0f",
                    trunc(js.chars().symbol(), 16),
                    js.perf().trades(), js.perf().wins(), js.perf().totalPnl(),
                    js.perf().avgPnl(), js.perf().winRate(),
                    js.chars().avgDailyTurnoverLakhs(), js.chars().avgSpreadPct(),
                    js.chars().avgOvernightGapPct(), js.chars().medianClosePrice()));
        }

        // ── Phase 5: HTML Report ──────────────────────────────────
        log.info("");
        log.info("Phase 5: Generating HTML Report");
        Path reportPath = Path.of(System.getProperty("user.home"), ".whiteowl", "reports", "universe_analysis_report.html");
        reportPath.getParent().toFile().mkdirs();
        generateHtmlReport(reportPath, charMap.size(), neverTraded.size(), analyses, byPnl, result.getMetrics());
        log.info("HTML report: {}", reportPath.toAbsolutePath());
        log.info("");
        log.info("DONE");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Decile-based characteristic analysis
    // ══════════════════════════════════════════════════════════════════

    private static CharacteristicAnalysis analyzeCharacteristic(
            String name, String unit,
            ToDoubleFunction<StockCharacteristics> extractor,
            List<JoinedStock> joined) {

        log.info("══════════════════════════════════════════════════════════════");
        log.info("  {} Analysis", name);
        log.info("══════════════════════════════════════════════════════════════");

        // Extract and sort values
        double[] vals = joined.stream().mapToDouble(js -> extractor.applyAsDouble(js.chars())).toArray();
        log.info("Min={} P10={} Median={} Mean={} P90={} Max={}",
                fmt(min(vals)), fmt(pct(vals, 10)), fmt(pct(vals, 50)),
                fmt(mean(vals)), fmt(pct(vals, 90)), fmt(max(vals)));

        // Build decile boundaries (10 buckets: 0-10%, 10-20%, ..., 90-100%)
        double[] sorted = vals.clone();
        Arrays.sort(sorted);
        List<Bucket> buckets = new ArrayList<>();
        int n = sorted.length;

        for (int d = 0; d < 10; d++) {
            int idxLo = d * n / 10;
            int idxHi = (d + 1) * n / 10;
            if (idxHi == idxLo) continue;
            double lo = sorted[idxLo];
            double hi = (d == 9) ? sorted[n - 1] * 1.001 : sorted[idxHi]; // inclusive on last

            // Find stocks in this bucket
            final double flo = lo, fhi = hi;
            final int fd = d;
            List<JoinedStock> inBucket = joined.stream()
                    .filter(js -> {
                        double v = extractor.applyAsDouble(js.chars());
                        return fd == 9 ? (v >= flo) : (v >= flo && v < fhi);
                    }).toList();

            if (inBucket.isEmpty()) continue;

            int symbols = inBucket.size();
            int trades = inBucket.stream().mapToInt(js -> js.perf().trades()).sum();
            int wins = inBucket.stream().mapToInt(js -> js.perf().wins()).sum();
            double totalPnl = inBucket.stream().mapToDouble(js -> js.perf().totalPnl()).sum();
            double gp = inBucket.stream().mapToDouble(js -> js.perf().grossProfit()).sum();
            double gl = inBucket.stream().mapToDouble(js -> js.perf().grossLoss()).sum();
            double avgPnl = trades > 0 ? totalPnl / trades : 0;
            double winRate = trades > 0 ? (double) wins / trades * 100 : 0;
            double pf = gl > 0 ? gp / gl : (gp > 0 ? 999.0 : 0);

            String label = String.format("D%d [%s-%s]", d + 1, fmtCompact(lo), fmtCompact(hi));
            buckets.add(new Bucket(label, lo, hi, symbols, trades, wins, totalPnl, avgPnl, winRate, pf));

            log.info("{}", String.format("D%-2d [%8s,%8s) %5d sym %6d trd %10.0f PnL %8.0f avg %6.1f%% WR %6.2f PF",
                    d + 1, fmtCompact(lo), fmtCompact(hi), symbols, trades, totalPnl, avgPnl, winRate, pf));
        }

        // Find best contiguous range of deciles by avg PnL/trade
        double bestAvgPnl = Double.NEGATIVE_INFINITY;
        double bestMin = 0, bestMax = 0;
        int bestSymbols = 0, bestTrades = 0;
        double bestWinRate = 0;

        for (int i = 0; i < buckets.size(); i++) {
            for (int j = i; j < buckets.size(); j++) {
                double lo = buckets.get(i).lo(), hi = buckets.get(j).hi();
                final double flo = lo, fhi = hi;
                final int fj = j;
                List<JoinedStock> inRange = joined.stream()
                        .filter(js -> {
                            double v = extractor.applyAsDouble(js.chars());
                            return fj == buckets.size() - 1 ? (v >= flo) : (v >= flo && v < fhi);
                        }).toList();
                int syms = inRange.size();
                if (syms < 10) continue;
                int trds = inRange.stream().mapToInt(js -> js.perf().trades()).sum();
                if (trds < 50) continue;
                double pnl = inRange.stream().mapToDouble(js -> js.perf().totalPnl()).sum();
                double avg = pnl / trds;
                if (avg > bestAvgPnl) {
                    bestAvgPnl = avg;
                    bestMin = lo; bestMax = hi;
                    bestSymbols = syms; bestTrades = trds;
                    int w = inRange.stream().mapToInt(js -> js.perf().wins()).sum();
                    bestWinRate = trds > 0 ? (double) w / trds * 100 : 0;
                }
            }
        }

        log.info("BEST: [{}, {}]  Symbols={} Trades={} Avg PnL/Trade={} Win%={}%",
                fmtCompact(bestMin), fmtCompact(bestMax), bestSymbols, bestTrades,
                fmt(bestAvgPnl), fmt(bestWinRate));
        log.info("");

        return new CharacteristicAnalysis(name, unit, buckets,
                bestMin, bestMax, bestSymbols, bestTrades, bestAvgPnl, bestWinRate);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Stock characteristic computation
    // ══════════════════════════════════════════════════════════════════

    static StockCharacteristics computeCharacteristics(String symbol, Bars dailyBars) {
        int size = dailyBars.size();
        if (size < 30) return null;

        float[] atr14 = IndicatorFunctions.atr(
                dailyBars.arrays().high(), dailyBars.arrays().low(),
                dailyBars.arrays().close(), size, 14);

        double sumTurnover = 0, sumSpread = 0, sumGap = 0, sumBody = 0;
        double sumAtrPct = 0, sumVolume = 0;
        int validDays = 0, gapDays = 0;
        double[] closes = new double[size];

        for (int i = 1; i < size; i++) {
            float open = dailyBars.getOpen(i);
            float high = dailyBars.getHigh(i);
            float low = dailyBars.getLow(i);
            float close = dailyBars.getClose(i);
            long volume = dailyBars.getVolume(i);
            float prevClose = dailyBars.getClose(i - 1);

            if (close <= 0 || high <= low) continue;
            closes[validDays] = close;
            validDays++;

            sumTurnover += (double) close * volume / 100_000.0;
            sumSpread += (high - low) / close * 100.0;
            if (prevClose > 0) { sumGap += Math.abs(open - prevClose) / prevClose * 100.0; gapDays++; }
            double spread = high - low;
            if (spread > 0) sumBody += Math.abs(close - open) / spread;
            if (atr14 != null && i < atr14.length && atr14[i] > 0 && close > 0)
                sumAtrPct += atr14[i] / close * 100.0;
            sumVolume += volume;
        }
        if (validDays < 20) return null;

        double[] validCloses = Arrays.copyOf(closes, validDays);
        Arrays.sort(validCloses);
        return new StockCharacteristics(symbol,
                sumTurnover / validDays, sumSpread / validDays,
                gapDays > 0 ? sumGap / gapDays : 0, sumBody / validDays,
                sumAtrPct / validDays, sumVolume / validDays,
                validCloses[validDays / 2], validDays);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Per-symbol trade performance
    // ══════════════════════════════════════════════════════════════════

    static Map<String, SymbolPerformance> computeSymbolPerformance(List<RotationalTrade> trades) {
        Map<String, List<RotationalTrade>> bySymbol = trades.stream()
                .collect(Collectors.groupingBy(RotationalTrade::symbol, LinkedHashMap::new, Collectors.toList()));

        Map<String, SymbolPerformance> result = new LinkedHashMap<>();
        for (var e : bySymbol.entrySet()) {
            List<RotationalTrade> st = e.getValue();
            int total = st.size();
            int wins = (int) st.stream().filter(t -> t.netPnl() > 0).count();
            double totalPnl = st.stream().mapToDouble(RotationalTrade::netPnl).sum();
            double gp = st.stream().filter(t -> t.netPnl() > 0).mapToDouble(RotationalTrade::netPnl).sum();
            double gl = st.stream().filter(t -> t.netPnl() <= 0).mapToDouble(t -> Math.abs(t.netPnl())).sum();
            double longPnl = st.stream().filter(t -> t.side() == RotationalTrade.Side.LONG)
                    .mapToDouble(RotationalTrade::netPnl).sum();
            double shortPnl = st.stream().filter(t -> t.side() == RotationalTrade.Side.SHORT)
                    .mapToDouble(RotationalTrade::netPnl).sum();
            int longCount = (int) st.stream().filter(t -> t.side() == RotationalTrade.Side.LONG).count();
            result.put(e.getKey(), new SymbolPerformance(
                    e.getKey(), total, wins, totalPnl,
                    total > 0 ? totalPnl / total : 0,
                    total > 0 ? (double) wins / total * 100 : 0,
                    gl > 0 ? gp / gl : (gp > 0 ? 999 : 0),
                    longPnl, shortPnl, longCount, total - longCount,
                    gp, gl));
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════
    //  HTML Report
    // ══════════════════════════════════════════════════════════════════

    static void generateHtmlReport(Path outputPath,
                                    int universeSize, int neverTradedCount,
                                    List<CharacteristicAnalysis> analyses,
                                    List<JoinedStock> sortedByPnl,
                                    RotationalMetrics metrics) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(outputPath)) {
            w.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            w.write("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n");
            w.write("<title>Universe Analysis — Stock Characteristics vs Performance</title>\n");
            w.write("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>\n");
            w.write("<style>\n" + CSS + "</style>\n</head>\n<body>\n");

            // Header
            w.write("<div class=\"header\"><h1>Universe Analysis Report</h1>\n");
            w.write("<p class=\"subtitle\">Stock Characteristics vs Trade Performance — Decile Bucketing</p></div>\n");

            // KPIs
            w.write("<div class=\"kpi-grid\">\n");
            kpi(w, "Universe", String.valueOf(universeSize), "neutral");
            kpi(w, "Traded Stocks", String.valueOf(sortedByPnl.size()), "blue");
            kpi(w, "Never Traded", String.valueOf(neverTradedCount), "red");
            kpi(w, "Total Trades", String.valueOf(metrics.getTotalTrades()), "neutral");
            kpi(w, "Sortino", fmt(metrics.getSortino()), "green");
            kpi(w, "CAGR", fmt(metrics.getCagr() * 100) + "%", "green");
            w.write("</div>\n");

            // Characteristic sections
            int ci = 0;
            for (CharacteristicAnalysis ca : analyses) {
                String cid = "c" + ci++;
                w.write("<div class=\"section\"><h2>" + ca.name() + " Analysis</h2>\n");
                w.write(String.format("<div class=\"callout\">Best decile range: <strong>[%s, %s]</strong> &mdash; %d symbols, %d trades, Avg PnL/Trade: %s, Win%%: %s%%</div>\n",
                        fmtCompact(ca.bestMinThreshold()), fmtCompact(ca.bestMaxThreshold()),
                        ca.bestRangeSymbols(), ca.bestRangeTrades(),
                        fmt(ca.bestRangeAvgPnl()), fmt(ca.bestRangeWinRate())));
                w.write("<canvas id=\"" + cid + "\" height=\"60\"></canvas>\n");
                w.write("<table class=\"stats-table\"><tr><th>Decile</th><th>Range (" + ca.unit() + ")</th><th>Stocks</th><th>Trades</th>");
                w.write("<th>Wins</th><th>Total PnL</th><th>Avg PnL/Trd</th><th>Win%</th><th>PF</th></tr>\n");
                for (int i = 0; i < ca.buckets().size(); i++) {
                    Bucket b = ca.buckets().get(i);
                    String cls = b.avgPnlPerTrade() >= 0 ? "pos" : "neg";
                    w.write(String.format("<tr><td>D%d</td><td>[%s, %s)</td><td>%d</td><td>%d</td><td>%d</td>",
                            i + 1, fmtCompact(b.lo()), fmtCompact(b.hi()), b.symbols(), b.trades(), b.wins()));
                    w.write(String.format("<td class=\"%s\">%s</td><td class=\"%s\">%.0f</td><td>%.1f%%</td><td>%.2f</td></tr>\n",
                            cls, fmtPnl(b.totalPnl()), cls, b.avgPnlPerTrade(), b.winRate(), b.profitFactor()));
                }
                w.write("</table></div>\n");
            }

            // Full symbol table
            w.write("<div class=\"section\"><h2>Full Per-Symbol Breakdown (sorted by Total PnL)</h2>\n");
            w.write("<input type=\"text\" id=\"symFilter\" onkeyup=\"filterTable()\" placeholder=\"Search symbol...\" ");
            w.write("style=\"width:200px;padding:6px 10px;margin-bottom:10px;background:#334155;color:#e2e8f0;border:1px solid #475569;border-radius:6px\">\n");
            w.write("<table class=\"stats-table\" id=\"symTbl\"><tr>");
            w.write("<th>Symbol</th><th>Trades</th><th>Wins</th><th>Total PnL</th><th>Avg PnL</th>");
            w.write("<th>Win%</th><th>PF</th><th>Turnover(L)</th><th>Spread%</th><th>Gap%</th>");
            w.write("<th>Body/Spr</th><th>ATR%</th><th>Vol(K)</th><th>Price</th></tr>\n");
            for (JoinedStock js : sortedByPnl) {
                var sc = js.chars(); var sp = js.perf();
                String cls = sp.totalPnl() >= 0 ? "pos" : "neg";
                w.write(String.format("<tr><td>%s</td><td>%d</td><td>%d</td><td class=\"%s\">%s</td><td class=\"%s\">%.0f</td>",
                        sc.symbol(), sp.trades(), sp.wins(), cls, fmtPnl(sp.totalPnl()), cls, sp.avgPnl()));
                w.write(String.format("<td>%.1f%%</td><td>%.2f</td><td>%.0f</td><td>%.2f%%</td><td>%.2f%%</td>",
                        sp.winRate(), sp.profitFactor(), sc.avgDailyTurnoverLakhs(), sc.avgSpreadPct(), sc.avgOvernightGapPct()));
                w.write(String.format("<td>%.2f</td><td>%.2f%%</td><td>%.0f</td><td>%.0f</td></tr>\n",
                        sc.avgBodySpreadRatio(), sc.avgAtrPctOfPrice(), sc.avgDailyVolume() / 1000.0, sc.medianClosePrice()));
            }
            w.write("</table></div>\n");

            // Charts
            w.write("<script>\n");
            ci = 0;
            for (CharacteristicAnalysis ca : analyses) {
                writeChart(w, "c" + ci++, ca);
            }
            w.write("function filterTable(){var f=document.getElementById('symFilter').value.toUpperCase();");
            w.write("var rows=document.getElementById('symTbl').rows;");
            w.write("for(var i=1;i<rows.length;i++){rows[i].style.display=rows[i].cells[0].textContent.toUpperCase().indexOf(f)>-1?'':'none';}}\n");
            w.write("</script>\n");

            w.write("<div class=\"footer\">Generated " +
                    LocalDateTime.now(IST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    " IST</div>\n</body>\n</html>\n");
        }
    }

    private static void writeChart(BufferedWriter w, String id, CharacteristicAnalysis ca) throws IOException {
        StringBuilder labels = new StringBuilder("[");
        StringBuilder pnlData = new StringBuilder("[");
        StringBuilder wrData = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");
        for (int i = 0; i < ca.buckets().size(); i++) {
            Bucket b = ca.buckets().get(i);
            if (i > 0) { labels.append(","); pnlData.append(","); wrData.append(","); colors.append(","); }
            labels.append("'D").append(i + 1).append("'");
            pnlData.append(String.format("%.0f", b.avgPnlPerTrade()));
            wrData.append(String.format("%.1f", b.winRate()));
            colors.append(b.avgPnlPerTrade() >= 0 ? "'rgba(22,163,74,0.7)'" : "'rgba(220,38,38,0.7)'");
        }
        labels.append("]"); pnlData.append("]"); wrData.append("]"); colors.append("]");

        w.write("new Chart(document.getElementById('" + id + "'),{type:'bar',data:{labels:" + labels + ",datasets:[");
        w.write("{label:'Avg PnL/Trade',data:" + pnlData + ",backgroundColor:" + colors + ",yAxisID:'y'},");
        w.write("{label:'Win Rate %',data:" + wrData + ",type:'line',borderColor:'#f59e0b',pointBackgroundColor:'#f59e0b',pointRadius:4,borderWidth:2,yAxisID:'y1'}");
        w.write("]},options:{responsive:true,interaction:{intersect:false,mode:'index'},");
        w.write("plugins:{legend:{display:true,labels:{color:'#94a3b8'}},tooltip:{callbacks:{title:function(ctx){var b=" + ca.buckets().size() + ";var i=ctx[0].dataIndex;var ranges=");
        // Build ranges array for tooltip
        StringBuilder ranges = new StringBuilder("[");
        for (int i = 0; i < ca.buckets().size(); i++) {
            if (i > 0) ranges.append(",");
            ranges.append("'").append(ca.buckets().get(i).label().replace("'", "\\'")).append("'");
        }
        ranges.append("]");
        w.write(ranges + ";return ranges[i];}}}},");
        w.write("scales:{x:{ticks:{color:'#94a3b8'}},y:{position:'left',title:{display:true,text:'Avg PnL/Trade (INR)',color:'#94a3b8'},ticks:{color:'#94a3b8'},grid:{color:'#334155'}},");
        w.write("y1:{position:'right',title:{display:true,text:'Win Rate %',color:'#94a3b8'},ticks:{color:'#94a3b8'},grid:{drawOnChartArea:false}}}}});\n");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static void kpi(BufferedWriter w, String label, String value, String color) throws IOException {
        w.write(String.format("<div class=\"kpi-card %s\"><div class=\"kpi-value\">%s</div><div class=\"kpi-label\">%s</div></div>\n", color, value, label));
    }

    private static String trunc(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "~"; }
    private static String fmt(double v) { return String.format("%.2f", v); }
    private static String fmtCompact(double v) {
        if (Math.abs(v) >= 10000) return String.format("%.0f", v);
        if (Math.abs(v) >= 100) return String.format("%.0f", v);
        if (Math.abs(v) >= 10) return String.format("%.1f", v);
        return String.format("%.2f", v);
    }
    private static String fmtPnl(double pnl) {
        if (Math.abs(pnl) >= 100_000) return String.format("%.2fL", pnl / 100_000);
        return String.format("%.0f", pnl);
    }
    private static double min(double[] a) { return Arrays.stream(a).min().orElse(0); }
    private static double max(double[] a) { return Arrays.stream(a).max().orElse(0); }
    private static double mean(double[] a) { return Arrays.stream(a).average().orElse(0); }
    private static double pct(double[] values, double p) {
        double[] s = values.clone(); Arrays.sort(s);
        double idx = (p / 100.0) * (s.length - 1);
        int lo = (int) Math.floor(idx), hi = (int) Math.ceil(idx);
        if (lo == hi || hi >= s.length) return s[lo];
        return s[lo] + (idx - lo) * (s[hi] - s[lo]);
    }

    // ── CSS ──────────────────────────────────────────────────────────

    private static final String CSS = """
            *{margin:0;padding:0;box-sizing:border-box}
            body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                 background:#0f172a;color:#e2e8f0;line-height:1.6;padding:20px}
            .header{text-align:center;padding:30px 20px;margin-bottom:24px;
                    background:linear-gradient(135deg,#1e293b,#334155);border-radius:12px;border:1px solid #475569}
            .header h1{font-size:28px;color:#f8fafc;margin-bottom:8px}
            .subtitle{color:#94a3b8;font-size:14px}
            .kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:16px;margin-bottom:24px}
            .kpi-card{background:#1e293b;border-radius:10px;padding:20px;text-align:center;border:1px solid #334155}
            .kpi-value{font-size:26px;font-weight:700}
            .kpi-label{font-size:12px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;margin-top:4px}
            .kpi-card.green .kpi-value{color:#4ade80}
            .kpi-card.red .kpi-value{color:#f87171}
            .kpi-card.blue .kpi-value{color:#60a5fa}
            .kpi-card.neutral .kpi-value{color:#e2e8f0}
            .section{background:#1e293b;border-radius:10px;padding:24px;margin-bottom:20px;border:1px solid #334155}
            .section h2{font-size:18px;color:#f8fafc;margin-bottom:16px;border-bottom:1px solid #334155;padding-bottom:8px}
            .callout{background:#334155;border-left:4px solid #60a5fa;padding:12px 16px;margin-bottom:16px;border-radius:0 8px 8px 0;font-size:14px}
            table{width:100%;border-collapse:collapse;font-size:13px}
            .stats-table td,.stats-table th{padding:6px 10px;border-bottom:1px solid #334155}
            .stats-table th{text-align:left;color:#94a3b8;font-weight:600;position:sticky;top:0;background:#1e293b}
            .stats-table td{text-align:right}
            .stats-table td:first-child,.stats-table td:nth-child(2){text-align:left}
            .pos{color:#4ade80}.neg{color:#f87171}
            canvas{max-width:100%}
            .footer{text-align:center;color:#475569;font-size:12px;padding:20px}
            @media(max-width:768px){.kpi-grid{grid-template-columns:repeat(2,1fr)}}
            """;
}
