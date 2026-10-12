package com.whiteowl.core.backtest.discovery;

import java.nio.file.Path;
import java.time.LocalTime;

/**
 * Configuration for the strategy-discovery pipeline: raw 1-minute input,
 * resampled bar size, session exit conventions, trading cost, and optional
 * ATR-based initial stop / fixed target (null = disabled, EOD-only exit).
 */
public record DiscoveryConfig(
        Path inputCsv,
        Path outputDir,
        int barMinutes,
        LocalTime exitTime,
        LocalTime entryCutoff,
        LocalTime entryStart,
        double costPointsPerSide,
        double rupeesPerPoint,
        int[] lookbacks,
        Double initialStopAtr,
        Double targetAtr) {

    /** Fibonacci lookback grid for regime-feature generation. */
    public static final int[] FIB_LOOKBACKS = {2, 3, 5, 8, 13, 21, 34, 55, 89, 144};

    public static DiscoveryConfig mcxCrude(Path inputCsv, Path outputDir) {
        return new DiscoveryConfig(
                inputCsv,
                outputDir,
                5,                       // 5-minute bars
                LocalTime.of(23, 15),    // EOD exit at 23:15
                LocalTime.of(22, 45),    // no fresh entries in the last 30 min
                LocalTime.of(0, 0),      // entries allowed from session open
                2.0,                     // 2 points slippage+fees per side
                100.0,                   // CRUDEOIL: Rs 100 per point per lot
                FIB_LOOKBACKS,
                null,                    // no initial stop (EOD exit only)
                null);                   // no target
    }

    public DiscoveryConfig withStops(Double initialStopAtr, Double targetAtr) {
        return new DiscoveryConfig(inputCsv, outputDir, barMinutes, exitTime,
                entryCutoff, entryStart, costPointsPerSide, rupeesPerPoint, lookbacks,
                initialStopAtr, targetAtr);
    }

    public DiscoveryConfig withEntryStart(LocalTime entryStart) {
        return new DiscoveryConfig(inputCsv, outputDir, barMinutes, exitTime,
                entryCutoff, entryStart, costPointsPerSide, rupeesPerPoint, lookbacks,
                initialStopAtr, targetAtr);
    }
}
