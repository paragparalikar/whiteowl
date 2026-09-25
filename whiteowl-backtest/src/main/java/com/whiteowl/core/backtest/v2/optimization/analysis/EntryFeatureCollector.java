package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.engine.AtrSeries;
import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleCallback;
import com.whiteowl.core.backtest.v2.feature.TradeLifecycleEvent;
import com.whiteowl.core.backtest.v2.feature.TradeLifecyclePhase;
import com.whiteowl.core.backtest.v2.model.TradeRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures a {@link TradeObservation} per trade: indicator features evaluated
 * at the signal bar (fill bar − 1, avoiding look-ahead) joined with the
 * trade outcome at exit. Not thread-safe — create one per engine run.
 */
public final class EntryFeatureCollector implements TradeLifecycleCallback {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MARKET_OPEN_MINUTES = 9 * 60 + 15;

    private final int atrPeriod;
    private final int rsiPeriod;
    private final int adxPeriod;
    private final int rocPeriod;
    private final int volPeriod;
    private final double riskAtrMultiple;

    private final Map<Integer, float[]> pending = new HashMap<>();
    private final List<TradeObservation> observations = new ArrayList<>();
    private Object boundArrays;
    private float[] atr, rsi, roc, volRatio, atrRatio, adx;

    /**
     * @param riskAtrMultiple ATR multiple defining initial risk for the
     *                        R-multiple (typically the selected stop). 0 → R
     *                        multiples reported as NaN.
     */
    public EntryFeatureCollector(int atrPeriod, int rsiPeriod, int adxPeriod,
                                  int rocPeriod, int volPeriod, double riskAtrMultiple) {
        this.atrPeriod = atrPeriod;
        this.rsiPeriod = rsiPeriod;
        this.adxPeriod = adxPeriod;
        this.rocPeriod = rocPeriod;
        this.volPeriod = volPeriod;
        this.riskAtrMultiple = riskAtrMultiple;
    }

    public List<TradeObservation> observations() {
        return observations;
    }

    @Override
    public void onTradeEvent(TradeLifecycleEvent e) {
        if (e.getPhase() == TradeLifecyclePhase.ENTRY) {
            ensureSeries(e);
            int signalBar = Math.max(0, e.getEntryBarIndex() - 1);
            pending.put(e.getPositionId(), new float[]{
                    at(adx, signalBar), at(rsi, signalBar), at(roc, signalBar),
                    at(volRatio, signalBar), at(atrRatio, signalBar), at(atr, signalBar)});
        } else if (e.getPhase() == TradeLifecyclePhase.EXIT) {
            float[] f = pending.remove(e.getPositionId());
            TradeRecord t = e.getTradeRecord();
            if (f == null || t == null) return;
            observations.add(build(e, t, f));
        }
    }

    private TradeObservation build(TradeLifecycleEvent e, TradeRecord t, float[] f) {
        float riskPrice = f[5] * (float) riskAtrMultiple;
        float signedMove = e.getSide() == Side.LONG
                ? t.getExitPrice() - t.getEntryPrice()
                : t.getEntryPrice() - t.getExitPrice();
        float r = riskPrice > 0 ? signedMove / riskPrice : Float.NaN;

        Instant entry = Instant.ofEpochMilli(t.getEntryTimestamp());
        LocalTime tod = entry.atZone(IST).toLocalTime();
        LocalDate date = entry.atZone(IST).toLocalDate();
        return new TradeObservation(
                t.getPositionId(), t.getScripId(), t.getSide(),
                t.getEntryBarIndex(), t.getExitBarIndex(),
                t.getEntryTimestamp(), t.getExitTimestamp(), t.getHoldingBars(),
                t.getNetPnl(), t.getNetPnlPercent(), r,
                f[0], f[1], f[2], f[3], f[4], f[5],
                tod.get(ChronoField.MINUTE_OF_DAY) - MARKET_OPEN_MINUTES,
                date.getDayOfWeek().getValue(),
                (date.getDayOfMonth() - 1) / 7 + 1,
                date.getMonthValue());
    }

    private void ensureSeries(TradeLifecycleEvent e) {
        if (boundArrays == e.getArrays()) return;
        boundArrays = e.getArrays();
        atr = AtrSeries.compute(e.getArrays(), atrPeriod);
        rsi = IndicatorSeries.rsi(e.getArrays().close(), rsiPeriod);
        roc = IndicatorSeries.roc(e.getArrays().close(), rocPeriod);
        adx = IndicatorSeries.adx(e.getArrays(), adxPeriod);
        float[] sd = IndicatorSeries.stdDev(e.getArrays().close(), volPeriod);
        float[] ma = IndicatorSeries.sma(e.getArrays().close(), volPeriod);
        int n = e.getArrays().size();
        volRatio = new float[n];
        atrRatio = new float[n];
        for (int i = 0; i < n; i++) {
            boolean ok = !Float.isNaN(ma[i]) && ma[i] != 0;
            volRatio[i] = ok && !Float.isNaN(sd[i]) ? sd[i] / ma[i] : Float.NaN;
            atrRatio[i] = ok && !Float.isNaN(atr[i]) ? atr[i] / ma[i] : Float.NaN;
        }
    }

    private float at(float[] series, int i) {
        return series != null && i >= 0 && i < series.length ? series[i] : Float.NaN;
    }

}
