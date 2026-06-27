package com.whiteowl.core.backtest.engine;

import com.whiteowl.core.backtest.dsl.FillTiming;
import com.whiteowl.core.backtest.dsl.Signal;
import com.whiteowl.core.backtest.dsl.SignalType;
import com.whiteowl.core.backtest.model.BacktestResult;
import com.whiteowl.core.backtest.model.EquityCurve;
import com.whiteowl.core.backtest.model.TradeRecord;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.order.model.OrderSide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PortfolioBacktestEngine {

    private static final float HUNDRED = 100f;
    private static final String PORTFOLIO_SCRIP_ID = "PORTFOLIO";

    private final float initialCapital;
    private final float costPercent;
    private final float slippagePercent;
    private final float positionSizePercent;
    private final float minPrice;
    private final float volumeParticipationPercent;

    public PortfolioBacktestEngine(float initialCapital, float costPercent, float slippagePercent,
                                   float positionSizePercent, float minPrice,
                                   float volumeParticipationPercent) {
        this.initialCapital = initialCapital;
        this.costPercent = costPercent;
        this.slippagePercent = slippagePercent;
        this.positionSizePercent = positionSizePercent;
        this.minPrice = minPrice;
        this.volumeParticipationPercent = volumeParticipationPercent;
    }

    public List<BacktestResult> run(Map<String, ScripData> scripDataMap) {
        List<ScripSignal> allSignals = collectAndSortSignals(scripDataMap);
        float capital = initialCapital;
        Map<String, PositionTracker> trackers = new HashMap<>();
        List<TradeRecord> allTrades = new ArrayList<>();
        List<Float> equitySnapshots = new ArrayList<>();
        List<Long> equityTimestamps = new ArrayList<>();
        equitySnapshots.add(initialCapital);
        equityTimestamps.add(findEarliestTimestamp(scripDataMap));
        for (ScripSignal ss : allSignals) {
            String scripId = ss.scripId();
            Signal signal = ss.signal();
            BarsArrays arrays = ss.arrays();
            SignalType type = signal.type();
            boolean currentBar = signal.fillTiming() == FillTiming.CURRENT_BAR;
            int fillBar = currentBar ? signal.barIndex() : signal.barIndex() + 1;
            if (fillBar >= arrays.size()) continue;
            float fillPrice = signal.fillPrice().resolve(arrays, fillBar);
            long fillTimestamp = arrays.timestamp()[fillBar];
            long fillVolume = arrays.volume()[fillBar];
            PositionTracker tracker = trackers.computeIfAbsent(scripId, k -> new PositionTracker());
            if (isEntrySignal(type) && !tracker.hasOpenPosition()) {
                capital = openPosition(type, fillBar, fillPrice, fillTimestamp, fillVolume,
                        tracker, capital);
            } else if (isExitSignal(type, tracker)) {
                capital = closePosition(scripId, fillBar, fillPrice, fillTimestamp,
                        tracker, allTrades, capital);
            }
            equitySnapshots.add(capital);
            equityTimestamps.add(fillTimestamp);
        }
        capital = forceCloseOpenPositions(scripDataMap, trackers, allTrades, capital);
        equitySnapshots.add(capital);
        equityTimestamps.add(findLatestTimestamp(scripDataMap));
        return List.of(buildPortfolioResult(allTrades, equitySnapshots, equityTimestamps));
    }

    private List<ScripSignal> collectAndSortSignals(Map<String, ScripData> scripDataMap) {
        List<ScripSignal> allSignals = new ArrayList<>();
        for (Map.Entry<String, ScripData> entry : scripDataMap.entrySet()) {
            String scripId = entry.getKey();
            ScripData data = entry.getValue();
            BarsArrays arrays = data.arrays();
            for (Signal signal : data.signals()) {
                boolean currentBar = signal.fillTiming() == FillTiming.CURRENT_BAR;
                int fillBar = currentBar ? signal.barIndex() : signal.barIndex() + 1;
                if (fillBar >= arrays.size()) continue;
                long timestamp = arrays.timestamp()[fillBar];
                allSignals.add(new ScripSignal(scripId, signal, arrays, timestamp));
            }
        }
        Collections.sort(allSignals);
        return allSignals;
    }

    private float openPosition(SignalType type, int fillBar, float fillPrice, long fillTimestamp,
                               long fillVolume, PositionTracker tracker, float capital) {
        OrderSide side = type == SignalType.LONG_ENTRY ? OrderSide.BUY : OrderSide.SELL;
        float slippageDirection = side == OrderSide.BUY ? 1f : -1f;
        float entryPrice = fillPrice * (1f + slippageDirection * slippagePercent / HUNDRED);
        if (entryPrice < minPrice) return capital;
        float allocatedCapital = capital * positionSizePercent / HUNDRED;
        int volumeLimit = (int) (fillVolume * volumeParticipationPercent / HUNDRED);
        int quantity = Math.min((int) (allocatedCapital / entryPrice), volumeLimit);
        if (quantity <= 0) return capital;
        tracker.openPosition(side, fillBar, entryPrice, fillTimestamp, quantity);
        float invested = entryPrice * quantity;
        return capital - invested;
    }

    private float closePosition(String scripId, int fillBar, float fillPrice, long fillTimestamp,
                                PositionTracker tracker, List<TradeRecord> trades, float capital) {
        float slippageDirection = tracker.getSide() == OrderSide.BUY ? -1f : 1f;
        float exitPrice = fillPrice * (1f + slippageDirection * slippagePercent / HUNDRED);
        TradeRecord trade = tracker.closePosition(scripId, fillBar, exitPrice, fillTimestamp, costPercent);
        trades.add(trade);
        float invested = trade.getEntryPrice() * trade.getQuantity();
        return capital + invested + trade.getNetPnl();
    }

    private float forceCloseOpenPositions(Map<String, ScripData> scripDataMap,
                                          Map<String, PositionTracker> trackers,
                                          List<TradeRecord> allTrades, float capital) {
        for (Map.Entry<String, PositionTracker> entry : trackers.entrySet()) {
            PositionTracker tracker = entry.getValue();
            if (!tracker.hasOpenPosition()) continue;
            String scripId = entry.getKey();
            ScripData data = scripDataMap.get(scripId);
            BarsArrays arrays = data.arrays();
            int lastBar = arrays.size() - 1;
            capital = closePosition(scripId, lastBar, arrays.close()[lastBar],
                    arrays.timestamp()[lastBar], tracker, allTrades, capital);
        }
        return capital;
    }

    private BacktestResult buildPortfolioResult(List<TradeRecord> trades,
                                                List<Float> equitySnapshots,
                                                List<Long> timestamps) {
        int size = equitySnapshots.size();
        float[] values = new float[size];
        long[] ts = new long[size];
        for (int i = 0; i < size; i++) {
            values[i] = equitySnapshots.get(i);
            ts[i] = timestamps.get(i);
        }
        EquityCurve curve = new EquityCurve(values, ts, size);
        return new BacktestResult(PORTFOLIO_SCRIP_ID, trades, curve);
    }

    private static long findEarliestTimestamp(Map<String, ScripData> scripDataMap) {
        long earliest = Long.MAX_VALUE;
        for (ScripData data : scripDataMap.values()) {
            if (data.arrays().size() > 0) {
                earliest = Math.min(earliest, data.arrays().timestamp()[0]);
            }
        }
        return earliest == Long.MAX_VALUE ? 0L : earliest;
    }

    private static long findLatestTimestamp(Map<String, ScripData> scripDataMap) {
        long latest = Long.MIN_VALUE;
        for (ScripData data : scripDataMap.values()) {
            int size = data.arrays().size();
            if (size > 0) {
                latest = Math.max(latest, data.arrays().timestamp()[size - 1]);
            }
        }
        return latest == Long.MIN_VALUE ? 0L : latest;
    }

    private boolean isEntrySignal(SignalType type) {
        return type == SignalType.LONG_ENTRY || type == SignalType.SHORT_ENTRY;
    }

    private boolean isExitSignal(SignalType type, PositionTracker tracker) {
        if (!tracker.hasOpenPosition()) return false;
        return type == SignalType.LONG_EXIT || type == SignalType.SHORT_EXIT;
    }

}
