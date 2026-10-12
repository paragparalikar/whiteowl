package com.whiteowl.core.backtest.discovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Exhaustive stop x target grid: re-simulates the signal under each
 * (initialStopAtr, targetAtr) pair on real bar paths and ranks cells by
 * per-trade Sortino. Also reports first-half / second-half stability so a
 * cell that only works in one era is not mistaken for a robust optimum.
 */
public final class StopTargetGrid {

    private final DiscoveryConfig base;
    private final SignalContext ctx;

    public record Cell(double stop, double target, int trades, double sortino,
                       double totalNet, double winRate, double avgNet,
                       double sortinoH1, double sortinoH2) {}

    public StopTargetGrid(SignalContext ctx, DiscoveryConfig base) {
        this.ctx = ctx;
        this.base = base;
    }

    public List<Cell> sweep(byte[] signals, double[] stops, double[] targets) {
        List<Cell> cells = new ArrayList<>();
        for (double s : stops) {
            for (double t : targets) {
                DiscoveryEngine engine = new DiscoveryEngine(ctx,
                        base.withStops(s == 0 ? null : s, t == 0 ? null : t));
                List<DiscoveryTrade> trades = engine.run(signals);
                if (trades.size() < 50) continue;
                cells.add(score(s, t, trades));
            }
        }
        cells.sort(Comparator.comparingDouble(Cell::sortino).reversed());
        return cells;
    }

    private Cell score(double s, double t, List<DiscoveryTrade> trades) {
        int n = trades.size();
        double[] net = new double[n];
        int wins = 0;
        for (int i = 0; i < n; i++) {
            net[i] = trades.get(i).netPoints();
            if (net[i] > 0) wins++;
        }
        int half = n / 2;
        return new Cell(s, t, n, MetaLabeler.sortino(net), sum(net),
                (double) wins / n, sum(net) / n,
                MetaLabeler.sortino(java.util.Arrays.copyOfRange(net, 0, half)),
                MetaLabeler.sortino(java.util.Arrays.copyOfRange(net, half, n)));
    }

    private static double sum(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s;
    }

    public static String renderGrid(List<Cell> cells, double[] stops, double[] targets, int top) {
        StringBuilder sb = new StringBuilder();
        sb.append("STOP x TARGET GRID — Sortino (n / H1 / H2)\n");
        sb.append(String.format("%8s", "S\\T"));
        for (double t : targets) sb.append(String.format(" %22s", t == 0 ? "none" : fmt(t)));
        sb.append('\n');
        for (double s : stops) {
            sb.append(String.format("%8s", fmt(s)));
            for (double t : targets) {
                Cell c = find(cells, s, t);
                sb.append(c == null ? String.format(" %22s", "-")
                        : String.format(" %8.3f/%2d/%6.2f/%6.2f",
                        c.sortino(), c.trades() / 100, c.sortinoH1(), c.sortinoH2()));
            }
            sb.append('\n');
        }
        sb.append("\nTop cells:\n");
        for (int i = 0; i < Math.min(top, cells.size()); i++) {
            Cell c = cells.get(i);
            sb.append(String.format("  S=%.2fA T=%.2fA  sortino=%.3f  n=%d  win=%.0f%%  avg=%.1f  tot=%.0f  H1=%.2f H2=%.2f%n",
                    c.stop(), c.target(), c.sortino(), c.trades(),
                    c.winRate() * 100, c.avgNet(), c.totalNet(),
                    c.sortinoH1(), c.sortinoH2()));
        }
        return sb.toString();
    }

    private static Cell find(List<Cell> cells, double s, double t) {
        for (Cell c : cells) {
            if (c.stop() == s && c.target() == t) return c;
        }
        return null;
    }

    private static String fmt(double v) {
        return v == 0 ? "none" : String.format("%.2f", v);
    }
}
