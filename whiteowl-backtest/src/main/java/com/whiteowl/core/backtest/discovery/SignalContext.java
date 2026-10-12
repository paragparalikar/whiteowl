package com.whiteowl.core.backtest.discovery;

import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared evaluation context handed to entry signals and the feature matrix:
 * raw bar arrays, session bookkeeping, and a lazy cache of computed indicator
 * series so identical computations are never repeated across signals.
 */
public final class SignalContext {

    public final float[] open, high, low, close;
    public final long[] volume, timestamp;
    public final int size;
    public final SessionIndex session;
    private final Map<String, float[]> cache = new HashMap<>();
    private final Map<String, float[][]> cache2 = new HashMap<>();

    public SignalContext(BarsArrays bars, SessionIndex session) {
        // BarsArrays exposes the underlying capacity arrays — trim to size so
        // signal/feature code can safely trust array bounds.
        this.size = bars.size();
        this.open = java.util.Arrays.copyOf(bars.open(), size);
        this.high = java.util.Arrays.copyOf(bars.high(), size);
        this.low = java.util.Arrays.copyOf(bars.low(), size);
        this.close = java.util.Arrays.copyOf(bars.close(), size);
        this.volume = java.util.Arrays.copyOf(bars.volume(), size);
        this.timestamp = java.util.Arrays.copyOf(bars.timestamp(), size);
        this.session = session;
    }

    public float[] ema(int p)        { return cache.computeIfAbsent("ema" + p, k -> IndicatorFunctions.ema(close, size, p)); }
    public float[] sma(int p)        { return cache.computeIfAbsent("sma" + p, k -> IndicatorFunctions.sma(close, size, p)); }
    public float[] rsi(int p)        { return cache.computeIfAbsent("rsi" + p, k -> IndicatorFunctions.rsi(close, size, p)); }
    public float[] atr(int p)        { return cache.computeIfAbsent("atr" + p, k -> IndicatorFunctions.atr(high, low, close, size, p)); }
    public float[] roc(int p)        { return cache.computeIfAbsent("roc" + p, k -> IndicatorFunctions.roc(close, size, p)); }
    public float[] stddev(int p)     { return cache.computeIfAbsent("std" + p, k -> IndicatorFunctions.stddev(close, size, p)); }
    public float[] cci(int p)        { return cache.computeIfAbsent("cci" + p, k -> IndicatorFunctions.cci(high, low, close, size, p)); }
    public float[] willr(int p)      { return cache.computeIfAbsent("willr" + p, k -> IndicatorFunctions.williamsR(high, low, close, size, p)); }
    public float[] highest(int p)    { return cache.computeIfAbsent("hi" + p, k -> IndicatorFunctions.highest(high, size, p)); }
    public float[] lowest(int p)     { return cache.computeIfAbsent("lo" + p, k -> IndicatorFunctions.lowest(low, size, p)); }
    public float[] mfi(int p)        { return cache.computeIfAbsent("mfi" + p, k -> IndicatorFunctions.mfi(high, low, close, volume, size, p)); }
    public float[] cmf(int p)        { return cache.computeIfAbsent("cmf" + p, k -> IndicatorFunctions.cmf(high, low, close, volume, size, p)); }
    public float[] smaVol(int p)     { return cache.computeIfAbsent("smaV" + p, k -> IndicatorFunctions.sma(toFloat(volume), size, p)); }
    public float[] stochRsi(int p)   { return cache.computeIfAbsent("srsi" + p, k -> IndicatorFunctions.stochRsi(close, size, p, p, 3, 3)[0]); }

    public float[][] macd(int f, int s, int g) { return cache2.computeIfAbsent("macd" + f + "_" + s, k -> IndicatorFunctions.macd(close, size, f, s, g)); }
    public float[][] adx(int p)      { return cache2.computeIfAbsent("adx" + p, k -> IndicatorFunctions.adx(high, low, close, size, p)); }
    public float[][] stoch(int p)    { return cache2.computeIfAbsent("sto" + p, k -> IndicatorFunctions.stochastics(high, low, close, size, p, 3)); }
    public float[][] bollinger(int p, float m) { return cache2.computeIfAbsent("bb" + p + "_" + m, k -> IndicatorFunctions.bollingerBands(close, size, p, m)); }
    public float[][] donchian(int p) { return cache2.computeIfAbsent("dc" + p, k -> IndicatorFunctions.donchianChannels(high, low, size, p)); }
    public float[][] keltner(int p, float m)   { return cache2.computeIfAbsent("kc" + p + "_" + m, k -> IndicatorFunctions.keltnerChannels(high, low, close, size, p, p, m)); }
    public float[][] aroon(int p)    { return cache2.computeIfAbsent("aroon" + p, k -> IndicatorFunctions.aroon(high, low, size, p)); }
    public float[][] supertrend(int p, float m){ return cache2.computeIfAbsent("st" + p + "_" + m, k -> IndicatorFunctions.supertrend(high, low, close, size, p, m)); }
    public float[][] psar()          { return cache2.computeIfAbsent("psar", k -> IndicatorFunctions.parabolicSar(high, low, close, size, 0.02f, 0.02f, 0.2f)); }

    private static float[] toFloat(long[] v) {
        float[] f = new float[v.length];
        for (int i = 0; i < v.length; i++) f[i] = v[i];
        return f;
    }
}
