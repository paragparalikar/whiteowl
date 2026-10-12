package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.discovery.*;
import com.whiteowl.core.bar.model.Bars;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sweeps the earliest allowed entry time (session open -> 22:00) for the
 * chosen signal, holding exits fixed at the development-stage parameters
 * (3 ATR initial stop, no target, 23:15 EOD exit, no regime filters).
 * Answers: "when does the breakout's daily trend actually start?"
 */
@Slf4j
public final class EntryTimeSweepDriver {

    private static final Path INPUT = Path.of(
            "C:\\trading\\OptionsData_Order91_GOLD-GOLDM-and-6-more_Futures_2023-2026",
            "continuous_csv", "CRUDEOIL_continuous_1min.csv");
    private static final int STEP_MINUTES = 30;

    record Row(LocalTime entryStart, int trades, double winRate, double avgNet,
               double totalNet, double sortino, double sortinoH1, double sortinoH2) {}

    public static void main(String[] args) throws Exception {
        Path input = args.length > 0 ? Path.of(args[0]) : INPUT;
        Path outDir = Path.of(System.getProperty("user.home"),
                ".whiteowl", "discovery", "crudeoil", "development");
        DiscoveryConfig config = DiscoveryConfig.mcxCrude(input, outDir)
                .withStops(3.0, null);

        Bars bars = CsvBarsLoader.load(input, config.barMinutes());
        var arrays = bars.arrays();
        int exitMinute = config.exitTime().getHour() * 60 + config.exitTime().getMinute();
        SignalContext ctx = new SignalContext(arrays,
                new SessionIndex(arrays, config.barMinutes(), exitMinute));

        StringBuilder sb = new StringBuilder();
        // Anchor window variants: the band is built from the open of the bar
        // at (startMinute - window), i.e. the window just before the entry period.
        for (int window : new int[]{30, 15}) {
            List<Row> rows = new ArrayList<>();
            LocalTime open = LocalTime.of(9, 0);
            LocalTime end = LocalTime.of(22, 0);
            for (LocalTime t = open; !t.isAfter(end); t = t.plusMinutes(STEP_MINUTES)) {
                int startMin = t.getHour() * 60 + t.getMinute();
                byte[] sig = SignalCatalog.anchoredVolBreakout(ctx, startMin, window, 0.5);
                DiscoveryEngine engine = new DiscoveryEngine(ctx, config.withEntryStart(t));
                List<DiscoveryTrade> trades = engine.run(sig);
                rows.add(score(t, trades));
            }
            rows.sort(Comparator.comparingDouble(Row::sortino).reversed());
            sb.append(String.format("ANCHORED VOL BREAKOUT — window=%dmin (stop=3.0A, no target, EOD 23:15)%n",
                    window));
            sb.append(String.format("%-8s %6s %7s %8s %10s %8s %8s %8s%n",
                    "start", "n", "win%", "avgNet", "totNet", "sortino", "H1", "H2"));
            for (Row r : rows) {
                sb.append(String.format("%-8s %6d %6.1f%% %8.2f %10.0f %8.3f %8.3f %8.3f%n",
                        r.entryStart(), r.trades(), r.winRate() * 100, r.avgNet(),
                        r.totalNet(), r.sortino(), r.sortinoH1(), r.sortinoH2()));
            }
            sb.append('\n');
        }
        log.info("\n{}", sb);
        Path f = outDir.resolve("entry_start_sweep_anchored.txt");
        Files.createDirectories(f.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(f)) {
            w.write(sb.toString());
        }
        log.info("Sweep written to {}", f);
    }

    private static Row score(LocalTime t, List<DiscoveryTrade> trades) {
        int n = trades.size();
        double[] net = new double[n];
        int wins = 0;
        for (int i = 0; i < n; i++) {
            net[i] = trades.get(i).netPoints();
            if (net[i] > 0) wins++;
        }
        int half = n / 2;
        return new Row(t, n,
                n > 0 ? (double) wins / n : Double.NaN,
                n > 0 ? sum(net) / n : Double.NaN,
                sum(net),
                MetaLabeler.sortino(net),
                MetaLabeler.sortino(java.util.Arrays.copyOfRange(net, 0, half)),
                MetaLabeler.sortino(java.util.Arrays.copyOfRange(net, half, n)));
    }

    private static double sum(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s;
    }
}
