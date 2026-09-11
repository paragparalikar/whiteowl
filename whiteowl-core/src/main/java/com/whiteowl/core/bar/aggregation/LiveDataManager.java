package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderUpdateListener;
import com.whiteowl.core.tick.model.Tick;
import com.whiteowl.core.tick.model.TickListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central orchestrator for live data flows.
 * Manages per-scrip tick-to-bar aggregators, routes ticks and order updates
 * to registered listeners, and coordinates bar persistence.
 */
@Slf4j
public final class LiveDataManager implements AutoCloseable {

    private final BarsRepository barsRepository;
    private final Map<String, TickBarAggregator> aggregators = new ConcurrentHashMap<>();
    private final Map<String, IntradayBarAggregator> intradayAggregators = new ConcurrentHashMap<>();
    private final List<BarCompletionListener> barListeners = new CopyOnWriteArrayList<>();
    private final List<OrderUpdateListener> orderUpdateListeners = new CopyOnWriteArrayList<>();
    private final List<TickListener> tickListeners = new CopyOnWriteArrayList<>();
    private final BarPersistenceListener persistenceListener;
    private final ExecutorService tickExecutor;
    private volatile boolean started;

    public LiveDataManager(BarsRepository barsRepository) {
        this.barsRepository = barsRepository;
        this.persistenceListener = new BarPersistenceListener(barsRepository);
        this.tickExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "live-data-tick-processor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts live data processing by registering as a tick and order update listener
     * on the broker adapter.
     */
    public void start(BrokerAdapter adapter) {
        if (started) return;
        started = true;
        adapter.addTickListener(this::onTick);
        adapter.addOrderUpdateListener(this::onOrderUpdate);
        log.info("LiveDataManager started");
    }

    public void addBarListener(BarCompletionListener listener) {
        barListeners.add(listener);
    }

    public void removeBarListener(BarCompletionListener listener) {
        barListeners.remove(listener);
    }

    public void addOrderUpdateListener(OrderUpdateListener listener) {
        orderUpdateListeners.add(listener);
    }

    public void removeOrderUpdateListener(OrderUpdateListener listener) {
        orderUpdateListeners.remove(listener);
    }

    public void addTickListener(TickListener listener) {
        tickListeners.add(listener);
    }

    public void removeTickListener(TickListener listener) {
        tickListeners.remove(listener);
    }

    private void onTick(Tick tick) {
        tickExecutor.execute(() -> processTick(tick));
    }

    private void processTick(Tick tick) {
        String scripId = tick.getScripId();

        for (TickListener listener : tickListeners) {
            try {
                listener.onTick(tick);
            } catch (Exception e) {
                log.warn("Error in tick listener", e);
            }
        }

        TickBarAggregator aggregator = aggregators.computeIfAbsent(scripId, id -> {
            TickBarAggregator agg = new TickBarAggregator(id);
            IntradayBarAggregator intradayAgg = new IntradayBarAggregator(id);

            // Wire 1m bar persistence
            agg.addListener(persistenceListener);
            // Wire 1m bars → higher TF aggregation
            agg.addListener(intradayAgg);
            // Wire higher TF bar persistence
            intradayAgg.addListener(persistenceListener);

            // Wire all registered bar listeners to both aggregators
            for (BarCompletionListener listener : barListeners) {
                agg.addListener(listener);
                intradayAgg.addListener(listener);
            }

            intradayAggregators.put(id, intradayAgg);

            alignToExistingBars(id, agg);
            log.debug("Created aggregator for {}", id);
            return agg;
        });

        aggregator.onTick(tick);
    }

    private void onOrderUpdate(Order order) {
        for (OrderUpdateListener listener : orderUpdateListeners) {
            try {
                listener.onOrderUpdate(order);
            } catch (Exception e) {
                log.warn("Error in order update listener", e);
            }
        }
    }

    /**
     * Aligns the aggregator to existing historical bar data to avoid duplicate bars.
     * If bars already exist for the scrip at 1m, the aggregator starts from the next boundary.
     */
    private void alignToExistingBars(String scripId, TickBarAggregator aggregator) {
        try {
            Optional<Long> latestTs = barsRepository.findLatestTimestamp(scripId, Timeframe.ONE_MINUTE);
            if (latestTs.isPresent()) {
                log.debug("Existing 1m bars for {} up to {}, aggregator will skip duplicates", scripId, latestTs.get());
            }
        } catch (IOException e) {
            log.warn("Failed to check existing bars for {}", scripId, e);
        }
    }

    /**
     * Flushes all active aggregators, completing their in-progress bars.
     * Call this at market close or shutdown.
     */
    public void flush() {
        aggregators.values().forEach(TickBarAggregator::flush);
        intradayAggregators.values().forEach(IntradayBarAggregator::flush);
    }

    @Override
    public void close() {
        flush();
        tickExecutor.shutdown();
        aggregators.clear();
        intradayAggregators.clear();
        started = false;
        log.info("LiveDataManager closed");
    }

    public boolean isStarted() {
        return started;
    }

}
