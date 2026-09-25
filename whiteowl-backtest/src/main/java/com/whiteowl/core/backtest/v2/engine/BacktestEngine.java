package com.whiteowl.core.backtest.v2.engine;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleEvent;
import com.whiteowl.core.backtest.v2.feature.TradeLifecyclePhase;
import com.whiteowl.core.backtest.v2.model.EquityCurve;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.IndicatorGraph;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.breadth.BreadthResult;
import com.whiteowl.core.rs.RSResult;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class BacktestEngine {

    private static final float HUNDRED = 100f;

    private final float initialCapital;
    private final float costPercent;
    private final float slippagePercent;
    private final float volumeParticipationPercent;
    private final TradeLifecycleCallback lifecycleCallback;
    private final StandardExitPolicy exitPolicy;

    public BacktestEngine(float initialCapital, float costPercent,
                          float slippagePercent, float volumeParticipationPercent) {
        this(initialCapital, costPercent, slippagePercent, volumeParticipationPercent, null);
    }

    public BacktestEngine(float initialCapital, float costPercent,
                          float slippagePercent, float volumeParticipationPercent,
                          TradeLifecycleCallback lifecycleCallback) {
        this(initialCapital, costPercent, slippagePercent, volumeParticipationPercent,
                lifecycleCallback, null);
    }

    public BacktestEngine(float initialCapital, float costPercent,
                          float slippagePercent, float volumeParticipationPercent,
                          TradeLifecycleCallback lifecycleCallback,
                          StandardExitPolicy exitPolicy) {
        this.initialCapital = initialCapital;
        this.costPercent = costPercent;
        this.slippagePercent = slippagePercent;
        this.volumeParticipationPercent = volumeParticipationPercent;
        this.lifecycleCallback = lifecycleCallback;
        this.exitPolicy = exitPolicy;
    }

    public ScripResult run(String scripId, BarsArrays arrays, TradingStrategyBase strategy) {
        return run(scripId, arrays, strategy, Map.of(), Map.of());
    }

    public ScripResult run(String scripId, BarsArrays arrays, TradingStrategyBase strategy,
                            Map<String, BreadthResult> breadthResults) {
        return run(scripId, arrays, strategy, breadthResults, Map.of());
    }

    public ScripResult run(String scripId, BarsArrays arrays, TradingStrategyBase strategy,
                            Map<String, BreadthResult> breadthResults,
                            Map<String, RSResult> rsResults) {
        int size = arrays.size();
        PositionTracker tracker = new PositionTracker(scripId, costPercent, slippagePercent);
        PortfolioState portfolio = new PortfolioState(initialCapital, tracker);
        SignalQueue signalQueue = new SignalQueue();
        List<TradeRecord> allTrades = new ArrayList<>();
        float[] equityValues = new float[size];
        long[] equityTimestamps = arrays.timestamp();
        int maxBars = strategy.getMaxBars();
        FloatSmartValue open = new FloatSmartValue(maxBars);
        FloatSmartValue high = new FloatSmartValue(maxBars);
        FloatSmartValue low = new FloatSmartValue(maxBars);
        FloatSmartValue close = new FloatSmartValue(maxBars);
        LongSmartValue volume = new LongSmartValue(maxBars);
        LongSmartValue timestamp = new LongSmartValue(maxBars);
        strategy.bindBarData(open, high, low, close, volume, timestamp);
        strategy.bindEngine(portfolio, signalQueue, tracker, volumeParticipationPercent);

        // Align and bind breadth data
        Map<String, FloatSmartValue> breadthSmartValues = new LinkedHashMap<>();
        Map<String, float[]> breadthAligned = new LinkedHashMap<>();
        for (Map.Entry<String, BreadthResult> entry : breadthResults.entrySet()) {
            float[] aligned = alignBreadthToScrip(entry.getValue(), arrays);
            breadthAligned.put(entry.getKey(), aligned);
            FloatSmartValue sv = new FloatSmartValue(maxBars);
            breadthSmartValues.put(entry.getKey(), sv);
            strategy.bindBreadthData(entry.getKey(), sv);
        }

        // Align and bind RS data
        Map<String, FloatSmartValue> rsSmartValues = new LinkedHashMap<>();
        Map<String, float[]> rsAligned = new LinkedHashMap<>();
        for (Map.Entry<String, RSResult> entry : rsResults.entrySet()) {
            float[] aligned = alignRSToScrip(entry.getValue(), arrays);
            rsAligned.put(entry.getKey(), aligned);
            FloatSmartValue sv = new FloatSmartValue(maxBars);
            rsSmartValues.put(entry.getKey(), sv);
            strategy.bindRSData(entry.getKey(), sv);
        }

        IndicatorGraph graph = strategy.getIndicatorGraph();
        strategy.invokeSetup();
        log.info("Setup complete for {}: indicators={}", scripId, graph.size());

        // Engine-side standardized exits (optional): ATR at entry drives
        // stop/target distances; per-position peak/trough drives trailing stops.
        float[] exitAtr = exitPolicy != null
                ? AtrSeries.compute(arrays, exitPolicy.atrPeriod()) : null;
        Map<Integer, Float> excursionPeak = exitPolicy != null
                ? new LinkedHashMap<>() : null;

        for (int bar = 0; bar < size; bar++) {
            open.push(arrays.open()[bar]);
            high.push(arrays.high()[bar]);
            low.push(arrays.low()[bar]);
            close.push(arrays.close()[bar]);
            volume.push(arrays.volume()[bar]);
            timestamp.push(arrays.timestamp()[bar]);

            // Push breadth values
            for (Map.Entry<String, FloatSmartValue> bEntry : breadthSmartValues.entrySet()) {
                float[] bArray = breadthAligned.get(bEntry.getKey());
                bEntry.getValue().push(bar < bArray.length ? bArray[bar] : Float.NaN);
            }

            // Push RS values
            for (Map.Entry<String, FloatSmartValue> rEntry : rsSmartValues.entrySet()) {
                float[] rArray = rsAligned.get(rEntry.getKey());
                rEntry.getValue().push(bar < rArray.length ? rArray[bar] : Float.NaN);
            }

            graph.updateAll();
            List<TradeRecord> fillTrades = processPendingSignals(
                    signalQueue, tracker, portfolio, arrays, bar);
            allTrades.addAll(fillTrades);
            strategy.invokeOnBar(bar, scripId);
            List<TradeRecord> currentBarTrades = processCurrentBarSignals(
                    signalQueue, tracker, portfolio, arrays, bar);
            allTrades.addAll(currentBarTrades);
            List<TradeRecord> policyTrades = exitPolicy != null
                    ? applyExitPolicy(tracker, portfolio, arrays, bar, exitAtr, excursionPeak)
                    : List.of();
            allTrades.addAll(policyTrades);
            if (!fillTrades.isEmpty() || !currentBarTrades.isEmpty() || !policyTrades.isEmpty()) {
                log.debug("Bar {} for {}: fills={}, currentBar={}, policy={}", bar, scripId,
                        fillTrades.size(), currentBarTrades.size(), policyTrades.size());
            }
            fireLifecycleEvents(scripId, bar, arrays, tracker, fillTrades, currentBarTrades);
            if (!policyTrades.isEmpty() && lifecycleCallback != null) {
                fireExitEvents(scripId, bar, arrays, policyTrades);
            }
            equityValues[bar] = portfolio.getCash() + tracker.unrealizedPnl(arrays.close()[bar]);
        }
        if (tracker.hasPositions()) {
            int lastBar = size - 1;
            List<TradeRecord> closeTrades = tracker.closeAll(
                    arrays.close()[lastBar], arrays.timestamp()[lastBar], lastBar);
            for (TradeRecord trade : closeTrades) {
                portfolio.addPnl(trade.getNetPnl());
                portfolio.adjustCash(trade.getNetPnl());
            }
            fireForceCloseEvents(scripId, lastBar, arrays, closeTrades);
            allTrades.addAll(closeTrades);
            equityValues[lastBar] = portfolio.getCash();
        }
        float totalNetPnl = 0f;
        for (TradeRecord trade : allTrades) {
            totalNetPnl += trade.getNetPnl();
        }
        EquityCurve curve = allTrades.isEmpty() ? null
                : new EquityCurve(equityValues, equityTimestamps, size);
        return ScripResult.builder()
                .scripId(scripId)
                .trades(allTrades)
                .equityCurve(curve)
                .totalNetPnl(totalNetPnl)
                .build();
    }

    private List<TradeRecord> processPendingSignals(SignalQueue queue, PositionTracker tracker,
                                                     PortfolioState portfolio, BarsArrays arrays,
                                                     int bar) {
        List<TradeRecord> trades = new ArrayList<>();
        List<QueuedSignal> exits = queue.drainExits();
        for (QueuedSignal signal : exits) {
            if (signal.getFillTiming() == FillTiming.NEXT_BAR) {
                trades.addAll(processFill(signal, tracker, portfolio, arrays, bar));
            }
        }
        List<QueuedSignal> entries = queue.drainEntries();
        for (QueuedSignal signal : entries) {
            if (signal.getFillTiming() == FillTiming.NEXT_BAR) {
                trades.addAll(processFill(signal, tracker, portfolio, arrays, bar));
            }
        }
        return trades;
    }

    private List<TradeRecord> processCurrentBarSignals(SignalQueue queue, PositionTracker tracker,
                                                        PortfolioState portfolio, BarsArrays arrays,
                                                        int bar) {
        List<TradeRecord> trades = new ArrayList<>();
        List<QueuedSignal> exits = queue.drainExits();
        for (QueuedSignal signal : exits) {
            if (signal.getFillTiming() == FillTiming.CURRENT_BAR) {
                trades.addAll(processFill(signal, tracker, portfolio, arrays, bar));
            } else {
                queue.requeueExit(signal);
            }
        }
        List<QueuedSignal> entries = queue.drainEntries();
        for (QueuedSignal signal : entries) {
            if (signal.getFillTiming() == FillTiming.CURRENT_BAR) {
                trades.addAll(processFill(signal, tracker, portfolio, arrays, bar));
            } else {
                queue.requeueEntry(signal);
            }
        }
        return trades;
    }

    private List<TradeRecord> processFill(QueuedSignal signal, PositionTracker tracker,
                                           PortfolioState portfolio, BarsArrays arrays, int bar) {
        List<TradeRecord> trades = new ArrayList<>();
        float o = arrays.open()[bar];
        float h = arrays.high()[bar];
        float l = arrays.low()[bar];
        float c = arrays.close()[bar];
        float rawPrice = signal.resolvePrice(o, h, l, c);
        long ts = arrays.timestamp()[bar];
        long vol = arrays.volume()[bar];
        if (signal.getType().isEntry()) {
            Side entrySide = signal.getType().entrySide();
            if (tracker.hasPositionsOnSide(entrySide.opposite())) {
                List<TradeRecord> reversalTrades = tracker.closeAllBySide(
                        entrySide.opposite(), rawPrice, ts, bar);
                for (TradeRecord trade : reversalTrades) {
                    portfolio.addPnl(trade.getNetPnl());
                    applyExitCash(portfolio, trade, entrySide.opposite());
                }
                trades.addAll(reversalTrades);
            }
            OpenPosition pos = tracker.openPosition(entrySide, signal.getQuantity(),
                    rawPrice, ts, bar, vol, volumeParticipationPercent);
            if (pos != null) {
                applyEntryCash(portfolio, pos);
            }
        } else {
            if (signal.getType() == SignalType.CLOSE_POSITION) {
                OpenPosition pos = tracker.findById(signal.getPositionId());
                if (pos != null) {
                    TradeRecord trade = tracker.closePosition(pos, rawPrice, ts, bar);
                    portfolio.addPnl(trade.getNetPnl());
                    applyExitCash(portfolio, trade, pos.getSide());
                    trades.add(trade);
                }
            } else {
                Side exitSide = signal.getType() == SignalType.LONG_EXIT ? Side.LONG : Side.SHORT;
                if (tracker.hasPositionsOnSide(exitSide)) {
                    List<TradeRecord> exitTrades = tracker.closeAllBySide(exitSide, rawPrice, ts, bar);
                    for (TradeRecord trade : exitTrades) {
                        portfolio.addPnl(trade.getNetPnl());
                        applyExitCash(portfolio, trade, exitSide);
                    }
                    trades.addAll(exitTrades);
                }
            }
        }
        return trades;
    }

    private void applyEntryCash(PortfolioState portfolio, OpenPosition pos) {
        if (pos.getSide() == Side.LONG) {
            portfolio.adjustCash(-pos.getEntryPrice() * pos.getQuantity());
        } else {
            portfolio.adjustCash(pos.getEntryPrice() * pos.getQuantity());
        }
    }

    private void applyExitCash(PortfolioState portfolio, TradeRecord trade, Side side) {
        if (side == Side.LONG) {
            portfolio.adjustCash(trade.getExitPrice() * trade.getQuantity());
        } else {
            portfolio.adjustCash(-trade.getExitPrice() * trade.getQuantity());
        }
    }

    /**
     * Evaluate engine-side standardized exits for every open position on this
     * bar. Check order is conservative: stop (initial or trailing, whichever
     * is tighter) → target → time stop. Gap-throughs fill at the open.
     */
    private List<TradeRecord> applyExitPolicy(PositionTracker tracker,
                                               PortfolioState portfolio,
                                               BarsArrays arrays, int bar,
                                               float[] atr, Map<Integer, Float> excursionPeak) {
        List<TradeRecord> trades = new ArrayList<>();
        float o = arrays.open()[bar];
        float h = arrays.high()[bar];
        float l = arrays.low()[bar];
        float c = arrays.close()[bar];
        for (OpenPosition pos : new ArrayList<>(tracker.getPositions())) {
            float atrAtEntry = pos.getEntryBarIndex() < atr.length
                    ? atr[pos.getEntryBarIndex()] : Float.NaN;
            Float exitPrice = resolvePolicyExit(pos, arrays, bar, o, h, l, c,
                    atrAtEntry, excursionPeak);
            if (exitPrice == null) {
                continue;
            }
            TradeRecord trade = tracker.closePosition(pos, exitPrice,
                    arrays.timestamp()[bar], bar);
            portfolio.addPnl(trade.getNetPnl());
            applyExitCash(portfolio, trade, pos.getSide());
            excursionPeak.remove(pos.getId());
            trades.add(trade);
        }
        return trades;
    }

    private Float resolvePolicyExit(OpenPosition pos, BarsArrays arrays, int bar,
                                     float o, float h, float l, float c,
                                     float atrAtEntry, Map<Integer, Float> excursionPeak) {
        boolean isLong = pos.getSide() == Side.LONG;
        float entry = pos.getEntryPrice();
        int barsInTrade = bar - pos.getEntryBarIndex();

        // Track the favorable extreme since entry for the trailing stop.
        float extreme = excursionPeak.getOrDefault(pos.getId(), entry);
        extreme = isLong ? Math.max(extreme, h) : Math.min(extreme, l);
        excursionPeak.put(pos.getId(), extreme);

        if (!Float.isNaN(atrAtEntry)) {
            float stopLevel = Float.NaN;
            if (exitPolicy.initialStopAtr() != null) {
                float d = (float) (exitPolicy.initialStopAtr() * atrAtEntry);
                stopLevel = isLong ? entry - d : entry + d;
            }
            if (exitPolicy.trailingStopAtr() != null) {
                float d = (float) (exitPolicy.trailingStopAtr() * atrAtEntry);
                float trail = isLong ? extreme - d : extreme + d;
                stopLevel = Float.isNaN(stopLevel)
                        ? trail
                        : (isLong ? Math.max(stopLevel, trail) : Math.min(stopLevel, trail));
            }
            if (!Float.isNaN(stopLevel)) {
                boolean hit = isLong ? l <= stopLevel : h >= stopLevel;
                if (hit) {
                    // Gap-through fills at the open rather than the stop price.
                    return isLong ? Math.min(o, stopLevel) : Math.max(o, stopLevel);
                }
            }
            if (exitPolicy.targetAtr() != null) {
                float d = (float) (exitPolicy.targetAtr() * atrAtEntry);
                float target = isLong ? entry + d : entry - d;
                boolean hit = isLong ? h >= target : l <= target;
                if (hit) {
                    return isLong ? Math.max(o, target) : Math.min(o, target);
                }
            }
        }
        if (exitPolicy.timeStopBars() != null && barsInTrade >= exitPolicy.timeStopBars()) {
            return c;
        }
        return null;
    }

    private void fireLifecycleEvents(String scripId, int bar, BarsArrays arrays,
                                     PositionTracker tracker,
                                     List<TradeRecord> fillTrades,
                                     List<TradeRecord> currentBarTrades) {
        if (lifecycleCallback == null) {
            return;
        }
        fireExitEvents(scripId, bar, arrays, fillTrades);
        fireExitEvents(scripId, bar, arrays, currentBarTrades);
        fireEntryAndInTradeEvents(scripId, bar, arrays, tracker);
    }

    private void fireEntryAndInTradeEvents(String scripId, int bar, BarsArrays arrays,
                                           PositionTracker tracker) {
        float currentClose = arrays.close()[bar];
        for (OpenPosition pos : tracker.getPositions()) {
            boolean isEntryBar = pos.getEntryBarIndex() == bar;
            TradeLifecyclePhase phase = isEntryBar
                    ? TradeLifecyclePhase.ENTRY : TradeLifecyclePhase.IN_TRADE;
            int barsInTrade = bar - pos.getEntryBarIndex();
            float unrealized = pos.unrealizedPnl(currentClose);
            TradeLifecycleEvent event = TradeLifecycleEvent.builder()
                    .phase(phase)
                    .scripId(scripId)
                    .barIndex(bar)
                    .arrays(arrays)
                    .side(pos.getSide())
                    .positionId(pos.getId())
                    .entryPrice(pos.getEntryPrice())
                    .entryTimestamp(pos.getEntryTimestamp())
                    .entryBarIndex(pos.getEntryBarIndex())
                    .quantity(pos.getQuantity())
                    .barsInTrade(barsInTrade)
                    .unrealizedPnl(unrealized)
                    .build();
            lifecycleCallback.onTradeEvent(event);
        }
    }

    private void fireExitEvents(String scripId, int bar, BarsArrays arrays,
                                List<TradeRecord> trades) {
        for (TradeRecord trade : trades) {
            TradeLifecycleEvent event = TradeLifecycleEvent.builder()
                    .phase(TradeLifecyclePhase.EXIT)
                    .scripId(scripId)
                    .barIndex(bar)
                    .arrays(arrays)
                    .side(trade.getSide())
                    .positionId(trade.getPositionId())
                    .entryPrice(trade.getEntryPrice())
                    .entryTimestamp(trade.getEntryTimestamp())
                    .entryBarIndex(trade.getEntryBarIndex())
                    .quantity(trade.getQuantity())
                    .barsInTrade(trade.getHoldingBars())
                    .unrealizedPnl(trade.getNetPnl())
                    .tradeRecord(trade)
                    .build();
            lifecycleCallback.onTradeEvent(event);
        }
    }

    private void fireForceCloseEvents(String scripId, int bar, BarsArrays arrays,
                                      List<TradeRecord> trades) {
        if (lifecycleCallback == null) {
            return;
        }
        fireExitEvents(scripId, bar, arrays, trades);
    }

    /**
     * Align RS values to the scrip's bar timestamps.
     * Uses linear scan since both arrays are sorted ascending.
     */
    private float[] alignRSToScrip(RSResult rs, BarsArrays arrays) {
        int size = arrays.size();
        float[] aligned = new float[size];
        long[] rTimestamps = rs.getTimestamps();
        float[] rValues = rs.getValues();
        int rIdx = 0;
        for (int i = 0; i < size; i++) {
            long ts = arrays.timestamp()[i];
            while (rIdx < rTimestamps.length && rTimestamps[rIdx] < ts) {
                rIdx++;
            }
            if (rIdx < rTimestamps.length && rTimestamps[rIdx] == ts) {
                aligned[i] = rValues[rIdx];
            } else {
                aligned[i] = Float.NaN;
            }
        }
        return aligned;
    }

    /**
     * Align breadth net values to the scrip's bar timestamps.
     * Uses linear scan since both arrays are sorted ascending.
     */
    private float[] alignBreadthToScrip(BreadthResult breadth, BarsArrays arrays) {
        int size = arrays.size();
        float[] aligned = new float[size];
        long[] bTimestamps = breadth.getTimestamps();
        float[] bNet = breadth.getNet();
        int bIdx = 0;
        for (int i = 0; i < size; i++) {
            long ts = arrays.timestamp()[i];
            while (bIdx < bTimestamps.length && bTimestamps[bIdx] < ts) {
                bIdx++;
            }
            if (bIdx < bTimestamps.length && bTimestamps[bIdx] == ts) {
                aligned[i] = bNet[bIdx];
            } else {
                aligned[i] = Float.NaN;
            }
        }
        return aligned;
    }

}
