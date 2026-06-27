package com.whiteowl.core.backtest.dsl;

import com.whiteowl.core.backtest.model.StrategyInput;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import com.whiteowl.core.indicator.script.GroovyIndicator;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRegistry;
import com.whiteowl.core.script.ScriptRepository;
import groovy.lang.Script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.script.ScriptType.INDICATOR;

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
    private ScriptRegistry scriptRegistry;

    public void setDiscoveryMode(boolean discoveryMode) {
        this.discoveryMode = discoveryMode;
    }

    public void setScriptRegistry(ScriptRegistry scriptRegistry) {
        this.scriptRegistry = scriptRegistry;
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
        return IndicatorFunctions.sma(source, barCount, period);
    }

    public float[] ema(float[] source, int period) {
        return IndicatorFunctions.ema(source, barCount, period);
    }

    public float[] rsi(float[] source, int period) {
        return IndicatorFunctions.rsi(source, barCount, period);
    }

    public float[] atr(int period) {
        return IndicatorFunctions.atr(high, low, close, barCount, period);
    }

    public float[][] macd(float[] source, int fastPeriod, int slowPeriod, int signalPeriod) {
        return IndicatorFunctions.macd(source, barCount, fastPeriod, slowPeriod, signalPeriod);
    }

    public boolean[] crossover(float[] a, float[] b) {
        return IndicatorFunctions.crossover(a, b, barCount);
    }

    public boolean[] crossunder(float[] a, float[] b) {
        return IndicatorFunctions.crossunder(a, b, barCount);
    }

    public float[] highest(float[] source, int period) {
        return IndicatorFunctions.highest(source, barCount, period);
    }

    public float[] lowest(float[] source, int period) {
        return IndicatorFunctions.lowest(source, barCount, period);
    }

    public float[][] stochastics(int kPeriod, int dPeriod) {
        return IndicatorFunctions.stochastics(high, low, close, barCount, kPeriod, dPeriod);
    }

    public float[] obv() {
        return IndicatorFunctions.obv(close, volume, barCount);
    }

    public float[] stddev(float[] source, int period) {
        return IndicatorFunctions.stddev(source, barCount, period);
    }

    public float[] roc(float[] source, int period) {
        return IndicatorFunctions.roc(source, barCount, period);
    }

    public float[][] adx(int period) {
        return IndicatorFunctions.adx(high, low, close, barCount, period);
    }

    public float[][] supertrend(int period, float multiplier) {
        return IndicatorFunctions.supertrend(high, low, close, barCount, period, multiplier);
    }

    public float[][] bollingerBands(float[] source, int period, float multiplier) {
        return IndicatorFunctions.bollingerBands(source, barCount, period, multiplier);
    }

    public float[] cci(int period) {
        return IndicatorFunctions.cci(high, low, close, barCount, period);
    }

    public float[] williamsR(int period) {
        return IndicatorFunctions.williamsR(high, low, close, barCount, period);
    }

    public float[][] stochRsi(int rsiPeriod, int stochPeriod, int kSmooth, int dSmooth) {
        return IndicatorFunctions.stochRsi(close, barCount, rsiPeriod, stochPeriod, kSmooth, dSmooth);
    }

    public float[] ultimateOscillator(int period1, int period2, int period3) {
        return IndicatorFunctions.ultimateOscillator(high, low, close, barCount, period1, period2, period3);
    }

    public float[][] parabolicSar(float afStart, float afStep, float afMax) {
        return IndicatorFunctions.parabolicSar(high, low, close, barCount, afStart, afStep, afMax);
    }

    public float[][] ichimoku(int tenkan, int kijun, int senkouB, int displacement) {
        return IndicatorFunctions.ichimoku(high, low, close, barCount, tenkan, kijun, senkouB, displacement);
    }

    public float[][] aroon(int period) {
        return IndicatorFunctions.aroon(high, low, barCount, period);
    }

    public float[][] keltnerChannels(int emaPeriod, int atrPeriod, float multiplier) {
        return IndicatorFunctions.keltnerChannels(high, low, close, barCount, emaPeriod, atrPeriod, multiplier);
    }

    public float[][] donchianChannels(int period) {
        return IndicatorFunctions.donchianChannels(high, low, barCount, period);
    }

    public float[] accumulationDistribution() {
        return IndicatorFunctions.accumulationDistribution(high, low, close, volume, barCount);
    }

    public float[] cmf(int period) {
        return IndicatorFunctions.cmf(high, low, close, volume, barCount, period);
    }

    public float[] mfi(int period) {
        return IndicatorFunctions.mfi(high, low, close, volume, barCount, period);
    }

    public float[] vwap() {
        return IndicatorFunctions.vwap(high, low, close, volume, barCount);
    }

    public float[] indicator(String name) {
        if (scriptRegistry == null) {
            throw new IllegalStateException("ScriptRegistry not configured");
        }
        ScriptDescriptor descriptor = scriptRegistry.findScript(INDICATOR, name)
                .orElseThrow(() -> new IllegalArgumentException("Indicator script not found: " + name));
        ScriptRepository repository = scriptRegistry.getRepository(INDICATOR)
                .orElseThrow(() -> new IllegalStateException("Indicator repository not configured"));
        try {
            return new GroovyIndicator(descriptor, repository)
                    .compute(new BarsArrays(open, high, low, close, volume, timestamps, barCount));
        } catch (Exception e) {
            throw new RuntimeException("Indicator script '" + name + "' failed: " + e.getMessage(), e);
        }
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
