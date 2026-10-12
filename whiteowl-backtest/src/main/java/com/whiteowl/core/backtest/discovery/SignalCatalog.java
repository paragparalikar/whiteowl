package com.whiteowl.core.backtest.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * The 25 raw entry signals, grouped by family. Every signal is symmetric
 * (long and short variants share one evaluator) and causal — bar i only sees
 * data up to bar i. Parameters are deliberately ordinary defaults; regime
 * suitability is the feature matrix's job, not the signal's.
 */
public final class SignalCatalog {

    private SignalCatalog() {}

    /**
     * Time-anchored volatility breakout: for each day, the band is
     * {@code open(anchor) +/- atrMult * ATR(14)} where anchor is the first bar
     * at/after {@code startMinute - windowMinutes} — i.e. the open of the
     * window that forms immediately before the allowed entry period.
     * A cross is only evaluated on bars at/after {@code startMinute}.
     */
    public static byte[] anchoredVolBreakout(SignalContext ctx, int startMinute,
                                              int windowMinutes, double atrMult) {
        int n = ctx.size;
        SessionIndex s = ctx.session;
        float[] atr = ctx.atr(14);
        byte[] sig = new byte[n];
        for (int d = 0; d < s.dayCount(); d++) {
            int from = s.dayStart(d), to = s.dayEnd(d);
            int anchor = -1;
            for (int i = from; i <= to; i++) {
                if (s.minuteOfDay(i) >= startMinute - windowMinutes) { anchor = i; break; }
            }
            if (anchor < 0) continue;
            float a = atr[anchor];
            if (Float.isNaN(a) || a <= 0) continue;
            float up = ctx.open[anchor] + (float) (atrMult * a);
            float dn = ctx.open[anchor] - (float) (atrMult * a);
            for (int i = Math.max(anchor + 1, from + 1); i <= to; i++) {
                if (s.minuteOfDay(i) < startMinute) continue;
                if (ctx.close[i - 1] <= up && ctx.close[i] > up) sig[i] = 1;
                else if (ctx.close[i - 1] >= dn && ctx.close[i] < dn) sig[i] = -1;
            }
        }
        return sig;
    }

    public static List<EntrySignal> all(SignalContext ctx) {
        List<EntrySignal> out = new ArrayList<>();
        breakout(ctx, out);
        movingAverage(ctx, out);
        vwap(ctx, out);
        oscillator(ctx, out);
        priceAction(ctx, out);
        return out;
    }

    // ═══════════════ Breakout / channel ═══════════════════════════════

    private static void breakout(SignalContext c, List<EntrySignal> out) {
        int n = c.size;

        out.add(EntrySignal.of("donchian_20", x -> {
            float[][] dc = x.donchian(20);
            byte[] s = new byte[n];
            for (int i = 2; i < n; i++) {
                if (crossPrev(c.close, dc[1], i)) s[i] = 1;
                else if (crossPrevDown(c.close, dc[2], i)) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("orb_30", x -> {
            float[][] or = x.session.openingRange(x.high, x.low, 6);
            return crossBoth(c.close, or[0], or[1]);
        }));

        out.add(EntrySignal.of("prevday_hl", x -> {
            float[] ph = prevDaySeries(x, true), pl = prevDaySeries(x, false);
            return crossBoth(c.close, ph, pl);
        }));

        out.add(EntrySignal.of("prevday_close", x -> {
            float[] pc = new float[n];
            for (int i = 0; i < n; i++) pc[i] = x.session.prevDayClose(i);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (crossUp(c.close, pc, i)) s[i] = 1;
                else if (crossDown(c.close, pc, i)) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("pivot_r1s1", x -> {
            float[] r1 = new float[n], s1 = new float[n];
            for (int i = 0; i < n; i++) {
                float h = x.session.prevDayHigh(i), l = x.session.prevDayLow(i),
                        cl = x.session.prevDayClose(i);
                if (Float.isNaN(h)) { r1[i] = s1[i] = Float.NaN; continue; }
                float p = (h + l + cl) / 3f;
                r1[i] = 2 * p - l;
                s1[i] = 2 * p - h;
            }
            return crossBoth(c.close, r1, s1);
        }));

        out.add(EntrySignal.of("vol_breakout", x -> {
            float[] atr = x.atr(14);
            float[] up = new float[n], dn = new float[n];
            for (int i = 0; i < n; i++) {
                float o = x.session.dayOpen(i), a = atr[i];
                up[i] = Float.isNaN(a) ? Float.NaN : o + 0.5f * a;
                dn[i] = Float.isNaN(a) ? Float.NaN : o - 0.5f * a;
            }
            return crossBoth(c.close, up, dn);
        }));

        out.add(EntrySignal.of("inside_bar", x -> {
            byte[] s = new byte[n];
            for (int i = 2; i < n; i++) {
                boolean inside = c.high[i - 1] < c.high[i - 2] && c.low[i - 1] > c.low[i - 2];
                if (!inside) continue;
                if (c.close[i] > c.high[i - 1]) s[i] = 1;
                else if (c.close[i] < c.low[i - 1]) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("squeeze_release", x -> {
            float[][] bb = x.bollinger(20, 2f);
            float[] bw = new float[n];
            for (int i = 0; i < n; i++) {
                bw[i] = bb[0][i] > 0 ? (bb[1][i] - bb[2][i]) / bb[0][i] : Float.NaN;
            }
            float[] rank = rollingRank(bw, 144);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(rank[i]) || rank[i] > 0.20f) continue;
                if (c.close[i - 1] <= bb[1][i - 1] && c.close[i] > bb[1][i]) s[i] = 1;
                else if (c.close[i - 1] >= bb[2][i - 1] && c.close[i] < bb[2][i]) s[i] = -1;
            }
            return s;
        }));
    }

    // ═══════════════ Moving average ═══════════════════════════════════

    private static void movingAverage(SignalContext c, List<EntrySignal> out) {
        int n = c.size;

        out.add(EntrySignal.of("ema_9_21", x -> crossBoth(x.ema(9), x.ema(21), c)));
        out.add(EntrySignal.of("ema_20_50", x -> crossBoth(x.ema(20), x.ema(50), c)));

        out.add(EntrySignal.of("close_ema50", x -> {
            float[] e = x.ema(50);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (crossUp(c.close, e, i)) s[i] = 1;
                else if (crossDown(c.close, e, i)) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("ema20_pullback", x -> {
            float[] e20 = x.ema(20), e50 = x.ema(50);
            byte[] s = new byte[n];
            for (int i = 5; i < n; i++) {
                if (Float.isNaN(e50[i - 5])) continue;
                boolean upTrend = e50[i] > e50[i - 5];
                boolean dnTrend = e50[i] < e50[i - 5];
                if (upTrend && c.low[i] <= e20[i] && c.close[i] > e20[i] && c.close[i] > c.open[i]) s[i] = 1;
                else if (dnTrend && c.high[i] >= e20[i] && c.close[i] < e20[i] && c.close[i] < c.open[i]) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("supertrend_10_3", x -> {
            float[] dir = x.supertrend(10, 3f)[1];
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(dir[i]) || Float.isNaN(dir[i - 1])) continue;
                if (dir[i] > 0 && dir[i - 1] < 0) s[i] = 1;
                else if (dir[i] < 0 && dir[i - 1] > 0) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("psar_flip", x -> {
            float[] bull = x.psar()[1];
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(bull[i])) continue;
                if (bull[i] > 0.5f && bull[i - 1] < 0.5f) s[i] = 1;
                else if (bull[i] < 0.5f && bull[i - 1] > 0.5f) s[i] = -1;
            }
            return s;
        }));
    }

    // ═══════════════ VWAP ═════════════════════════════════════════════

    private static void vwap(SignalContext c, List<EntrySignal> out) {
        int n = c.size;

        out.add(EntrySignal.of("vwap_cross", x -> {
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                float v0 = x.session.vwap(i - 1), v1 = x.session.vwap(i);
                if (c.close[i - 1] <= v0 && c.close[i] > v1) s[i] = 1;
                else if (c.close[i - 1] >= v0 && c.close[i] < v1) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("vwap_band_2", x -> {
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                float u0 = x.session.vwap(i - 1) + 2f * x.session.vwapStd(i - 1);
                float u1 = x.session.vwap(i) + 2f * x.session.vwapStd(i);
                float d0 = x.session.vwap(i - 1) - 2f * x.session.vwapStd(i - 1);
                float d1 = x.session.vwap(i) - 2f * x.session.vwapStd(i);
                if (c.close[i - 1] <= u0 && c.close[i] > u1) s[i] = 1;
                else if (c.close[i - 1] >= d0 && c.close[i] < d1) s[i] = -1;
            }
            return s;
        }));
    }

    // ═══════════════ Oscillator / momentum ════════════════════════════

    private static void oscillator(SignalContext c, List<EntrySignal> out) {
        int n = c.size;

        out.add(EntrySignal.of("rsi14_50", x -> crossLevel(x.rsi(14), 50)));

        out.add(EntrySignal.of("rsi2_revert", x -> {
            float[] r = x.rsi(2);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(r[i])) continue;
                if (r[i - 1] >= 10 && r[i] < 10) s[i] = 1;
                else if (r[i - 1] <= 90 && r[i] > 90) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("stoch_kd", x -> crossBoth(x.stoch(14)[0], x.stoch(14)[1], c)));

        out.add(EntrySignal.of("srsi_zone", x -> {
            float[] k = x.stochRsi(14);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(k[i])) continue;
                if (k[i - 1] <= 20 && k[i] > 20) s[i] = 1;
                else if (k[i - 1] >= 80 && k[i] < 80) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("macd_12_26_9", x -> crossBoth(x.macd(12, 26, 9)[0], x.macd(12, 26, 9)[1], c)));

        out.add(EntrySignal.of("cci_100", x -> {
            float[] cc = x.cci(20);
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                if (Float.isNaN(cc[i])) continue;
                if (cc[i - 1] <= 100 && cc[i] > 100) s[i] = 1;
                else if (cc[i - 1] >= -100 && cc[i] < -100) s[i] = -1;
            }
            return s;
        }));

        out.add(EntrySignal.of("roc21_zero", x -> crossLevel(x.roc(21), 0)));

        out.add(EntrySignal.of("di_cross", x -> {
            float[][] a = x.adx(14);
            return crossBoth(a[1], a[2], c);
        }));
    }

    // ═══════════════ Price action / volume ════════════════════════════

    private static void priceAction(SignalContext c, List<EntrySignal> out) {
        int n = c.size;

        out.add(EntrySignal.of("engulfing", x -> {
            byte[] s = new byte[n];
            for (int i = 1; i < n; i++) {
                boolean bull = c.close[i - 1] < c.open[i - 1] && c.close[i] > c.open[i]
                        && c.close[i] >= c.open[i - 1] && c.open[i] <= c.close[i - 1];
                boolean bear = c.close[i - 1] > c.open[i - 1] && c.close[i] < c.open[i]
                        && c.close[i] <= c.open[i - 1] && c.open[i] >= c.close[i - 1];
                if (bull) s[i] = 1;
                else if (bear) s[i] = -1;
            }
            return s;
        }));
    }

    // ═══════════════ Helpers ══════════════════════════════════════════

    private static byte[] crossBoth(float[] a, float[] up, float[] dn) {
        byte[] s = new byte[a.length];
        for (int i = 1; i < a.length; i++) {
            if (crossUp(a, up, i)) s[i] = 1;
            else if (crossDown(a, dn, i)) s[i] = -1;
        }
        return s;
    }

    /** Two-series crossover: a crossing b upward -> +1, downward -> -1. */
    private static byte[] crossBoth(float[] a, float[] b, SignalContext c) {
        byte[] s = new byte[a.length];
        for (int i = 1; i < a.length; i++) {
            if (crossUp(a, b, i)) s[i] = 1;
            else if (crossDown(a, b, i)) s[i] = -1;
        }
        return s;
    }

    private static byte[] crossLevel(float[] a, float level) {
        byte[] s = new byte[a.length];
        for (int i = 1; i < a.length; i++) {
            if (Float.isNaN(a[i]) || Float.isNaN(a[i - 1])) continue;
            if (a[i - 1] <= level && a[i] > level) s[i] = 1;
            else if (a[i - 1] >= level && a[i] < level) s[i] = -1;
        }
        return s;
    }

    private static boolean crossUp(float[] a, float[] b, int i) {
        return !Float.isNaN(a[i - 1]) && !Float.isNaN(b[i - 1]) && !Float.isNaN(a[i]) && !Float.isNaN(b[i])
                && a[i - 1] <= b[i - 1] && a[i] > b[i];
    }

    private static boolean crossDown(float[] a, float[] b, int i) {
        return !Float.isNaN(a[i - 1]) && !Float.isNaN(b[i - 1]) && !Float.isNaN(a[i]) && !Float.isNaN(b[i])
                && a[i - 1] >= b[i - 1] && a[i] < b[i];
    }

    /** Cross against the *previous* bar's channel value (Donchian-style). */
    private static boolean crossPrev(float[] a, float[] b, int i) {
        return !Float.isNaN(b[i - 1]) && !Float.isNaN(b[i - 2]) && !Float.isNaN(a[i - 1])
                && a[i - 1] <= b[i - 2] && a[i] > b[i - 1];
    }

    private static boolean crossPrevDown(float[] a, float[] b, int i) {
        return !Float.isNaN(b[i - 1]) && !Float.isNaN(b[i - 2]) && !Float.isNaN(a[i - 1])
                && a[i - 1] >= b[i - 2] && a[i] < b[i - 1];
    }

    private static float[] prevDaySeries(SignalContext x, boolean high) {
        float[] r = new float[x.size];
        for (int i = 0; i < x.size; i++) {
            r[i] = high ? x.session.prevDayHigh(i) : x.session.prevDayLow(i);
        }
        return r;
    }

    /** Fraction of trailing {@code p} values (incl. current) <= current value. */
    private static float[] rollingRank(float[] x, int p) {
        float[] r = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            if (Float.isNaN(x[i])) { r[i] = Float.NaN; continue; }
            int cnt = 0, tot = 0;
            for (int j = Math.max(0, i - p + 1); j <= i; j++) {
                if (Float.isNaN(x[j])) continue;
                tot++;
                if (x[j] <= x[i]) cnt++;
            }
            r[i] = tot > 0 ? (float) cnt / tot : Float.NaN;
        }
        return r;
    }
}
