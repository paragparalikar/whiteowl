package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.discovery.BullishBarUptrendStrategy;
import com.whiteowl.core.backtest.discovery.CsvBarsLoader;
import com.whiteowl.core.backtest.discovery.DiscoveryTrade;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the {@link BullishBarUptrendStrategy} (Java port of
 * "Bullish bar in uptrend.afl") on an MCX continuous-futures CSV and writes
 * one row per trade for win/loss classification.
 *
 * <p>Per trade, all features are sampled at the entry bar (the decision bar —
 * entries fill at that bar's close), or {@code -lag} bars earlier to use
 * strictly past values. Columns: the six indicator grids
 * (RSI, close-vs-EMA distance, CCI, Choppiness Index, StdDev/EMA, ATR/EMA)
 * over Fibonacci lengths, then {@code entry_time_hhmm}, {@code bars_held},
 * {@code success} (net P&L &gt; 0) and {@code pnl_pct} (net P&L as % of the
 * entry price).
 *
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.BullishBarUptrendFeatureDriver \
 *       -Dexec.args="[inputCsv] -tf 15 -out out.csv [-lag 1] [-ema 130] [-ibs 0.66] [-atr 50] [-mult 1.0] [-cost 1.2]"
 * </pre>
 */
@Slf4j
public final class BullishBarUptrendFeatureDriver {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Path DEFAULT_INPUT = Path.of(
            "C:\\trading\\OptionsData_Order91_GOLD-GOLDM-and-6-more_Futures_2023-2026",
            "continuous_csv", "CRUDEOIL_continuous_1min.csv");

    /** Lengths for RSI, CCI and Choppiness Index. */
    private static final int[] SHORT_LENS = {2, 3, 5, 8, 13, 21, 34, 55};
    /** Lengths for EMA-distance, StdDev/EMA and ATR/EMA. */
    private static final int[] LONG_LENS = {2, 3, 5, 8, 13, 21, 34, 55, 89, 144};

    public static void main(String[] args) throws Exception {
        Path input = DEFAULT_INPUT;
        Path out = null;
        int barMinutes = 15;
        int lag = 0;
        int emaLen = 130, atrLen = 50;
        float ibs = 0.66f, mult = 1.0f, cost = 1.2f;

        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tf" -> barMinutes = Integer.parseInt(args[++i]);
                case "-out" -> out = Path.of(args[++i]);
                case "-lag" -> lag = Integer.parseInt(args[++i]);
                case "-ema" -> emaLen = Integer.parseInt(args[++i]);
                case "-ibs" -> ibs = Float.parseFloat(args[++i]);
                case "-atr" -> atrLen = Integer.parseInt(args[++i]);
                case "-mult" -> mult = Float.parseFloat(args[++i]);
                case "-cost" -> cost = Float.parseFloat(args[++i]);
                default -> positional.add(args[i]);
            }
        }
        if (!positional.isEmpty()) {
            input = Path.of(positional.get(0));
        }
        String symbol = symbolOf(input);
        if (out == null) {
            out = Path.of(System.getProperty("user.home"), ".whiteowl", "features",
                    "bullish_bar_uptrend_" + symbol + "_" + barMinutes + "min"
                            + (lag > 0 ? "_lag" + lag : "") + ".csv");
        }

        log.info("Loading {} -> {}-min bars", input, barMinutes);
        Bars bars = CsvBarsLoader.load(input, barMinutes);
        BarsArrays a = bars.arrays();
        int n = a.size();

        BullishBarUptrendStrategy strategy =
                new BullishBarUptrendStrategy(emaLen, ibs, atrLen, mult, cost);
        List<DiscoveryTrade> trades = strategy.run(a);
        long wins = trades.stream().filter(DiscoveryTrade::win).count();
        log.info("{}: {} trades, {} winners ({:.1f}%)", symbol, trades.size(), wins,
                trades.isEmpty() ? 0.0 : 100.0 * wins / trades.size());

        Map<String, float[]> features = buildFeatures(a, n);
        writeCsv(out, symbol, trades, features, lag);
        log.info("Wrote {} rows x {} feature columns to {}", trades.size(),
                features.size(), out.toAbsolutePath());
    }

    /**
     * The six requested feature grids. Every column is indexed by bar and is
     * causal (value at bar i uses only bars &le; i).
     */
    private static Map<String, float[]> buildFeatures(BarsArrays a, int n) {
        float[] c = a.close();
        Map<String, float[]> cols = new LinkedHashMap<>();

        float[][] ema = new float[LONG_LENS.length][];
        for (int k = 0; k < LONG_LENS.length; k++) {
            ema[k] = IndicatorFunctions.ema(c, n, LONG_LENS[k]);
        }

        for (int p : SHORT_LENS) {
            cols.put("rsi_" + p, IndicatorFunctions.rsi(c, n, p));
        }
        for (int k = 0; k < LONG_LENS.length; k++) {
            cols.put("ema_dist_" + LONG_LENS[k], emaDistance(c, ema[k], n));
        }
        for (int p : SHORT_LENS) {
            cols.put("cci_" + p,
                    IndicatorFunctions.cci(a.high(), a.low(), c, n, p));
        }
        for (int p : SHORT_LENS) {
            cols.put("chop_" + p,
                    IndicatorFunctions.choppinessIndex(a.high(), a.low(), c, n, p));
        }
        for (int k = 0; k < LONG_LENS.length; k++) {
            float[] sd = IndicatorFunctions.stddev(c, n, LONG_LENS[k]);
            cols.put("stddev_ema_" + LONG_LENS[k], ratio(sd, ema[k], n));
        }
        for (int k = 0; k < LONG_LENS.length; k++) {
            float[] atr = IndicatorFunctions.atr(a.high(), a.low(), c, n, LONG_LENS[k]);
            cols.put("atr_ema_" + LONG_LENS[k], ratio(atr, ema[k], n));
        }
        return cols;
    }

    private static float[] emaDistance(float[] close, float[] ema, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = ema[i] != 0f && !Float.isNaN(ema[i])
                    ? (close[i] - ema[i]) / ema[i] : Float.NaN;
        }
        return r;
    }

    private static float[] ratio(float[] num, float[] den, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = den[i] != 0f && !Float.isNaN(den[i]) && !Float.isNaN(num[i])
                    ? num[i] / den[i] : Float.NaN;
        }
        return r;
    }

    private static void writeCsv(Path file, String symbol, List<DiscoveryTrade> trades,
                                 Map<String, float[]> features, int lag) throws IOException {
        Files.createDirectories(file.getParent() != null ? file.getParent() : Path.of("."));
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            StringBuilder h = new StringBuilder(
                    "symbol,entry_date,entry_time,exit_date,exit_time,entry_price,exit_price");
            for (String name : features.keySet()) {
                h.append(',').append(name);
            }
            h.append(",bars_held,success,pnl_pct");
            w.write(h.toString());
            w.newLine();

            for (DiscoveryTrade t : trades) {
                StringBuilder r = new StringBuilder(1024);
                r.append(symbol).append(',')
                 .append(fmt(t.entryTimestamp(), DATE_FMT)).append(',')
                 .append(t.entryMinuteOfDay() / 60 * 100 + t.entryMinuteOfDay() % 60).append(',')
                 .append(fmt(t.exitTimestamp(), DATE_FMT)).append(',')
                 .append(hhmm(t.exitTimestamp())).append(',')
                 .append(t.entryPrice()).append(',')
                 .append(t.exitPrice());
                int bar = t.entryBarIndex() - lag;
                for (float[] col : features.values()) {
                    r.append(',').append(num(bar >= 0 ? col[bar] : Float.NaN));
                }
                float pnlPct = t.entryPrice() != 0f
                        ? t.netPoints() / t.entryPrice() * 100f : Float.NaN;
                r.append(',').append(t.holdingBars())
                 .append(',').append(t.win())
                 .append(',').append(num(pnlPct));
                w.write(r.toString());
                w.newLine();
            }
        }
    }

    private static String fmt(long epochMilli, DateTimeFormatter formatter) {
        return formatter.format(Instant.ofEpochMilli(epochMilli).atZone(IST));
    }

    private static int hhmm(long epochMilli) {
        var t = Instant.ofEpochMilli(epochMilli).atZone(IST);
        return t.getHour() * 100 + t.getMinute();
    }

    private static String num(float v) {
        return Float.isNaN(v) ? "" : Float.toString(v);
    }

    private static String symbolOf(Path csv) {
        String name = csv.getFileName().toString();
        int underscore = name.indexOf('_');
        return underscore > 0 ? name.substring(0, underscore) : name;
    }

}
