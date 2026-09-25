package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.bar.model.BarsArrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Plain array-based indicator series for feature capture and analysis.
 * Element {@code i} uses only bars {@code <= i}; warmup entries are NaN.
 * (TradingStrategyBase's indicator graph is for strategies; these serve the
 * analysis layer.)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IndicatorSeries {

    public static float[] sma(float[] src, int period) {
        int n = src.length;
        float[] out = new float[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += src[i];
            if (i >= period) sum -= src[i - period];
            out[i] = i >= period - 1 ? (float) (sum / period) : Float.NaN;
        }
        return out;
    }

    public static float[] stdDev(float[] src, int period) {
        int n = src.length;
        float[] out = new float[n];
        double sum = 0, sumSq = 0;
        for (int i = 0; i < n; i++) {
            sum += src[i];
            sumSq += src[i] * src[i];
            if (i >= period) {
                sum -= src[i - period];
                sumSq -= src[i - period] * src[i - period];
            }
            if (i >= period - 1) {
                double mean = sum / period;
                double var = sumSq / period - mean * mean;
                out[i] = (float) Math.sqrt(Math.max(0, var));
            } else {
                out[i] = Float.NaN;
            }
        }
        return out;
    }

    /** Wilder RSI over close prices. */
    public static float[] rsi(float[] close, int period) {
        int n = close.length;
        float[] out = new float[n];
        if (n == 0) return out;
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i < n; i++) {
            double d = close[i] - close[i - 1];
            double g = Math.max(d, 0), l = Math.max(-d, 0);
            if (i <= period) {
                avgGain += g / period;
                avgLoss += l / period;
                out[i] = i == period ? rsiValue(avgGain, avgLoss) : Float.NaN;
            } else {
                avgGain = (avgGain * (period - 1) + g) / period;
                avgLoss = (avgLoss * (period - 1) + l) / period;
                out[i] = rsiValue(avgGain, avgLoss);
            }
        }
        out[0] = Float.NaN;
        return out;
    }

    private static float rsiValue(double avgGain, double avgLoss) {
        if (avgLoss == 0) return avgGain == 0 ? 50f : 100f;
        double rs = avgGain / avgLoss;
        return (float) (100.0 - 100.0 / (1.0 + rs));
    }

    /** Wilder ADX (returns ADX only). */
    public static float[] adx(BarsArrays a, int period) {
        int n = a.size();
        float[] out = new float[n];
        if (n < 2) return out;
        double[] tr = new double[n], pdm = new double[n], ndm = new double[n];
        double[] atr = new double[n], pdi = new double[n], ndi = new double[n], dx = new double[n];
        double trS = 0, pdmS = 0, ndmS = 0, adxS = 0;
        for (int i = 1; i < n; i++) {
            double up = a.high()[i] - a.high()[i - 1];
            double dn = a.low()[i - 1] - a.low()[i];
            tr[i] = Math.max(a.high()[i] - a.low()[i],
                    Math.max(Math.abs(a.high()[i] - a.close()[i - 1]),
                            Math.abs(a.low()[i] - a.close()[i - 1])));
            pdm[i] = up > dn && up > 0 ? up : 0;
            ndm[i] = dn > up && dn > 0 ? dn : 0;
            if (i <= period) {
                trS += tr[i]; pdmS += pdm[i]; ndmS += ndm[i];
                atr[i] = trS; pdi[i] = pdmS; ndi[i] = ndmS;
            } else {
                trS = trS - trS / period + tr[i];
                pdmS = pdmS - pdmS / period + pdm[i];
                ndmS = ndmS - ndmS / period + ndm[i];
                atr[i] = trS; pdi[i] = pdmS; ndi[i] = ndmS;
            }
            double pdiV = atr[i] == 0 ? 0 : 100 * pdi[i] / atr[i];
            double ndiV = atr[i] == 0 ? 0 : 100 * ndi[i] / atr[i];
            double sum = pdiV + ndiV;
            dx[i] = sum == 0 ? 0 : 100 * Math.abs(pdiV - ndiV) / sum;
            if (i == 2 * period - 1) {
                adxS = 0;
                for (int j = period; j <= i; j++) adxS += dx[j];
                adxS /= period;
            } else if (i > 2 * period - 1) {
                adxS = (adxS * (period - 1) + dx[i]) / period;
            }
            out[i] = i >= 2 * period - 1 ? (float) adxS : Float.NaN;
        }
        out[0] = Float.NaN;
        return out;
    }

    /** Rate of change (%): {@code (src[i] - src[i-n]) / src[i-n] * 100}. */
    public static float[] roc(float[] src, int period) {
        int n = src.length;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            out[i] = i >= period && src[i - period] != 0
                    ? (src[i] - src[i - period]) / src[i - period] * 100f
                    : Float.NaN;
        }
        return out;
    }

}
