package com.whiteowl.core.backtest.v2.optimization.exit;

import com.whiteowl.core.backtest.v2.engine.AtrSeries;
import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleEvent;
import com.whiteowl.core.backtest.v2.model.TradeRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects {@link Excursion} records via trade-lifecycle events. Create one
 * instance per backtest run and pass it to the engine's lifecycle callback —
 * it is not thread-safe by design (a run is single-threaded).
 */
public final class ExcursionCollector implements TradeLifecycleCallback {

    private final int atrPeriod;
    private final Map<Integer, Tracker> open = new HashMap<>();
    private final List<Excursion> excursions = new ArrayList<>();
    private float[] atr;            // lazily computed for the current run
    private Object boundArrays;     // identity of the arrays atr belongs to

    public ExcursionCollector(int atrPeriod) {
        this.atrPeriod = atrPeriod;
    }

    public List<Excursion> excursions() {
        return excursions;
    }

    @Override
    public void onTradeEvent(TradeLifecycleEvent e) {
        switch (e.getPhase()) {
            case ENTRY -> open.put(e.getPositionId(), new Tracker());
            case IN_TRADE -> update(e);
            case EXIT -> {
                update(e);
                finalize(e);
            }
        }
    }

    private void update(TradeLifecycleEvent e) {
        Tracker t = open.get(e.getPositionId());
        if (t == null) return;
        int bar = e.getBarIndex();
        float low = e.getArrays().low()[bar];
        float high = e.getArrays().high()[bar];
        if (low < t.minLow) {
            t.minLow = low;
            t.minLowBar = bar;
        }
        if (high > t.maxHigh) {
            t.maxHigh = high;
            t.maxHighBar = bar;
        }
    }

    private void finalize(TradeLifecycleEvent e) {
        Tracker t = open.remove(e.getPositionId());
        TradeRecord trade = e.getTradeRecord();
        if (t == null || trade == null) return;
        if (atr == null || boundArrays != e.getArrays()) {
            atr = AtrSeries.compute(e.getArrays(), atrPeriod);
            boundArrays = e.getArrays();
        }
        float entryAtr = e.getEntryBarIndex() < atr.length ? atr[e.getEntryBarIndex()] : Float.NaN;
        boolean isLong = e.getSide() == Side.LONG;
        // Spec convention: adverse excursions are negative for both sides.
        float maePrice = isLong
                ? t.minLow - e.getEntryPrice()
                : e.getEntryPrice() - t.maxHigh;
        float mfePrice = isLong
                ? t.maxHigh - e.getEntryPrice()
                : e.getEntryPrice() - t.minLow;
        float maeAtr = Float.isNaN(entryAtr) || entryAtr <= 0
                ? Float.NaN : Math.abs(maePrice) / entryAtr;
        float mfeAtr = Float.isNaN(entryAtr) || entryAtr <= 0
                ? Float.NaN : Math.abs(mfePrice) / entryAtr;
        excursions.add(new Excursion(
                e.getPositionId(), e.getScripId(), e.getSide(), e.getEntryPrice(),
                e.getEntryBarIndex(), trade.getExitBarIndex(), entryAtr,
                maePrice, mfePrice, maeAtr, mfeAtr,
                t.minLowBar - e.getEntryBarIndex(),
                t.maxHighBar - e.getEntryBarIndex()));
    }

    private static final class Tracker {
        float minLow = Float.POSITIVE_INFINITY;
        float maxHigh = Float.NEGATIVE_INFINITY;
        int minLowBar = -1;
        int maxHighBar = -1;
    }

}
