package com.whiteowl.core.backtest.discovery;

import java.util.Arrays;
import java.util.List;

/**
 * MFE/MAE excursion analysis: percentile distributions of favorable and
 * adverse excursion (in ATR units) split by trade outcome. Used to sanity
 * check the stop/target grid — e.g. a stop should sit beyond the typical
 * adverse excursion of winning trades, a target within reach of winners.
 */
public final class ExcursionAnalyzer {

    private static final double[] PCTS = {10, 25, 50, 75, 90, 95, 99};

    private ExcursionAnalyzer() {}

    public static String report(List<DiscoveryTrade> trades) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXCURSION ANALYSIS (ATR units at entry)\n");
        sb.append(String.format("%-12s %6s |", "group", "n"));
        for (double p : PCTS) sb.append(String.format(" %6s", "p" + (int) p));
        sb.append('\n');

        double[] mfeW = collect(trades, true, true);
        double[] mfeL = collect(trades, false, true);
        double[] maeW = collect(trades, true, false);
        double[] maeL = collect(trades, false, false);

        appendRow(sb, "MFE winners", mfeW);
        appendRow(sb, "MFE losers", mfeL);
        appendRow(sb, "|MAE| winners", maeW);
        appendRow(sb, "|MAE| losers", maeL);

        sb.append("\nRead: stop ~ between winner-MAE p90 and loser-MAE p50;");
        sb.append(" target ~ around winner-MFE p50-p75.\n");
        return sb.toString();
    }

    private static double[] collect(List<DiscoveryTrade> trades, boolean winners,
                                     boolean favorable) {
        return trades.stream()
                .filter(t -> (t.netPoints() > 0) == winners)
                .mapToDouble(t -> favorable ? t.mfeAtr() : -t.maeAtr())
                .filter(v -> !Double.isNaN(v) && !Double.isInfinite(v))
                .sorted().toArray();
    }

    private static void appendRow(StringBuilder sb, String name, double[] v) {
        sb.append(String.format("%-12s %6d |", name, v.length));
        for (double p : PCTS) {
            sb.append(String.format(" %6.2f", percentile(v, p)));
        }
        sb.append('\n');
    }

    private static double percentile(double[] sorted, double pct) {
        if (sorted.length == 0) return Double.NaN;
        int k = (int) Math.ceil(pct / 100 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(k, sorted.length - 1))];
    }
}
