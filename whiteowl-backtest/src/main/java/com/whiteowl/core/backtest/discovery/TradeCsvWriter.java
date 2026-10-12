package com.whiteowl.core.backtest.discovery;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes one feature CSV per signal: trade metadata and outcomes followed by
 * the full feature snapshot taken at the signal bar (decision time). The
 * {@code label} column is the meta-labeling target (net P&L > 0).
 */
public final class TradeCsvWriter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] META = {
            "signal_bar", "entry_time", "exit_time", "side",
            "entry_price", "exit_price", "bars_held", "entry_minute", "day_of_week",
            "gross_pts", "cost_pts", "net_pts", "label",
            "mfe_pts", "mae_pts", "mfe_atr", "mae_atr", "atr14"
    };

    private TradeCsvWriter() {}

    public static void write(Path file, String signalName, List<DiscoveryTrade> trades,
                             FeatureMatrix features) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            StringBuilder h = new StringBuilder(String.join(",", META));
            for (String name : features.names()) h.append(',').append(name);
            w.write(h.toString());
            w.newLine();

            for (DiscoveryTrade t : trades) {
                StringBuilder r = new StringBuilder(1024);
                r.append(t.signalBarIndex()).append(',')
                 .append(FMT.format(Instant.ofEpochMilli(t.entryTimestamp()).atZone(IST))).append(',')
                 .append(FMT.format(Instant.ofEpochMilli(t.exitTimestamp()).atZone(IST))).append(',')
                 .append(t.side()).append(',')
                 .append(t.entryPrice()).append(',')
                 .append(t.exitPrice()).append(',')
                 .append(t.holdingBars()).append(',')
                 .append(t.entryMinuteOfDay()).append(',')
                 .append(t.dayOfWeek()).append(',')
                 .append(t.grossPoints()).append(',')
                 .append(t.costPoints()).append(',')
                 .append(t.netPoints()).append(',')
                 .append(t.win() ? 1 : 0).append(',')
                 .append(t.mfePoints()).append(',')
                 .append(t.maePoints()).append(',')
                 .append(num(t.mfeAtr())).append(',')
                 .append(num(t.maeAtr())).append(',')
                 .append(num(t.atrAtEntry()));
                int bar = t.signalBarIndex();
                for (int j = 0; j < features.columnCount(); j++) {
                    r.append(',').append(num(features.value(j, bar)));
                }
                w.write(r.toString());
                w.newLine();
            }
        }
    }

    private static String num(float v) {
        return Float.isNaN(v) ? "" : Float.toString(v);
    }
}
