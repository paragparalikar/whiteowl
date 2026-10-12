package com.whiteowl.core.backtest.discovery;

import com.whiteowl.core.bar.model.BarsArrays;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-bar session bookkeeping for MCX-style intraday data: calendar-day
 * boundaries, previous-day OHLC, session-anchored VWAP with standard-deviation
 * bands, time-of-day volume profile and the per-day EOD exit bar.
 */
public final class SessionIndex {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final int size;
    private final int[] dayIndex;          // bar -> day ordinal
    private final List<int[]> dayRanges;   // day -> [firstBar, lastBar]
    private final int[] minuteOfDay;       // bar -> minutes since midnight IST
    private final int[] barOfDay;          // bar -> ordinal position within its day
    private final int[] dayOfWeek;         // bar -> 1=Mon ... 7=Sun
    private final float[] prevDayOpen, prevDayHigh, prevDayLow, prevDayClose;
    private final float[] dayOpen;         // bar -> open of its day
    private final float[] vwap, vwapStd;   // session-anchored
    private final float[] rvolTod;         // volume / 20-day mean of same bar-of-day
    private final int[] dayExitBar;        // bar -> last bar of its day at/before exit time

    public SessionIndex(BarsArrays bars, int barMinutes, int exitMinuteOfDay) {
        this.size = bars.size();
        dayIndex = new int[size];
        minuteOfDay = new int[size];
        barOfDay = new int[size];
        dayOfWeek = new int[size];
        prevDayOpen = new float[size];
        prevDayHigh = new float[size];
        prevDayLow = new float[size];
        prevDayClose = new float[size];
        dayOpen = new float[size];
        dayRanges = new ArrayList<>();

        // ── Day boundaries and previous-day levels ──────────────────────
        LocalDate curDate = null;
        int dayStart = 0;
        float pdO = Float.NaN, pdH = Float.NaN, pdL = Float.NaN, pdC = Float.NaN;
        float dayO = Float.NaN, dayH = Float.NaN, dayL = Float.NaN, dayC = Float.NaN;
        int body = 0;
        for (int i = 0; i < size; i++) {
            var zdt = Instant.ofEpochMilli(bars.timestamp()[i]).atZone(IST);
            LocalDate d = zdt.toLocalDate();
            if (!d.equals(curDate)) {
                if (curDate != null) {
                    dayRanges.add(new int[]{dayStart, i - 1});
                    pdO = dayO; pdH = dayH; pdL = dayL; pdC = dayC;
                }
                curDate = d;
                dayStart = i;
                body = 0;
                dayO = bars.open()[i]; dayH = bars.high()[i];
                dayL = bars.low()[i];
            }
            dayC = bars.close()[i];
            dayH = Math.max(dayH, bars.high()[i]);
            dayL = Math.min(dayL, bars.low()[i]);
            dayIndex[i] = dayRanges.size();
            minuteOfDay[i] = zdt.toLocalTime().getHour() * 60 + zdt.toLocalTime().getMinute();
            barOfDay[i] = body++;
            dayOfWeek[i] = d.getDayOfWeek().getValue();
            prevDayOpen[i] = pdO; prevDayHigh[i] = pdH;
            prevDayLow[i] = pdL; prevDayClose[i] = pdC;
            dayOpen[i] = dayO;
        }
        dayRanges.add(new int[]{dayStart, size - 1});

        // ── Session-anchored VWAP and volume-weighted band std ──────────
        vwap = new float[size];
        vwapStd = new float[size];
        for (int d = 0; d < dayRanges.size(); d++) {
            int s = dayRanges.get(d)[0], e = dayRanges.get(d)[1];
            double cumTpV = 0, cumV = 0;
            for (int i = s; i <= e; i++) {
                double tp = (bars.high()[i] + bars.low()[i] + bars.close()[i]) / 3.0;
                cumTpV += tp * bars.volume()[i];
                cumV += bars.volume()[i];
                vwap[i] = (float) (cumV > 0 ? cumTpV / cumV : tp);
            }
            double cumVar = 0; cumV = 0;
            for (int i = s; i <= e; i++) {
                double tp = (bars.high()[i] + bars.low()[i] + bars.close()[i]) / 3.0;
                double dev = tp - vwap[i];
                cumVar += dev * dev * bars.volume()[i];
                cumV += bars.volume()[i];
                vwapStd[i] = (float) Math.sqrt(cumV > 0 ? cumVar / cumV : 0);
            }
        }

        // ── Time-of-day relative volume (20-day same-slot mean) ─────────
        rvolTod = new float[size];
        int barsPerDay = Math.max(1, (24 * 60) / barMinutes);
        double[] slotSum = new double[barsPerDay];
        int[] slotCount = new int[barsPerDay];
        int maxSlot = -1;
        for (int d = 0; d < dayRanges.size(); d++) {
            int s = dayRanges.get(d)[0], e = dayRanges.get(d)[1];
            for (int i = s; i <= e; i++) {
                int slot = barOfDay[i];
                double mean = slotCount[slot] > 0 ? slotSum[slot] / slotCount[slot] : Double.NaN;
                rvolTod[i] = slotCount[slot] >= 5
                        ? (float) (bars.volume()[i] / mean) : Float.NaN;
            }
            for (int i = s; i <= e; i++) {
                int slot = barOfDay[i];
                slotSum[slot] += bars.volume()[i];
                slotCount[slot]++;
                maxSlot = Math.max(maxSlot, slot);
            }
        }

        // ── Per-day exit bar: last bar starting at or before exit time ───
        dayExitBar = new int[size];
        for (int d = 0; d < dayRanges.size(); d++) {
            int s = dayRanges.get(d)[0], e = dayRanges.get(d)[1];
            int exitBar = e;
            for (int i = s; i <= e; i++) {
                if (minuteOfDay[i] <= exitMinuteOfDay) exitBar = i;
            }
            for (int i = s; i <= e; i++) dayExitBar[i] = exitBar;
        }
    }

    /** Opening-range high/low per bar (NaN until the opening range completes). */
    public float[][] openingRange(float[] high, float[] low, int orBars) {
        float[] orHigh = new float[size];
        float[] orLow = new float[size];
        for (int d = 0; d < dayRanges.size(); d++) {
            int s = dayRanges.get(d)[0], e = dayRanges.get(d)[1];
            float h = Float.MIN_VALUE, l = Float.MAX_VALUE;
            for (int i = s; i <= e; i++) {
                if (barOfDay[i] < orBars) {
                    h = Math.max(h, high[i]);
                    l = Math.min(l, low[i]);
                    orHigh[i] = Float.NaN; orLow[i] = Float.NaN;
                } else {
                    orHigh[i] = h; orLow[i] = l;
                }
            }
        }
        return new float[][]{orHigh, orLow};
    }

    public int size() { return size; }
    public int dayCount() { return dayRanges.size(); }
    public int dayIndex(int bar) { return dayIndex[bar]; }
    public int dayStart(int day) { return dayRanges.get(day)[0]; }
    public int dayEnd(int day) { return dayRanges.get(day)[1]; }
    public int minuteOfDay(int bar) { return minuteOfDay[bar]; }
    public int barOfDay(int bar) { return barOfDay[bar]; }
    public int dayOfWeek(int bar) { return dayOfWeek[bar]; }
    public int exitBar(int bar) { return dayExitBar[bar]; }
    public boolean sameDay(int a, int b) { return dayIndex[a] == dayIndex[b]; }
    public float prevDayOpen(int bar) { return prevDayOpen[bar]; }
    public float prevDayHigh(int bar) { return prevDayHigh[bar]; }
    public float prevDayLow(int bar) { return prevDayLow[bar]; }
    public float prevDayClose(int bar) { return prevDayClose[bar]; }
    public float dayOpen(int bar) { return dayOpen[bar]; }
    public float vwap(int bar) { return vwap[bar]; }
    public float vwapStd(int bar) { return vwapStd[bar]; }
    public float rvolTod(int bar) { return rvolTod[bar]; }
}
