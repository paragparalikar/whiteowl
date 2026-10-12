package com.whiteowl.core.backtest.discovery;

import com.whiteowl.core.indicator.IndicatorFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * The regime-feature universe: every indicator evaluated at each Fibonacci
 * lookback plus fixed session context. Distance-like features are normalized
 * by ATR(14) so thresholds transfer across volatility regimes; bounded
 * oscillators (RSI, Stoch, ADX, %B) are left raw.
 *
 * <p>All series are indexed by bar and computed causally — the value at bar i
 * uses only data up to and including bar i.</p>
 */
public final class FeatureMatrix {

    private final List<String> names = new ArrayList<>();
    private final List<float[]> columns = new ArrayList<>();

    public FeatureMatrix(SignalContext ctx, int[] lookbacks, int exitMinuteOfDay) {
        int n = ctx.size;
        SessionIndex s = ctx.session;
        float[] atr14 = ctx.atr(14);

        // ── Context features (single columns) ───────────────────────────
        float[] minute = new float[n];
        float[] sinceOpen = new float[n];
        float[] toExit = new float[n];
        float[] dow = new float[n];
        float[] gapAtr = new float[n];
        float[] dPclose = new float[n];
        float[] dPhigh = new float[n];
        float[] dPlow = new float[n];
        float[] orRangeAtr = new float[n];
        float[] orHiDist = new float[n];
        float[] orLoDist = new float[n];
        float[] vwapDevAtr = new float[n];
        float[] vwapDevSig = new float[n];
        float[] rvolTod = new float[n];
        float[] atrPct14 = new float[n];
        float[] dayPos = new float[n];

        float[][] or = s.openingRange(ctx.high, ctx.low, 6); // 30-min opening range
        float[] orHigh = or[0], orLow = or[1];
        float[] volF = toFloat(ctx.volume);

        for (int d = 0; d < s.dayCount(); d++) {
            int from = s.dayStart(d), to = s.dayEnd(d);
            float runHi = Float.MIN_VALUE, runLo = Float.MAX_VALUE;
            for (int i = from; i <= to; i++) {
                runHi = Math.max(runHi, ctx.high[i]);
                runLo = Math.min(runLo, ctx.low[i]);
                float a = safe(atr14[i]);
                minute[i] = s.minuteOfDay(i);
                sinceOpen[i] = s.barOfDay(i);
                toExit[i] = exitMinuteOfDay - s.minuteOfDay(i);
                dow[i] = s.dayOfWeek(i);
                gapAtr[i] = valid(s.prevDayClose(i)) ? (s.dayOpen(i) - s.prevDayClose(i)) / a : Float.NaN;
                dPclose[i] = valid(s.prevDayClose(i)) ? (ctx.close[i] - s.prevDayClose(i)) / a : Float.NaN;
                dPhigh[i] = valid(s.prevDayHigh(i)) ? (ctx.close[i] - s.prevDayHigh(i)) / a : Float.NaN;
                dPlow[i] = valid(s.prevDayLow(i)) ? (ctx.close[i] - s.prevDayLow(i)) / a : Float.NaN;
                orRangeAtr[i] = valid(orHigh[i]) ? (orHigh[i] - orLow[i]) / a : Float.NaN;
                orHiDist[i] = valid(orHigh[i]) ? (ctx.close[i] - orHigh[i]) / a : Float.NaN;
                orLoDist[i] = valid(orLow[i]) ? (ctx.close[i] - orLow[i]) / a : Float.NaN;
                vwapDevAtr[i] = (ctx.close[i] - s.vwap(i)) / a;
                vwapDevSig[i] = s.vwapStd(i) > 0 ? (ctx.close[i] - s.vwap(i)) / s.vwapStd(i) : 0f;
                rvolTod[i] = s.rvolTod(i);
                atrPct14[i] = ctx.close[i] != 0 ? atr14[i] / ctx.close[i] * 100f : Float.NaN;
                dayPos[i] = runHi > runLo ? (ctx.close[i] - runLo) / (runHi - runLo) : 0.5f;
            }
        }

        add("minute_of_day", minute);
        add("bars_since_open", sinceOpen);
        add("minutes_to_exit", toExit);
        add("dow", dow);
        add("gap_atr", gapAtr);
        add("dist_pclose_atr", dPclose);
        add("dist_phigh_atr", dPhigh);
        add("dist_plow_atr", dPlow);
        add("or30_range_atr", orRangeAtr);
        add("dist_or30_high_atr", orHiDist);
        add("dist_or30_low_atr", orLoDist);
        add("vwap_dev_atr", vwapDevAtr);
        add("vwap_dev_sigma", vwapDevSig);
        add("rvol_tod", rvolTod);
        add("atr_pct_14", atrPct14);
        add("day_position", dayPos);
        add("ultosc_7_14_28", IndicatorFunctions.ultimateOscillator(ctx.high, ctx.low, ctx.close, n, 7, 14, 28));

        // ── Indicator x lookback grid ───────────────────────────────────
        for (int p : lookbacks) {
            add("rsi_" + p, ctx.rsi(p));
            add("stochk_" + p, ctx.stoch(p)[0]);
            float[][] adx = ctx.adx(p);
            add("adx_" + p, adx[0]);
            add("didiff_" + p, diff(adx[1], adx[2], n));
            float[][] aroon = ctx.aroon(p);
            add("aroon_" + p, diff(aroon[0], aroon[1], n));
            add("rocn_" + p, ratio(ctx.roc(p), atrPct14, n));
            add("cci_" + p, scale(ctx.cci(p), 0.01f, n));
            add("willr_" + p, ctx.willr(p));
            add("mfi_" + p, ctx.mfi(p));
            add("cmf_" + p, ctx.cmf(p));
            add("atrpct_" + p, ratio(ctx.atr(p), ctx.close, 100f, n));
            add("stdpct_" + p, ratio(ctx.stddev(p), ctx.close, 100f, n));
            float[][] bb = ctx.bollinger(p, 2f);
            add("bbbw_" + p, bandwidth(bb, n));
            add("bbpctb_" + p, pctB(bb, ctx.close, n));
            add("emadist_" + p, norm(ctx.close, ctx.ema(p), atr14, n));
            add("emaslope_" + p, slope(ctx.ema(p), atr14, n));
            float[][] dc = ctx.donchian(p);
            add("donchpos_" + p, posIn(dc, ctx.close, n));
            add("rvol_" + p, ratio(volF, ctx.smaVol(p), n));
            add("srsi_" + p, ctx.stochRsi(p));
        }
    }

    private void add(String name, float[] col) {
        names.add(name);
        columns.add(col);
    }

    public int columnCount() { return columns.size(); }
    public String name(int j) { return names.get(j); }
    public float value(int j, int bar) { return columns.get(j)[bar]; }
    public List<String> names() { return names; }

    /** True when every feature is defined at this bar (past all warm-ups). */
    public boolean isFullyDefined(int bar) {
        for (float[] c : columns) {
            if (Float.isNaN(c[bar])) return false;
        }
        return true;
    }

    // ── vector helpers ──────────────────────────────────────────────────

    private static float[] diff(float[] a, float[] b, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) r[i] = a[i] - b[i];
        return r;
    }

    private static float[] scale(float[] a, float k, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) r[i] = a[i] * k;
        return r;
    }

    private static float[] ratio(float[] a, float[] b, float mul, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = b[i] != 0 ? a[i] / b[i] * mul : Float.NaN;
        }
        return r;
    }

    /** a / b element-wise (b==0 -> NaN). */
    private static float[] ratio(float[] a, float[] b, int n) {
        if (a == null || b == null) {
            float[] r = new float[n];
            java.util.Arrays.fill(r, Float.NaN);
            return r;
        }
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = b[i] != 0 ? a[i] / b[i] : Float.NaN;
        }
        return r;
    }

    /** (a - b) / atr element-wise. */
    private static float[] norm(float[] a, float[] b, float[] atr, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = atr[i] > 0 ? (a[i] - b[i]) / atr[i] : Float.NaN;
        }
        return r;
    }

    /** (series[i] - series[i-5]) / atr[i]. */
    private static float[] slope(float[] x, float[] atr, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = i >= 5 && atr[i] > 0 && !Float.isNaN(x[i - 5])
                    ? (x[i] - x[i - 5]) / atr[i] : Float.NaN;
        }
        return r;
    }

    private static float[] bandwidth(float[][] bb, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = bb[0][i] != 0 ? (bb[1][i] - bb[2][i]) / bb[0][i] : Float.NaN;
        }
        return r;
    }

    private static float[] pctB(float[][] bb, float[] close, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            float w = bb[1][i] - bb[2][i];
            r[i] = w > 0 ? (close[i] - bb[2][i]) / w : Float.NaN;
        }
        return r;
    }

    private static float[] posIn(float[][] dc, float[] close, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            float w = dc[1][i] - dc[2][i];
            r[i] = w > 0 ? (close[i] - dc[2][i]) / w : Float.NaN;
        }
        return r;
    }

    private static float[] toFloat(long[] v) {
        float[] f = new float[v.length];
        for (int i = 0; i < v.length; i++) f[i] = v[i];
        return f;
    }

    private static boolean valid(float v) { return !Float.isNaN(v); }
    private static float safe(float atr) { return atr > 0 ? atr : 1f; }
}
