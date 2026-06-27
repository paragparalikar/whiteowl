package com.whiteowl.core.indicator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IndicatorFunctions {

    public static float[] sma(float[] source, int barCount, int period) {
        return Sma.compute(source, barCount, period);
    }

    public static float[] ema(float[] source, int barCount, int period) {
        return Ema.compute(source, barCount, period);
    }

    public static float[] rsi(float[] source, int barCount, int period) {
        return Rsi.compute(source, barCount, period);
    }

    public static float[] atr(float[] high, float[] low, float[] close, int barCount, int period) {
        return Atr.compute(high, low, close, barCount, period);
    }

    public static float[][] macd(float[] source, int barCount, int fastPeriod, int slowPeriod, int signalPeriod) {
        return Macd.compute(source, barCount, fastPeriod, slowPeriod, signalPeriod);
    }

    public static boolean[] crossover(float[] a, float[] b, int barCount) {
        return Crossover.compute(a, b, barCount);
    }

    public static boolean[] crossunder(float[] a, float[] b, int barCount) {
        return Crossunder.compute(a, b, barCount);
    }

    public static float[] highest(float[] source, int barCount, int period) {
        return Highest.compute(source, barCount, period);
    }

    public static float[] lowest(float[] source, int barCount, int period) {
        return Lowest.compute(source, barCount, period);
    }

    public static float[][] stochastics(float[] high, float[] low, float[] close,
                                        int barCount, int kPeriod, int dPeriod) {
        return Stochastics.compute(high, low, close, barCount, kPeriod, dPeriod);
    }

    public static float[] obv(float[] close, long[] volume, int barCount) {
        return Obv.compute(close, volume, barCount);
    }

    public static float[] stddev(float[] source, int barCount, int period) {
        return StdDev.compute(source, barCount, period);
    }

    public static float[] roc(float[] source, int barCount, int period) {
        return Roc.compute(source, barCount, period);
    }

    public static float[][] adx(float[] high, float[] low, float[] close, int barCount, int period) {
        return Adx.compute(high, low, close, barCount, period);
    }

    public static float[][] supertrend(float[] high, float[] low, float[] close,
                                       int barCount, int period, float multiplier) {
        return Supertrend.compute(high, low, close, barCount, period, multiplier);
    }

    public static float[][] bollingerBands(float[] source, int barCount, int period, float multiplier) {
        return BollingerBands.compute(source, barCount, period, multiplier);
    }

    public static float[] cci(float[] high, float[] low, float[] close, int barCount, int period) {
        return Cci.compute(high, low, close, barCount, period);
    }

    public static float[] williamsR(float[] high, float[] low, float[] close, int barCount, int period) {
        return WilliamsR.compute(high, low, close, barCount, period);
    }

    public static float[][] stochRsi(float[] close, int barCount, int rsiPeriod,
                                     int stochPeriod, int kSmooth, int dSmooth) {
        return StochRsi.compute(close, barCount, rsiPeriod, stochPeriod, kSmooth, dSmooth);
    }

    public static float[] ultimateOscillator(float[] high, float[] low, float[] close,
                                             int barCount, int period1, int period2, int period3) {
        return UltimateOscillator.compute(high, low, close, barCount, period1, period2, period3);
    }

    public static float[][] parabolicSar(float[] high, float[] low, float[] close,
                                         int barCount, float afStart, float afStep, float afMax) {
        return ParabolicSar.compute(high, low, close, barCount, afStart, afStep, afMax);
    }

    public static float[][] ichimoku(float[] high, float[] low, float[] close, int barCount,
                                     int tenkan, int kijun, int senkouB, int displacement) {
        return Ichimoku.compute(high, low, close, barCount, tenkan, kijun, senkouB, displacement);
    }

    public static float[][] aroon(float[] high, float[] low, int barCount, int period) {
        return Aroon.compute(high, low, barCount, period);
    }

    public static float[][] keltnerChannels(float[] high, float[] low, float[] close,
                                            int barCount, int emaPeriod, int atrPeriod, float multiplier) {
        return KeltnerChannels.compute(high, low, close, barCount, emaPeriod, atrPeriod, multiplier);
    }

    public static float[][] donchianChannels(float[] high, float[] low, int barCount, int period) {
        return DonchianChannels.compute(high, low, barCount, period);
    }

    public static float[] accumulationDistribution(float[] high, float[] low, float[] close,
                                                   long[] volume, int barCount) {
        return AccumulationDistribution.compute(high, low, close, volume, barCount);
    }

    public static float[] cmf(float[] high, float[] low, float[] close,
                               long[] volume, int barCount, int period) {
        return ChaikinMoneyFlow.compute(high, low, close, volume, barCount, period);
    }

    public static float[] mfi(float[] high, float[] low, float[] close,
                               long[] volume, int barCount, int period) {
        return MoneyFlowIndex.compute(high, low, close, volume, barCount, period);
    }

    public static float[] vwap(float[] high, float[] low, float[] close,
                                long[] volume, int barCount) {
        return Vwap.compute(high, low, close, volume, barCount);
    }

}
