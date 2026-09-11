package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.backtest.rotational.RotationalTrade;
import com.whiteowl.core.bar.model.Bars;
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
 * Collects {@link TradeFeature} values for a list of trades and writes
 * the results to CSV for analysis.
 *
 * <p>Usage:</p>
 * <pre>
 *   TradeFeatureCollector collector = new TradeFeatureCollector(
 *       List.of(new OrbRangeAtrFeature(), new GapAtrFeature(), new OrbRvolFeature()),
 *       dailyBarsMap, intradayBarsMap, orBarCount);
 *
 *   collector.collectAndWriteCsv(trades, outputPath);
 * </pre>
 *
 * <p>The collector handles date alignment between trades (which carry epoch
 * millis) and daily/intraday bars (which are indexed by position). It builds
 * date-to-index maps for each scrip's bars and pre-computes intraday day
 * boundaries.</p>
 */
public final class TradeFeatureCollector {

    private static final Logger log = LoggerFactory.getLogger(TradeFeatureCollector.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final List<TradeFeature> features;
    private final Map<String, Bars> dailyBarsMap;
    private final Map<String, Bars> intradayBarsMap;
    private final int orBarCount;

    // Cached date-to-index maps per symbol
    private final Map<String, Map<LocalDate, Integer>> dailyDateIndexCache = new HashMap<>();
    private final Map<String, Map<LocalDate, int[]>> intradayDayCache = new HashMap<>();

    /**
     * @param features        list of trade features to compute
     * @param dailyBarsMap    per-scrip daily bars, keyed by scripId (may be null)
     * @param intradayBarsMap per-scrip intraday bars, keyed by scripId (may be null)
     * @param orBarCount      number of intraday bars forming the opening range
     */
    public TradeFeatureCollector(List<TradeFeature> features,
                                 Map<String, Bars> dailyBarsMap,
                                 Map<String, Bars> intradayBarsMap,
                                 int orBarCount) {
        this.features = features;
        this.dailyBarsMap = dailyBarsMap != null ? dailyBarsMap : Map.of();
        this.intradayBarsMap = intradayBarsMap != null ? intradayBarsMap : Map.of();
        this.orBarCount = orBarCount;

        // Pre-compute cross-sectional features (e.g. RS rank)
        for (TradeFeature f : features) {
            if (f instanceof CrossSectionalFeature csf) {
                log.info("Pre-computing cross-sectional feature: {}", csf.name());
                csf.preCompute(this.intradayBarsMap, this.dailyBarsMap, orBarCount);
            }
        }
    }

    /**
     * Backward-compatible constructor (daily bars only).
     */
    public TradeFeatureCollector(List<TradeFeature> features,
                                 Map<String, Bars> dailyBarsMap) {
        this(features, dailyBarsMap, null, 0);
    }

    /**
     * Compute all features for each trade and write results to a CSV file.
     */
    public void collectAndWriteCsv(List<RotationalTrade> trades, Path csvPath) throws IOException {
        log.info("Computing {} trade features for {} trades...", features.size(), trades.size());

        try (BufferedWriter w = Files.newBufferedWriter(csvPath)) {
            // Header
            StringBuilder header = new StringBuilder(
                    "date,symbol,side,entry_price,exit_price,net_pnl,exit_reason,winner");
            for (TradeFeature f : features) {
                header.append(',').append(f.name());
            }
            w.write(header.toString());
            w.newLine();

            int computed = 0, skipped = 0;

            for (RotationalTrade trade : trades) {
                TradeFeature.TradeContext ctx = buildContext(trade);

                StringBuilder row = new StringBuilder();
                LocalDate tradeDate = Instant.ofEpochMilli(trade.date())
                        .atZone(IST).toLocalDate();
                row.append(tradeDate.format(DATE_FMT));
                row.append(',').append(trade.symbol());
                row.append(',').append(trade.side().name());
                row.append(',').append(String.format("%.2f", trade.entryPrice()));
                row.append(',').append(String.format("%.2f", trade.exitPrice()));
                row.append(',').append(String.format("%.2f", trade.netPnl()));
                row.append(',').append(trade.exitReason().name());
                row.append(',').append(trade.netPnl() > 0 ? 1 : 0);

                boolean anyValid = false;
                for (TradeFeature f : features) {
                    double value = f.compute(ctx);
                    row.append(',');
                    if (Double.isNaN(value)) {
                        row.append("");
                    } else {
                        row.append(String.format("%.4f", value));
                        anyValid = true;
                    }
                }

                if (anyValid) computed++; else skipped++;
                w.write(row.toString());
                w.newLine();
            }

            log.info("Wrote {} trades to {} ({} with features, {} without data)",
                    trades.size(), csvPath, computed, skipped);
        }
    }

    /**
     * Compute features for all trades and return as a list of maps (for programmatic use).
     */
    public List<Map<String, Double>> collect(List<RotationalTrade> trades) {
        List<Map<String, Double>> results = new ArrayList<>();
        for (RotationalTrade trade : trades) {
            TradeFeature.TradeContext ctx = buildContext(trade);
            Map<String, Double> row = new LinkedHashMap<>();
            for (TradeFeature f : features) {
                row.put(f.name(), f.compute(ctx));
            }
            results.add(row);
        }
        return results;
    }

    // ── Context building ─────────────────────────────────────────────────

    private TradeFeature.TradeContext buildContext(RotationalTrade trade) {
        LocalDate tradeDate = Instant.ofEpochMilli(trade.date())
                .atZone(IST).toLocalDate();
        String symbol = trade.symbol();

        // Daily bars lookup
        Bars dailyBars = dailyBarsMap.get(symbol);
        int dayIndex = -1;
        if (dailyBars != null) {
            dayIndex = findDailyIndex(symbol, dailyBars, tradeDate);
        }

        // Intraday bars lookup
        Bars intradayBars = intradayBarsMap.get(symbol);
        int intradayDayStart = -1;
        int intradayDayCount = 0;
        if (intradayBars != null) {
            int[] dayRange = findIntradayDayRange(symbol, intradayBars, tradeDate);
            if (dayRange != null) {
                intradayDayStart = dayRange[0];
                intradayDayCount = dayRange[1];
            }
        }

        return new TradeFeature.TradeContext(trade, dailyBars, dayIndex,
                intradayBars, intradayDayStart, intradayDayCount, orBarCount);
    }

    private int findDailyIndex(String symbol, Bars dailyBars, LocalDate tradeDate) {
        Map<LocalDate, Integer> dateMap = dailyDateIndexCache.computeIfAbsent(symbol,
                s -> buildDateIndex(dailyBars));
        Integer idx = dateMap.get(tradeDate);
        return idx != null ? idx : -1;
    }

    private int[] findIntradayDayRange(String symbol, Bars intradayBars, LocalDate tradeDate) {
        Map<LocalDate, int[]> dayMap = intradayDayCache.computeIfAbsent(symbol,
                s -> buildIntradayDayMap(intradayBars));
        return dayMap.get(tradeDate);
    }

    private static Map<LocalDate, Integer> buildDateIndex(Bars bars) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (int i = 0; i < bars.size(); i++) {
            LocalDate date = Instant.ofEpochMilli(bars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            map.put(date, i);
        }
        return map;
    }

    /**
     * Build a map of date -> [startIdx, barCount] for each trading day in intraday bars.
     */
    private static Map<LocalDate, int[]> buildIntradayDayMap(Bars bars) {
        Map<LocalDate, int[]> map = new LinkedHashMap<>();
        if (bars.size() == 0) return map;

        int dayStart = 0;
        LocalDate currentDate = Instant.ofEpochMilli(bars.getTimestamp(0))
                .atZone(IST).toLocalDate();

        for (int i = 1; i < bars.size(); i++) {
            LocalDate barDate = Instant.ofEpochMilli(bars.getTimestamp(i))
                    .atZone(IST).toLocalDate();
            if (!barDate.equals(currentDate)) {
                map.put(currentDate, new int[]{dayStart, i - dayStart});
                dayStart = i;
                currentDate = barDate;
            }
        }
        map.put(currentDate, new int[]{dayStart, bars.size() - dayStart});
        return map;
    }
}
