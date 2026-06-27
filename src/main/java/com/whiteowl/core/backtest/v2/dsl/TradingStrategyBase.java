package com.whiteowl.core.backtest.v2.dsl;

import com.whiteowl.core.backtest.v2.engine.FillPrice;
import com.whiteowl.core.backtest.v2.engine.FillTiming;
import com.whiteowl.core.backtest.v2.engine.OpenPosition;
import com.whiteowl.core.backtest.v2.engine.PortfolioState;
import com.whiteowl.core.backtest.v2.engine.PositionTracker;
import com.whiteowl.core.backtest.v2.engine.QueuedSignal;
import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.engine.SignalQueue;
import com.whiteowl.core.backtest.v2.engine.SignalType;
import com.whiteowl.core.backtest.v2.indicator.AdxIndicator;
import com.whiteowl.core.backtest.v2.indicator.AtrIndicator;
import com.whiteowl.core.backtest.v2.indicator.BollingerBandsIndicator;
import com.whiteowl.core.backtest.v2.indicator.EmaIndicator;
import com.whiteowl.core.backtest.v2.indicator.HighestIndicator;
import com.whiteowl.core.backtest.v2.indicator.Indicator;
import com.whiteowl.core.backtest.v2.indicator.LowestIndicator;
import com.whiteowl.core.backtest.v2.indicator.MacdIndicator;
import com.whiteowl.core.backtest.v2.indicator.RocIndicator;
import com.whiteowl.core.backtest.v2.indicator.RsiIndicator;
import com.whiteowl.core.backtest.v2.indicator.SmaIndicator;
import com.whiteowl.core.backtest.v2.indicator.StdDevIndicator;
import com.whiteowl.core.backtest.v2.indicator.SwingHighLowIndicator;
import com.whiteowl.core.backtest.v2.indicator.VolumeSmaIndicator;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.IndicatorGraph;
import com.whiteowl.core.backtest.v2.smartvalue.IndicatorNode;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;
import groovy.lang.Script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.backtest.v2.engine.FillPrice.OPEN;
import static com.whiteowl.core.backtest.v2.engine.FillTiming.NEXT_BAR;
import static com.whiteowl.core.backtest.v2.engine.SignalType.CLOSE_POSITION;
import static com.whiteowl.core.backtest.v2.engine.SignalType.LONG_ENTRY;
import static com.whiteowl.core.backtest.v2.engine.SignalType.LONG_EXIT;
import static com.whiteowl.core.backtest.v2.engine.SignalType.SHORT_ENTRY;
import static com.whiteowl.core.backtest.v2.engine.SignalType.SHORT_EXIT;

public abstract class TradingStrategyBase extends Script {

    private static final int DEFAULT_MAX_BARS = 200;
    private static final float HUNDRED = 100f;

    protected FloatSmartValue open;
    protected FloatSmartValue high;
    protected FloatSmartValue low;
    protected FloatSmartValue close;
    protected LongSmartValue volume;
    protected LongSmartValue timestamp;

    private int maxBars = DEFAULT_MAX_BARS;
    private final IndicatorGraph indicatorGraph = new IndicatorGraph();
    private final List<StrategyInput> declaredInputs = new ArrayList<>();
    private Map<String, Number> inputOverrides = Map.of();
    private boolean discoveryMode;
    private boolean setupPhase;
    private PortfolioState portfolio;
    private SignalQueue signalQueue;
    private PositionTracker positionTracker;
    private float volumeParticipationPercent;

    public void setDiscoveryMode(boolean discoveryMode) {
        this.discoveryMode = discoveryMode;
    }

    public void setInputOverrides(Map<String, Number> overrides) {
        this.inputOverrides = overrides != null ? overrides : Map.of();
    }

    public void bindBarData(FloatSmartValue open, FloatSmartValue high,
                            FloatSmartValue low, FloatSmartValue close,
                            LongSmartValue volume, LongSmartValue timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public void bindEngine(PortfolioState portfolio, SignalQueue signalQueue,
                           PositionTracker positionTracker, float volumeParticipationPercent) {
        this.portfolio = portfolio;
        this.signalQueue = signalQueue;
        this.positionTracker = positionTracker;
        this.volumeParticipationPercent = volumeParticipationPercent;
    }

    public void invokeSetup() {
        setupPhase = true;
        setup();
        setupPhase = false;
    }

    public void invokeOnBar(int bar, String scripId) {
        onBar(bar, scripId);
    }

    protected abstract void setup();

    protected abstract void onBar(int bar, String scripId);

    @Override
    public Object run() {
        return null;
    }

    public void setMaxBars(int bars) {
        this.maxBars = bars;
    }

    public int getMaxBars() {
        return maxBars;
    }

    public IndicatorGraph getIndicatorGraph() {
        return indicatorGraph;
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

    public List<StrategyInput> getDeclaredInputs() {
        return Collections.unmodifiableList(declaredInputs);
    }

    public void clearInputs() {
        declaredInputs.clear();
    }

    public FloatSmartValue sma(FloatSmartValue source, int period) {
        String sig = "sma:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new SmaIndicator(period));
    }

    public FloatSmartValue ema(FloatSmartValue source, int period) {
        String sig = "ema:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new EmaIndicator(period));
    }

    public FloatSmartValue rsi(FloatSmartValue source, int period) {
        String sig = "rsi:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new RsiIndicator(period));
    }

    public FloatSmartValue highest(FloatSmartValue source, int period) {
        String sig = "highest:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new HighestIndicator(period));
    }

    public FloatSmartValue lowest(FloatSmartValue source, int period) {
        String sig = "lowest:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new LowestIndicator(period));
    }

    public FloatSmartValue[] macd(FloatSmartValue source, int fastPeriod, int slowPeriod, int signalPeriod) {
        String sig = "macd:" + System.identityHashCode(source) + ":" + fastPeriod + ":" + slowPeriod + ":" + signalPeriod;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            MacdIndicator macd = (MacdIndicator) node.getIndicator();
            return new FloatSmartValue[]{ node.getOutput(), macd.getSignalOutput(), macd.getHistogramOutput() };
        }
        FloatSmartValue macdOutput = new FloatSmartValue(maxBars);
        FloatSmartValue signalOutput = new FloatSmartValue(maxBars);
        FloatSmartValue histogramOutput = new FloatSmartValue(maxBars);
        MacdIndicator indicator = new MacdIndicator(fastPeriod, slowPeriod, signalPeriod, signalOutput, histogramOutput);
        IndicatorNode node = IndicatorNode.ofSingleSource(macdOutput, source, indicator, sig);
        indicatorGraph.addNode(node);
        return new FloatSmartValue[]{ macdOutput, signalOutput, histogramOutput };
    }

    public FloatSmartValue[] bollinger(FloatSmartValue source, int period, float stdDevMult) {
        String sig = "bollinger:" + System.identityHashCode(source) + ":" + period + ":" + stdDevMult;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            BollingerBandsIndicator bb = (BollingerBandsIndicator) node.getIndicator();
            return new FloatSmartValue[]{ node.getOutput(), bb.getUpperOutput(), bb.getLowerOutput() };
        }
        FloatSmartValue middleOutput = new FloatSmartValue(maxBars);
        FloatSmartValue upperOutput = new FloatSmartValue(maxBars);
        FloatSmartValue lowerOutput = new FloatSmartValue(maxBars);
        BollingerBandsIndicator indicator = new BollingerBandsIndicator(period, stdDevMult, upperOutput, lowerOutput);
        IndicatorNode node = IndicatorNode.ofSingleSource(middleOutput, source, indicator, sig);
        indicatorGraph.addNode(node);
        return new FloatSmartValue[]{ middleOutput, upperOutput, lowerOutput };
    }

    public FloatSmartValue atr(int period) {
        String sig = "atr:" + period;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            return node.getOutput();
        }
        FloatSmartValue output = new FloatSmartValue(maxBars);
        AtrIndicator indicator = new AtrIndicator(high, low, close, period);
        IndicatorNode node = IndicatorNode.ofDerived(output, indicator, sig);
        indicatorGraph.addNode(node);
        return output;
    }

    public FloatSmartValue[] adx(int period) {
        String sig = "adx:" + period;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            AdxIndicator adx = (AdxIndicator) node.getDerivedIndicator();
            return new FloatSmartValue[]{ node.getOutput(), adx.getPlusDiOutput(), adx.getMinusDiOutput() };
        }
        FloatSmartValue adxOutput = new FloatSmartValue(maxBars);
        FloatSmartValue plusDiOutput = new FloatSmartValue(maxBars);
        FloatSmartValue minusDiOutput = new FloatSmartValue(maxBars);
        AdxIndicator indicator = new AdxIndicator(high, low, close, period, plusDiOutput, minusDiOutput);
        IndicatorNode node = IndicatorNode.ofDerived(adxOutput, indicator, sig);
        indicatorGraph.addNode(node);
        return new FloatSmartValue[]{ adxOutput, plusDiOutput, minusDiOutput };
    }

    public FloatSmartValue stddev(FloatSmartValue source, int period) {
        String sig = "stddev:" + System.identityHashCode(source) + ":" + period;
        return resolveIndicator(sig, source, new StdDevIndicator(period));
    }

    public FloatSmartValue volumeSma(int period) {
        String sig = "volumeSma:" + period;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            return node.getOutput();
        }
        FloatSmartValue output = new FloatSmartValue(maxBars);
        VolumeSmaIndicator indicator = new VolumeSmaIndicator(volume, output, period);
        IndicatorNode node = IndicatorNode.ofDerived(output, indicator, sig);
        indicatorGraph.addNode(node);
        return output;
    }

    public FloatSmartValue roc(FloatSmartValue source, int period) {
        String sig = "roc:" + System.identityHashCode(source) + ":" + period;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            return node.getOutput();
        }
        FloatSmartValue output = new FloatSmartValue(maxBars);
        RocIndicator indicator = new RocIndicator(source, output, period);
        IndicatorNode node = IndicatorNode.ofDerived(output, indicator, sig);
        indicatorGraph.addNode(node);
        return output;
    }

    public FloatSmartValue[] swingHighLow(int strength) {
        String sig = "swingHL:" + strength;
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            SwingHighLowIndicator sw = (SwingHighLowIndicator) node.getDerivedIndicator();
            return new FloatSmartValue[]{ sw.getSwingHighOutput(), sw.getSwingLowOutput() };
        }
        FloatSmartValue swingHighOutput = new FloatSmartValue(maxBars);
        FloatSmartValue swingLowOutput = new FloatSmartValue(maxBars);
        SwingHighLowIndicator indicator = new SwingHighLowIndicator(high, low, swingHighOutput, swingLowOutput, strength);
        IndicatorNode node = IndicatorNode.ofDerived(swingHighOutput, indicator, sig);
        indicatorGraph.addNode(node);
        return new FloatSmartValue[]{ swingHighOutput, swingLowOutput };
    }

    public boolean crossover(FloatSmartValue a, FloatSmartValue b) {
        return a.getAt(0) > b.getAt(0) && a.getAt(-1) <= b.getAt(-1);
    }

    public boolean crossunder(FloatSmartValue a, FloatSmartValue b) {
        return a.getAt(0) < b.getAt(0) && a.getAt(-1) >= b.getAt(-1);
    }

    public void longEntry(int quantity) {
        emitEntry(LONG_ENTRY, quantity, NEXT_BAR, OPEN);
    }

    public void longEntry(int quantity, FillTiming timing, FillPrice price) {
        emitEntry(LONG_ENTRY, quantity, timing, price);
    }

    public void shortEntry(int quantity) {
        emitEntry(SHORT_ENTRY, quantity, NEXT_BAR, OPEN);
    }

    public void shortEntry(int quantity, FillTiming timing, FillPrice price) {
        emitEntry(SHORT_ENTRY, quantity, timing, price);
    }

    public void longExit() {
        emitExit(LONG_EXIT, -1, NEXT_BAR, OPEN);
    }

    public void longExit(FillTiming timing, FillPrice price) {
        emitExit(LONG_EXIT, -1, timing, price);
    }

    public void shortExit() {
        emitExit(SHORT_EXIT, -1, NEXT_BAR, OPEN);
    }

    public void shortExit(FillTiming timing, FillPrice price) {
        emitExit(SHORT_EXIT, -1, timing, price);
    }

    public void closePosition(int positionId) {
        emitExit(CLOSE_POSITION, positionId, NEXT_BAR, OPEN);
    }

    public void closePosition(int positionId, FillTiming timing, FillPrice price) {
        emitExit(CLOSE_POSITION, positionId, timing, price);
    }

    public float getCash() {
        return portfolio.getCash();
    }

    public float getEquity() {
        return portfolio.getEquity(close.value());
    }

    public float getCumulativePnl() {
        return portfolio.getCumulativePnl();
    }

    public boolean hasOpenPositions() {
        return portfolio.hasOpenPositions();
    }

    public int positionCount() {
        return portfolio.positionCount();
    }

    public int totalSize() {
        return portfolio.totalSize();
    }

    public float avgPrice() {
        return portfolio.avgPrice();
    }

    public float unrealizedPnl() {
        return portfolio.unrealizedPnl(close.value());
    }

    public List<OpenPosition> getOpenPositions() {
        return portfolio.getOpenPositions();
    }

    private void emitEntry(SignalType type, int quantity, FillTiming timing, FillPrice price) {
        QueuedSignal signal = QueuedSignal.entry(type, quantity, timing, price,
                open.value(), high.value(), low.value(), close.value());
        signalQueue.queueEntry(signal);
    }

    private void emitExit(SignalType type, int positionId, FillTiming timing, FillPrice price) {
        QueuedSignal signal = QueuedSignal.exit(type, positionId, timing, price,
                open.value(), high.value(), low.value(), close.value());
        signalQueue.queueExit(signal);
    }

    private FloatSmartValue resolveIndicator(String sig, FloatSmartValue source, Indicator indicator) {
        if (!setupPhase) {
            IndicatorNode node = indicatorGraph.findBySignature(sig);
            if (node == null) {
                throw new IllegalStateException("Indicator not found: " + sig);
            }
            return node.getOutput();
        }
        FloatSmartValue output = new FloatSmartValue(maxBars);
        IndicatorNode node = IndicatorNode.ofSingleSource(output, source, indicator, sig);
        indicatorGraph.addNode(node);
        return output;
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

}
