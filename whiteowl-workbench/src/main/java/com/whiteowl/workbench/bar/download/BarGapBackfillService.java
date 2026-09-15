package com.whiteowl.workbench.bar.download;

import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fills gaps in stored bar data on demand, up to "now".
 * Deduplicates concurrent/repeat requests per (scrip, timeframe) within a session.
 * Delegates the actual fetch to {@link BarDataDownloader}, which handles chunking,
 * gap verification, and retry on gap.
 */
@Slf4j
public final class BarGapBackfillService {

    private static final int WORKER_COUNT = 4;

    private final ActiveAccountManager activeAccountManager;
    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private final Map<String, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public BarGapBackfillService(ActiveAccountManager activeAccountManager,
                                 ScripRepository scripRepository,
                                 BarsRepository barsRepository) {
        this.activeAccountManager = activeAccountManager;
        this.scripRepository = scripRepository;
        this.barsRepository = barsRepository;
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(WORKER_COUNT, r -> {
            Thread t = new Thread(r, "bar-gap-backfill-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Void> backfill(Scrip scrip, Timeframe timeframe) {
        String key = scrip.getId() + "|" + timeframe.name();
        CompletableFuture<Void> existing = inFlight.get(key);
        if (existing != null) return existing;
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> runBackfill(scrip, timeframe), executor);
        CompletableFuture<Void> previous = inFlight.putIfAbsent(key, future);
        if (previous != null) return previous;
        return future;
    }

    public CompletableFuture<Void> backfill(Scrip scrip, List<Timeframe> timeframes) {
        CompletableFuture<?>[] futures = timeframes.stream()
                .map(tf -> backfill(scrip, tf))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    public CompletableFuture<Void> backfill(Collection<Scrip> scrips, List<Timeframe> timeframes) {
        CompletableFuture<?>[] futures = scrips.stream()
                .flatMap(s -> timeframes.stream().map(tf -> backfill(s, tf)))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private void runBackfill(Scrip scrip, Timeframe timeframe) {
        try {
            BrokerAdapter adapter = activeAccountManager.getActiveAdapter().orElse(null);
            if (adapter == null) {
                log.debug("No active broker adapter — skipping backfill for {} {}", scrip.getSymbol(), timeframe.name());
                return;
            }
            BarDataDownloader downloader = BarDataDownloader.builder()
                    .scripRepository(scripRepository)
                    .barsRepository(barsRepository)
                    .brokerAdapter(adapter)
                    .build();
            downloader.downloadSingle(scrip, timeframe, null, false);
            log.debug("Backfill completed for {} {}", scrip.getSymbol(), timeframe.name());
        } catch (Exception e) {
            log.warn("Backfill failed for {} {}: {}", scrip.getSymbol(), timeframe.name(), e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
