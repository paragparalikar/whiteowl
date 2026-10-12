package com.whiteowl.core.backtest.discovery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Time-of-day and day-of-week performance breakdown for a trade set:
 * per-bucket count, win rate, average net points, total and Sortino.
 */
public final class BreakdownAnalyzer {

    private BreakdownAnalyzer() {}

    public static String report(List<DiscoveryTrade> trades, int todBucketMinutes) {
        Map<String, Stats> tod = new LinkedHashMap<>();
        Map<String, Stats> dow = new LinkedHashMap<>();
        String[] dowNames = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri"}) {
            dow.put(d, new Stats());
        }
        for (DiscoveryTrade t : trades) {
            int b = t.entryMinuteOfDay() / todBucketMinutes * todBucketMinutes;
            String key = String.format("%02d:%02d-%02d:%02d",
                    b / 60, b % 60, (b + todBucketMinutes) / 60, (b + todBucketMinutes) % 60);
            tod.computeIfAbsent(key, k -> new Stats()).add(t.netPoints());
            if (t.dayOfWeek() >= 1 && t.dayOfWeek() <= 7) {
                dow.computeIfAbsent(dowNames[t.dayOfWeek()], k -> new Stats()).add(t.netPoints());
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("TIME-OF-DAY BREAKDOWN (entry time, ").append(todBucketMinutes).append("-min buckets)\n");
        sb.append(render(tod));
        sb.append("\nDAY-OF-WEEK BREAKDOWN\n");
        sb.append(render(dow));
        return sb.toString();
    }

    private static String render(Map<String, Stats> m) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %6s %7s %8s %10s %8s%n", "bucket", "n", "win%", "avgNet", "totNet", "sortino"));
        for (var e : m.entrySet()) {
            Stats s = e.getValue();
            sb.append(String.format("%-10s %6d %6.1f%% %8.2f %10.0f %8.3f%n",
                    e.getKey(), s.n, s.n > 0 ? 100.0 * s.wins / s.n : 0,
                    s.avg(), s.total, s.sortino()));
        }
        return sb.toString();
    }

    private static final class Stats {
        int n, wins;
        double total, sumSq, downSq;

        void add(double v) {
            n++;
            total += v;
            if (v > 0) wins++;
            downSq += Math.min(v, 0) * Math.min(v, 0);
        }

        double avg() { return n > 0 ? total / n : 0; }

        double sortino() {
            if (n == 0) return Double.NaN;
            double dv = Math.sqrt(downSq / n);
            return dv > 0 ? avg() / dv : Double.NaN;
        }
    }
}
