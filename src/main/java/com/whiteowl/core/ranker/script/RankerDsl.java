package com.whiteowl.core.ranker.script;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import groovy.lang.Script;

public abstract class RankerDsl extends Script {

    protected float[] open;
    protected float[] high;
    protected float[] low;
    protected float[] close;
    protected long[] volume;
    protected long[] timestamps;
    protected int barCount;

    public void bind(Bars bars) {
        BarsArrays arrays = bars.arrays();
        this.open = arrays.open();
        this.high = arrays.high();
        this.low = arrays.low();
        this.close = arrays.close();
        this.volume = arrays.volume();
        this.timestamps = arrays.timestamp();
        this.barCount = arrays.size();
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

}
