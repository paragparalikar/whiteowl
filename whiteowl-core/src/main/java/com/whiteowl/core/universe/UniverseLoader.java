package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Loads bar data for all equity scrips into a {@link UniverseDataFrame}.
 *
 * <p>Scans the scrip repository for all equities on the given exchange,
 * loads their bars in parallel, builds a union date index, and aligns
 * all symbols to it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * UniverseLoader loader = new UniverseLoader(barsRepository, scripRepository);
 * UniverseDataFrame universe = loader.load(Exchange.NSE, Timeframe.DAILY);
 * }</pre>
 */
@Slf4j
public final class UniverseLoader {

    private final BarsRepository barsRepository;
    private final ScripRepository scripRepository;

    public UniverseLoader(BarsRepository barsRepository, ScripRepository scripRepository) {
        this.barsRepository = barsRepository;
        this.scripRepository = scripRepository;
    }

    /**
     * Load all equity scrips on the given exchange into a UniverseDataFrame.
     *
     * @param exchange  the exchange to load (e.g., NSE)
     * @param timeframe the bar timeframe (e.g., DAILY)
     * @return a fully loaded and date-aligned UniverseDataFrame
     */
    public UniverseDataFrame load(Exchange exchange, Timeframe timeframe) {
        return load(exchange, timeframe, Collections.emptyList());
    }

    /**
     * Load all equity scrips on the given exchange into a UniverseDataFrame,
     * with additional index scrips (e.g., Nifty 50, India VIX).
     *
     * @param exchange       the exchange to load
     * @param timeframe      the bar timeframe
     * @param indexScripIds   additional index scrip IDs to load (e.g., "NSE:NIFTY 50", "NSE:INDIA VIX")
     * @return a fully loaded and date-aligned UniverseDataFrame
     */
    public UniverseDataFrame load(Exchange exchange, Timeframe timeframe,
                                   List<String> indexScripIds) {
        // Step 1: Find all equity scrips on this exchange
        List<Scrip> equityScrips = scripRepository.findByScripType(ScripType.EQUITY).stream()
                .filter(s -> s.getExchange() == exchange)
                .toList();

        log.info("Found {} equity scrips on {}", equityScrips.size(), exchange);

        // Step 2: Load bars in parallel
        Map<String, Bars> barsBySymbol = loadBarsParallel(equityScrips, timeframe);
        log.info("Loaded bars for {} symbols", barsBySymbol.size());

        // Step 3: Load index bars
        Map<String, Bars> indexBars = loadIndexBars(indexScripIds, timeframe);
        log.info("Loaded {} index data series", indexBars.size());

        // Step 4: Build union date index from equity bars
        long[] dates = buildUnionTimeline(barsBySymbol.values());

        // Step 5: Build index maps (dateIndex -> barIndex per symbol)
        Map<String, int[]> indexMaps = buildIndexMaps(barsBySymbol, dates);
        Map<String, int[]> indexIndexMaps = buildIndexMaps(indexBars, dates);

        // Step 6: Sort symbol names for deterministic order
        String[] symbols = barsBySymbol.keySet().stream().sorted().toArray(String[]::new);

        log.info("Universe built: {} symbols, {} dates (from {} to {})",
                symbols.length, dates.length,
                dates.length > 0 ? new Date(dates[0]) : "N/A",
                dates.length > 0 ? new Date(dates[dates.length - 1]) : "N/A");

        return new UniverseDataFrame(symbols, dates, barsBySymbol, indexMaps,
                indexBars, indexIndexMaps);
    }

    // --- Private helpers ---

    private Map<String, Bars> loadBarsParallel(List<Scrip> scrips, Timeframe timeframe) {
        int threads = Math.min(scrips.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Map<String, Bars> result = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();
        for (Scrip scrip : scrips) {
            futures.add(executor.submit(() -> {
                try {
                    if (!barsRepository.exists(scrip.getId(), timeframe)) return;
                    Bars bars = barsRepository.load(scrip.getId(), timeframe);
                    if (bars != null && bars.size() > 0) {
                        result.put(scrip.getId(), bars);
                    }
                } catch (IOException e) {
                    log.debug("Failed to load bars for {}: {}", scrip.getId(), e.getMessage());
                }
            }));
        }

        // Wait for all loads to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.warn("Error during parallel bar loading: {}", e.getMessage());
            }
        }
        executor.shutdown();

        return result;
    }

    private Map<String, Bars> loadIndexBars(List<String> indexScripIds, Timeframe timeframe) {
        Map<String, Bars> result = new LinkedHashMap<>();
        for (String scripId : indexScripIds) {
            try {
                if (!barsRepository.exists(scripId, timeframe)) {
                    log.warn("Index bars not found: {}", scripId);
                    continue;
                }
                Bars bars = barsRepository.load(scripId, timeframe);
                if (bars != null && bars.size() > 0) {
                    result.put(scripId, bars);
                }
            } catch (IOException e) {
                log.warn("Failed to load index bars for {}: {}", scripId, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Build a sorted union of all timestamps across all scrips.
     * Uses a TreeSet to deduplicate and sort.
     */
    private long[] buildUnionTimeline(Collection<Bars> allBars) {
        TreeSet<Long> union = new TreeSet<>();
        for (Bars bars : allBars) {
            for (int i = 0; i < bars.size(); i++) {
                union.add(bars.getTimestamp(i));
            }
        }
        return union.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * For each symbol, build an int[] map from dateIndex -> barIndex.
     * Uses a merge-scan (both arrays sorted ascending) for O(T + B) per symbol.
     */
    private Map<String, int[]> buildIndexMaps(Map<String, Bars> allBars, long[] dates) {
        Map<String, int[]> maps = new HashMap<>();
        int dateCount = dates.length;

        for (Map.Entry<String, Bars> entry : allBars.entrySet()) {
            Bars bars = entry.getValue();
            int[] map = new int[dateCount];
            Arrays.fill(map, -1);

            int barIdx = 0;
            int barSize = bars.size();
            for (int d = 0; d < dateCount && barIdx < barSize; d++) {
                long date = dates[d];
                // Advance bar index to match or pass this date
                while (barIdx < barSize && bars.getTimestamp(barIdx) < date) {
                    barIdx++;
                }
                if (barIdx < barSize && bars.getTimestamp(barIdx) == date) {
                    map[d] = barIdx;
                    barIdx++;
                }
            }

            maps.put(entry.getKey(), map);
        }

        return maps;
    }
}
