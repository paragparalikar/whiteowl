package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Phase 7 — calendar/grouping analysis of the trade population:
 * time-of-day buckets, day-of-week, week-of-month, month-of-year.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalendarAnalyzer {

    private static final String[] DOW = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /** Time-of-day buckets of {@code bucketMinutes} width from market open. */
    public static List<BucketStats> timeOfDay(List<TradeObservation> trades,
                                               int bucketMinutes) {
        Map<Integer, List<TradeObservation>> groups = new TreeMap<>();
        for (TradeObservation t : trades) {
            if (t.minutesFromOpen() < 0) continue;
            groups.computeIfAbsent(t.minutesFromOpen() / bucketMinutes, k -> new ArrayList<>())
                    .add(t);
        }
        List<BucketStats> out = new ArrayList<>();
        for (Map.Entry<Integer, List<TradeObservation>> e : groups.entrySet()) {
            int startMin = 9 * 60 + 15 + e.getKey() * bucketMinutes;
            out.add(BucketStats.of(startMin, startMin + bucketMinutes,
                    String.format("%02d:%02d", startMin / 60, startMin % 60), e.getValue()));
        }
        return out;
    }

    public static List<BucketStats> dayOfWeek(List<TradeObservation> trades) {
        return categorical(trades, TradeObservation::dayOfWeek,
                i -> DOW[Math.min(6, Math.max(0, i - 1))]);
    }

    public static List<BucketStats> weekOfMonth(List<TradeObservation> trades) {
        return categorical(trades, TradeObservation::weekOfMonth, i -> "W" + i);
    }

    public static List<BucketStats> monthOfYear(List<TradeObservation> trades) {
        return categorical(trades, TradeObservation::monthOfYear,
                i -> MONTHS[Math.min(11, Math.max(0, i - 1))]);
    }

    private static List<BucketStats> categorical(List<TradeObservation> trades,
                                                  Function<TradeObservation, Integer> key,
                                                  Function<Integer, String> label) {
        Map<Integer, List<TradeObservation>> groups = new TreeMap<>();
        for (TradeObservation t : trades) {
            groups.computeIfAbsent(key.apply(t), k -> new ArrayList<>()).add(t);
        }
        Map<String, List<TradeObservation>> labeled = new LinkedHashMap<>();
        groups.forEach((k, v) -> labeled.put(label.apply(k), v));
        List<BucketStats> out = new ArrayList<>();
        labeled.forEach((lbl, list) ->
                out.add(BucketStats.of(Double.NaN, Double.NaN, lbl, list)));
        return out;
    }

}
