package com.whiteowl.core.backtest.v2.service;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptCompiler;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.engine.BacktestListener;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public final class BacktestService {

    private static final int MIN_BARS_REQUIRED = 1;

    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private volatile boolean cancelled;

    public void run(String scriptSource, BacktestConfig config,
                    BacktestListener listener) throws ScriptCompilationException {
        run(scriptSource, config, listener, null);
    }

    public void run(String scriptSource, BacktestConfig config,
                    BacktestListener listener,
                    TradeLifecycleCallback lifecycleCallback) throws ScriptCompilationException {
        cancelled = false;
        Class<? extends Script> compiledClass = ScriptCompiler.compileClass(
                scriptSource, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
        List<Scrip> scrips = scripRepository.findByScripType(config.getScripType());
        if (config.getExchange() != null) {
            scrips = scrips.stream().filter(s -> s.getExchange() == config.getExchange()).toList();
        }
        int total = scrips.size();
        if (total == 0) return;
        int threads = Math.min(total, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger completed = new AtomicInteger(0);
        List<Future<ScripResult>> futures = new ArrayList<>();
        for (Scrip scrip : scrips) {
            if (cancelled) break;
            futures.add(executor.submit(() -> runSingleScrip(scrip, compiledClass, config,
                    listener, lifecycleCallback, completed, total)));
        }
        for (Future<ScripResult> future : futures) {
            if (cancelled) break;
            try {
                ScripResult result = future.get();
                if (result != null) {
                    listener.onScripCompleted(result);
                }
            } catch (Exception e) {
                log.debug("Future error: {}", e.getMessage());
            }
        }
        executor.shutdown();
    }

    public void cancel() {
        cancelled = true;
    }

    private ScripResult runSingleScrip(Scrip scrip, Class<? extends Script> compiledClass,
                                        BacktestConfig config, BacktestListener listener,
                                        TradeLifecycleCallback lifecycleCallback,
                                        AtomicInteger completed, int total) {
        try {
            Bars bars = loadBars(scrip.getId(), config);
            if (bars == null || bars.size() < MIN_BARS_REQUIRED) {
                reportProgress(completed, total, listener);
                return null;
            }
            BarsArrays arrays = bars.arrays();
            Script instance = ScriptCompiler.instantiate(compiledClass);
            if (!(instance instanceof TradingStrategyBase strategy)) {
                log.warn("Script is not a TradingStrategyBase for {}", scrip.getId());
                reportProgress(completed, total, listener);
                return null;
            }
            strategy.setInputOverrides(config.getStrategyInputs());
            BacktestEngine engine = new BacktestEngine(
                    config.getInitialCapital(),
                    config.getCostPercent(),
                    config.getSlippagePercent(),
                    config.getVolumeParticipationPercent(),
                    lifecycleCallback
            );
            ScripResult result = engine.run(scrip.getId(), arrays, strategy);
            if (completed.get() == 0) {
                log.info("First scrip {}: bars={}, trades={}", scrip.getId(),
                        arrays.size(), result.getTrades().size());
            }
            reportProgress(completed, total, listener);
            return result;
        } catch (Exception e) {
            log.error("Backtest error for {}: {}", scrip.getId(), e.getMessage(), e);
            listener.onError(scrip, e.getMessage());
            reportProgress(completed, total, listener);
            return null;
        }
    }

    private void reportProgress(AtomicInteger completed, int total, BacktestListener listener) {
        int done = completed.incrementAndGet();
        listener.onProgress(done, total);
    }

    private Bars loadBars(String scripId, BacktestConfig config) {
        try {
            if (config.getOffset() <= 0) {
                return barsRepository.load(scripId, config.getTimeframe());
            }
            int totalBars = barsRepository.countBars(scripId, config.getTimeframe());
            int effectiveSize = totalBars - config.getOffset();
            if (effectiveSize < MIN_BARS_REQUIRED) {
                return null;
            }
            return barsRepository.loadRange(scripId, config.getTimeframe(), 0, effectiveSize);
        } catch (Exception e) {
            log.debug("Error loading bars for {}: {}", scripId, e.getMessage());
            return null;
        }
    }

}
