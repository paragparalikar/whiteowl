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
import java.util.Comparator;
import java.util.List;

public final class BacktestEngine {

    private static final float HUNDRED = 100f;

    private final float initialCapital;
    private final float costPercent;
    private final float slippagePercent;
    private final float positionSizePercent;
    private final float minPrice;
    private final float volumeParticipationPercent;

    public BacktestEngine(float initialCapital, float costPercent, float slippagePercent,
                          float positionSizePercent, float minPrice,
                          float volumeParticipationPercent) {
        this.initialCapital = initialCapital;
        this.costPercent = costPercent;
        this.slippagePercent = slippagePercent;
        this.positionSizePercent = positionSizePercent;
        this.minPrice = minPrice;
        this.volumeParticipationPercent = volumeParticipationPercent;
    }

    public BacktestResult run(String scripId, BarsArrays arrays, List<Signal> signals) {
        int size = arrays.size();
        float[] equityValues = new float[size];
        long[] equityTimestamps = arrays.timestamp();
        float capital = initialCapital;
        List<Signal> sorted = new ArrayList<>(signals);
        sorted.sort(Comparator.comparingInt(Signal::barIndex));
        PositionTracker tracker = new PositionTracker();
        List<TradeRecord> trades = new ArrayList<>();
        float[] pendingPnl = new float[size];
        int signalIdx = 0;
        for (int bar = 0; bar < size; bar++) {
            capital += pendingPnl[bar];
            while (signalIdx < sorted.size() && sorted.get(signalIdx).barIndex() < bar) {
                signalIdx++;
            }
            while (signalIdx < sorted.size() && sorted.get(signalIdx).barIndex() == bar) {
                Signal signal = sorted.get(signalIdx);
                capital = processSignal(signal, bar, scripId, arrays, tracker, trades, capital, pendingPnl);
                signalIdx++;
            }
            equityValues[bar] = capital;
        }
        if (tracker.hasOpenPosition()) {
            int lastBar = size - 1;
            TradeRecord trade = tracker.closePosition(scripId, lastBar,
                    arrays.close()[lastBar], arrays.timestamp()[lastBar], costPercent);
            trades.add(trade);
            capital += trade.getNetPnl();
            equityValues[lastBar] = capital;
        }
        EquityCurve curve = new EquityCurve(equityValues, equityTimestamps, size);
        return new BacktestResult(scripId, trades, curve);
    }

    private float processSignal(Signal signal, int bar, String scripId, BarsArrays arrays,
                                PositionTracker tracker, List<TradeRecord> trades, float capital,
                                float[] pendingPnl) {
        SignalType type = signal.type();
        boolean currentBar = signal.fillTiming() == FillTiming.CURRENT_BAR;
        int fillBar = currentBar ? bar : bar + 1;
        if (fillBar >= arrays.size()) return capital;
        float fillPrice = signal.fillPrice().resolve(arrays, fillBar);
        long fillTimestamp = arrays.timestamp()[fillBar];
        long fillVolume = arrays.volume()[fillBar];
        if (isEntrySignal(type) && !tracker.hasOpenPosition()) {
            return openPosition(type, fillBar, fillPrice, fillTimestamp, fillVolume, tracker, capital);
        }
        if (isExitSignal(type, tracker)) {
            return closePosition(scripId, fillBar, fillPrice, fillTimestamp, currentBar,
                    tracker, trades, capital, pendingPnl);
        }
        return capital;
    }

    private float openPosition(SignalType type, int fillBar, float fillOpen, long fillTimestamp,
                               long fillVolume, PositionTracker tracker, float capital) {
        OrderSide side = type == SignalType.LONG_ENTRY ? OrderSide.BUY : OrderSide.SELL;
        float slippageDirection = side == OrderSide.BUY ? 1f : -1f;
        float entryPrice = fillOpen * (1f + slippageDirection * slippagePercent / HUNDRED);
        if (entryPrice < minPrice) return capital;
        float allocatedCapital = capital * positionSizePercent / HUNDRED;
        int volumeLimit = (int) (fillVolume * volumeParticipationPercent / HUNDRED);
        int quantity = Math.min((int) (allocatedCapital / entryPrice), volumeLimit);
        if (quantity <= 0) return capital;
        tracker.openPosition(side, fillBar, entryPrice, fillTimestamp, quantity);
        return capital;
    }

    private float closePosition(String scripId, int fillBar, float fillPrice, long fillTimestamp,
                                boolean currentBar, PositionTracker tracker,
                                List<TradeRecord> trades, float capital, float[] pendingPnl) {
        float slippageDirection = tracker.getSide() == OrderSide.BUY ? -1f : 1f;
        float exitPrice = fillPrice * (1f + slippageDirection * slippagePercent / HUNDRED);
        TradeRecord trade = tracker.closePosition(scripId, fillBar, exitPrice, fillTimestamp, costPercent);
        trades.add(trade);
        if (currentBar) {
            return capital + trade.getNetPnl();
        }
        pendingPnl[fillBar] += trade.getNetPnl();
        return capital;
    }

    private boolean isEntrySignal(SignalType type) {
        return type == SignalType.LONG_ENTRY || type == SignalType.SHORT_ENTRY;
    }

    private boolean isExitSignal(SignalType type, PositionTracker tracker) {
        if (!tracker.hasOpenPosition()) return false;
        return type == SignalType.LONG_EXIT || type == SignalType.SHORT_EXIT;
    }

}
