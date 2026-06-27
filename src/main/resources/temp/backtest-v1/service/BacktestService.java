package com.whiteowl.core.backtest.service;

import com.whiteowl.core.backtest.dsl.Signal;
import com.whiteowl.core.backtest.dsl.StrategyDsl;
import com.whiteowl.core.backtest.engine.BacktestListener;
import com.whiteowl.core.backtest.engine.PortfolioBacktestEngine;
import com.whiteowl.core.backtest.engine.ScripData;
import com.whiteowl.core.backtest.model.BacktestConfig;
import com.whiteowl.core.backtest.model.BacktestResult;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public final class BacktestService {

    private static final int MIN_BARS_REQUIRED = 1;

    private final ScripRepository scripRepository;
    private final BarsRepository barsRepository;
    private volatile boolean cancelled;

    public void run(Script compiledScript, StrategyDsl dsl, BacktestConfig config,
                    BacktestListener listener) {
        cancelled = false;
        List<Scrip> scrips = scripRepository.findByScripType(config.getScripType());
        int total = scrips.size();
        if (total == 0) return;
        Map<String, ScripData> scripDataMap = collectSignals(compiledScript, dsl, config,
                scrips, listener);
        if (cancelled || scripDataMap.isEmpty()) return;
        PortfolioBacktestEngine engine = createEngine(config);
        List<BacktestResult> results = engine.run(scripDataMap);
        for (BacktestResult result : results) {
            listener.onScripCompleted(result);
        }
    }

    public void cancel() {
        cancelled = true;
    }

    private Map<String, ScripData> collectSignals(Script compiledScript, StrategyDsl dsl,
                                                  BacktestConfig config, List<Scrip> scrips,
                                                  BacktestListener listener) {
        Map<String, ScripData> scripDataMap = new LinkedHashMap<>();
        int total = scrips.size();
        int completed = 0;
        for (Scrip scrip : scrips) {
            if (cancelled) break;
            try {
                Bars bars = loadBars(scrip.getId(), config.getTimeframe(), config.getOffset());
                if (bars != null && bars.size() >= MIN_BARS_REQUIRED) {
                    BarsArrays arrays = bars.arrays();
                    dsl.bind(arrays);
                    compiledScript.run();
                    List<Signal> signals = List.copyOf(dsl.getSignals());
                    if (!signals.isEmpty()) {
                        scripDataMap.put(scrip.getId(), new ScripData(arrays, signals));
                    }
                }
            } catch (Exception e) {
                log.debug("Backtest error for {}: {}", scrip.getSymbol(), e.getMessage());
                listener.onError(scrip, e.getMessage());
            }
            completed++;
            listener.onProgress(completed, total);
        }
        return scripDataMap;
    }

    private PortfolioBacktestEngine createEngine(BacktestConfig config) {
        return new PortfolioBacktestEngine(
                config.getInitialCapital(),
                config.getCostPercent(),
                config.getSlippagePercent(),
                config.getPositionSizePercent(),
                config.getMinPrice(),
                config.getVolumeParticipationPercent()
        );
    }

    private Bars loadBars(String scripId, Timeframe timeframe, int offset) throws IOException {
        if (offset <= 0) {
            return barsRepository.load(scripId, timeframe);
        }
        int totalBars = barsRepository.countBars(scripId, timeframe);
        int effectiveSize = totalBars - offset;
        if (effectiveSize < MIN_BARS_REQUIRED) {
            return null;
        }
        return barsRepository.loadRange(scripId, timeframe, 0, effectiveSize);
    }

}
