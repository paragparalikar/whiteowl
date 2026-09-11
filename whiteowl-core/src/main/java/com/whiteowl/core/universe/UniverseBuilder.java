package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.core.indicator.IndicatorFunctions;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.FileScripRepository;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Builds stock universes by scoring all equity scrips using a volatility-based
 * composite metric and picking the top N.
 *
 * <h3>Scoring formula</h3>
 * <pre>
 *   A = avg( (H − L) / C )        over last {@code lookbackDays} days   (intraday range)
 *   B = avg( ATR(14) / C )         over last {@code lookbackDays} days   (smoothed volatility)
 *   C = avg( |O − C₋₁| / C )      over last {@code lookbackDays} days   (overnight gap)
 *
 *   score = A × B × C
 * </pre>
 *
 * <p>Stocks are sorted in descending order of score, and the top N are saved
 * as group CSV files under {@code $WHITEOWL_HOME/data/groups/}.</p>
 */
@Slf4j
public final class UniverseBuilder {

    private static final int ATR_PERIOD = 14;
    private static final int DEFAULT_LOOKBACK = 200;
    private static final double DEFAULT_MIN_AVG_TURNOVER = 3e7; // 3 Cr

    private final int lookbackDays;
    private final double minAvgTurnover;

    public UniverseBuilder() {
        this(DEFAULT_LOOKBACK, DEFAULT_MIN_AVG_TURNOVER);
    }

    public UniverseBuilder(int lookbackDays, double minAvgTurnover) {
        this.lookbackDays = lookbackDays;
        this.minAvgTurnover = minAvgTurnover;
    }

    /**
     * A scored symbol with the composite score and its components.
     *
     * @param symbol    scrip ID (e.g. "NSE:RELIANCE")
     * @param score     A × B × C
     * @param avgRange  A — average (H−L)/C
     * @param avgAtrPct B — average ATR(14)/C
     * @param avgGap    C — average |O−C₋₁|/C
     */
    public record ScoredSymbol(String symbol, double score,
                               double avgRange, double avgAtrPct, double avgGap) {}

    /**
     * Score and rank all symbols by the volatility composite metric.
     *
     * @param allDailyBars daily bars keyed by scrip ID
     * @return scored symbols sorted by score descending
     */
    public List<ScoredSymbol> scoreAndRank(Map<String, Bars> allDailyBars) {
        List<ScoredSymbol> results = new ArrayList<>();

        for (var entry : allDailyBars.entrySet()) {
            String symbol = entry.getKey();
            Bars bars = entry.getValue();
            int size = bars.size();

            // Need enough history for ATR warm-up + lookback window
            if (size < lookbackDays + ATR_PERIOD) continue;

            float[] atr = IndicatorFunctions.atr(
                    bars.arrays().high(), bars.arrays().low(),
                    bars.arrays().close(), size, ATR_PERIOD);

            int start = size - lookbackDays;

            // Hard filter 1: no day in the lookback window where H == L
            boolean hasZeroSpreadDay = false;
            for (int i = start; i < size; i++) {
                if (bars.getHigh(i) == bars.getLow(i)) {
                    hasZeroSpreadDay = true;
                    break;
                }
            }
            if (hasZeroSpreadDay) continue;

            double sumRange = 0, sumAtrPct = 0, sumGap = 0, sumTurnover = 0;
            int validDays = 0;

            for (int i = start; i < size; i++) {
                float h = bars.getHigh(i);
                float l = bars.getLow(i);
                float c = bars.getClose(i);
                float o = bars.getOpen(i);
                long v = bars.getVolume(i);

                if (c <= 0 || h < l) continue;

                // A: (H - L) / C
                sumRange += (h - l) / c;

                // B: ATR(14) / C
                if (atr != null && i < atr.length && !Float.isNaN(atr[i]) && atr[i] > 0) {
                    sumAtrPct += atr[i] / c;
                }

                // C: |O - C1| / C  (C1 = previous day close)
                if (i > 0) {
                    float prevClose = bars.getClose(i - 1);
                    if (prevClose > 0) {
                        sumGap += Math.abs(o - prevClose) / c;
                    }
                }

                sumTurnover += (double) c * v;
                validDays++;
            }

            if (validDays < lookbackDays / 2) continue;

            // Hard filter 2: average daily turnover > threshold
            double avgTurnover = sumTurnover / validDays;
            if (avgTurnover < minAvgTurnover) continue;

            double avgRange = sumRange / validDays;
            double avgAtrPct = sumAtrPct / validDays;
            double avgGap = sumGap / validDays;
            double score = avgRange * avgAtrPct * avgGap;

            results.add(new ScoredSymbol(symbol, score, avgRange, avgAtrPct, avgGap));
        }

        results.sort(Comparator.comparingDouble(ScoredSymbol::score).reversed());
        return results;
    }

    /**
     * Score all symbols, pick the top N for each count, and save as group CSVs.
     *
     * @param allDailyBars daily bars keyed by scrip ID
     * @param topCounts    universe sizes to create (e.g. 250, 500, 1000)
     */
    public void buildAndSave(Map<String, Bars> allDailyBars, int... topCounts) throws IOException {
        List<ScoredSymbol> ranked = scoreAndRank(allDailyBars);
        log.info("Scored {} symbols", ranked.size());
        log.info("");

        // Log top 20
        log.info("Top 20 by score:");
        log.info("  {}", String.format("%4s  %-22s  %12s  %8s  %8s  %8s",
                "Rank", "Symbol", "Score", "Range%", "ATR%", "Gap%"));
        log.info("  {}", "-".repeat(68));
        for (int i = 0; i < Math.min(20, ranked.size()); i++) {
            var s = ranked.get(i);
            log.info("  {}", String.format("%4d  %-22s  %12.8f  %7.2f%%  %7.2f%%  %7.2f%%",
                    i + 1, s.symbol(), s.score(),
                    s.avgRange() * 100, s.avgAtrPct() * 100, s.avgGap() * 100));
        }
        log.info("");

        Path groupsDir = Path.of(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"),
                "data", "groups");
        Files.createDirectories(groupsDir);

        for (int count : topCounts) {
            String groupName = "ORB Universe - " + count;
            int keepCount = Math.min(count, ranked.size());
            List<String> symbols = ranked.stream()
                    .limit(keepCount)
                    .map(ScoredSymbol::symbol)
                    .toList();

            Path groupFile = groupsDir.resolve(groupName + ".csv");
            Files.write(groupFile, symbols);
            log.info("Saved '{}' → {} symbols → {}", groupName, symbols.size(), groupFile);
        }
    }

    // ── Standalone entry point ────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        log.info("════════════════════════════════════════════════════════════");
        log.info("  Universe Builder — ORB Volatility Score");
        log.info("  score = avg(range) × avg(ATR%) × avg(gap)");
        log.info("════════════════════════════════════════════════════════════");
        log.info("");

        ScripRepository scripRepo = new FileScripRepository();
        BarsRepository barsRepo = new FileBarsRepository();

        // Load all NSE equity scrips
        List<Scrip> equities = scripRepo.findByScripType(ScripType.EQUITY).stream()
                .filter(s -> s.getExchange() == Exchange.NSE)
                .toList();
        log.info("Found {} equity scrips on NSE", equities.size());

        // Load daily bars in parallel
        Map<String, Bars> allDaily = loadDailyBarsParallel(barsRepo, equities);
        log.info("Loaded daily bars for {} scrips", allDaily.size());
        log.info("");

        // Build and save universes
        new UniverseBuilder().buildAndSave(allDaily, 250, 500, 1000);
    }

    private static Map<String, Bars> loadDailyBarsParallel(BarsRepository barsRepo,
                                                            List<Scrip> scrips) {
        int threads = Math.min(scrips.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Map<String, Bars> result = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();
        for (Scrip scrip : scrips) {
            futures.add(executor.submit(() -> {
                try {
                    if (!barsRepo.exists(scrip.getId(), Timeframe.DAILY)) return;
                    Bars bars = barsRepo.load(scrip.getId(), Timeframe.DAILY);
                    if (bars != null && bars.size() > 0) {
                        result.put(scrip.getId(), bars);
                    }
                } catch (IOException e) {
                    // skip silently
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.debug("Error loading bars: {}", e.getMessage());
            }
        }
        executor.shutdown();
        return result;
    }
}
