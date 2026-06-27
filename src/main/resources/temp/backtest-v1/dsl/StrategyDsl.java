package com.whiteowl.core.backtest.dsl;

import com.whiteowl.core.backtest.model.StrategyInput;
import com.whiteowl.core.backtest.indicator.Atr;
import com.whiteowl.core.backtest.indicator.Crossover;
import com.whiteowl.core.backtest.indicator.Crossunder;
import com.whiteowl.core.backtest.indicator.Ema;
import com.whiteowl.core.backtest.indicator.Highest;
import com.whiteowl.core.backtest.indicator.Lowest;
import com.whiteowl.core.backtest.indicator.Macd;
import com.whiteowl.core.backtest.indicator.Rsi;
import com.whiteowl.core.backtest.indicator.Sma;
import com.whiteowl.core.bar.model.BarsArrays;
import groovy.lang.Script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.backtest.dsl.SignalType.LONG_ENTRY;
import static com.whiteowl.core.backtest.dsl.SignalType.LONG_EXIT;
import static com.whiteowl.core.backtest.dsl.SignalType.SHORT_ENTRY;
import static com.whiteowl.core.backtest.dsl.SignalType.SHORT_EXIT;

public abstract class StrategyDsl extends Script {

    protected float[] open;
    protected float[] high;
    protected float[] low;
    protected float[] close;
    protected long[] volume;
    protected long[] timestamps;
    protected int barCount;
    private final List<Signal> signals = new ArrayList<>();
    private final List<StrategyInput> declaredInputs = new ArrayList<>();
    private Map<String, Number> inputOverrides = Map.of();
    private boolean discoveryMode;

    public void setDiscoveryMode(boolean discoveryMode) {
        this.discoveryMode = discoveryMode;
    }

    public void setInputOverrides(Map<String, Number> overrides) {
        this.inputOverrides = overrides != null ? overrides : Map.of();
    }

    public Number input(String name, Number defaultValue) {
        declaredInputs.add(new StrategyInput(name, defaultValue));
        if (discoveryMode) return defaultValue;
        return inputOverrides.getOrDefault(name, defaultValue);
    }

    public Number input(String name, Number defaultValue, Number minValue, Number maxValue) {
        declaredInputs.add(new StrategyInput(name, defaultValue, minValue, maxValue));
        if (discoveryMode) return defaultValue;
        return clampAndSnap(inputOverrides.getOrDefault(name, defaultValue), minValue, maxValue, null);
    }

    public Number input(String name, Number defaultValue, Number minValue, Number maxValue, Number step) {
        declaredInputs.add(new StrategyInput(name, defaultValue, minValue, maxValue, step));
        if (discoveryMode) return defaultValue;
        return clampAndSnap(inputOverrides.getOrDefault(name, defaultValue), minValue, maxValue, step);
    }

    private static Number clampAndSnap(Number value, Number min, Number max, Number step) {
        double v = value.doubleValue();
        if (step != null && min != null) {
            double s = step.doubleValue();
            double base = min.doubleValue();
            v = base + Math.round((v - base) / s) * s;
        }
        if (min != null && v < min.doubleValue()) v = min.doubleValue();
        if (max != null && v > max.doubleValue()) v = max.doubleValue();
        if (value instanceof Integer) return (int) v;
        return v;
    }

    public List<StrategyInput> getDeclaredInputs() {
        return Collections.unmodifiableList(declaredInputs);
    }

    public void clearInputs() {
        declaredInputs.clear();
    }

    public void bind(BarsArrays arrays) {
        this.open = arrays.open();
        this.high = arrays.high();
        this.low = arrays.low();
        this.close = arrays.close();
        this.volume = arrays.volume();
        this.timestamps = arrays.timestamp();
        this.barCount = arrays.size();
        signals.clear();
    }

    public List<Signal> getSignals() {
        return Collections.unmodifiableList(signals);
    }

    public float[] sma(float[] source, int period) {
        return Sma.compute(source, barCount, period);
    }

    public float[] ema(float[] source, int period) {
        return Ema.compute(source, barCount, period);
    }

    public float[] rsi(float[] source, int period) {
        return Rsi.compute(source, barCount, period);
    }

    public float[] atr(int period) {
        return Atr.compute(high, low, close, barCount, period);
    }

    public float[][] macd(float[] source, int fastPeriod, int slowPeriod, int signalPeriod) {
        return Macd.compute(source, barCount, fastPeriod, slowPeriod, signalPeriod);
    }

    public boolean[] crossover(float[] a, float[] b) {
        return Crossover.compute(a, b, barCount);
    }

    public boolean[] crossunder(float[] a, float[] b) {
        return Crossunder.compute(a, b, barCount);
    }

    public float[] highest(float[] source, int period) {
        return Highest.compute(source, barCount, period);
    }

    public float[] lowest(float[] source, int period) {
        return Lowest.compute(source, barCount, period);
    }

    public void longEntry(int barIndex) {
        signals.add(new Signal(LONG_ENTRY, barIndex));
    }

    public void longEntry(int barIndex, FillTiming timing, FillPrice price) {
        signals.add(new Signal(LONG_ENTRY, barIndex, timing, price));
    }

    public void longExit(int barIndex) {
        signals.add(new Signal(LONG_EXIT, barIndex));
    }

    public void longExit(int barIndex, FillTiming timing, FillPrice price) {
        signals.add(new Signal(LONG_EXIT, barIndex, timing, price));
    }

    public void shortEntry(int barIndex) {
        signals.add(new Signal(SHORT_ENTRY, barIndex));
    }

    public void shortEntry(int barIndex, FillTiming timing, FillPrice price) {
        signals.add(new Signal(SHORT_ENTRY, barIndex, timing, price));
    }

    public void shortExit(int barIndex) {
        signals.add(new Signal(SHORT_EXIT, barIndex));
    }

    public void shortExit(int barIndex, FillTiming timing, FillPrice price) {
        signals.add(new Signal(SHORT_EXIT, barIndex, timing, price));
    }

}
