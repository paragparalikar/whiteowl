package com.whiteowl.core.backtest.discovery;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Loads the continuous-futures CSV produced by tools/continuous-futures
 * ({@code datetime,open,high,low,close,volume,open_interest,expiry_date,symbol})
 * and resamples it to the requested bar size.
 */
@Slf4j
public final class CsvBarsLoader {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvBarsLoader() {}

    public static Bars load(Path csv, int barMinutes) throws IOException {
        Bars one = new Bars("CSV", Timeframe.ONE_MINUTE, 1 << 20);
        try (BufferedReader r = Files.newBufferedReader(csv)) {
            String line = r.readLine(); // header
            while ((line = r.readLine()) != null) {
                int c1 = line.indexOf(',');
                int c2 = line.indexOf(',', c1 + 1);
                int c3 = line.indexOf(',', c2 + 1);
                int c4 = line.indexOf(',', c3 + 1);
                int c5 = line.indexOf(',', c4 + 1);
                int c6 = line.indexOf(',', c5 + 1);
                long ts = LocalDateTime.parse(line.substring(0, c1), TS)
                        .atZone(IST).toInstant().toEpochMilli();
                one.append(ts,
                        Float.parseFloat(line.substring(c1 + 1, c2)),
                        Float.parseFloat(line.substring(c2 + 1, c3)),
                        Float.parseFloat(line.substring(c3 + 1, c4)),
                        Float.parseFloat(line.substring(c4 + 1, c5)),
                        Long.parseLong(line.substring(c5 + 1, c6)));
            }
        }
        log.info("Loaded {} 1-min bars from {}", one.size(), csv);
        return resample(one, barMinutes);
    }

    /** Aggregates bars into {@code minutes}-sized buckets aligned to wall clock. */
    public static Bars resample(Bars src, int minutes) {
        long bucketMs = minutes * 60_000L;
        Timeframe tf = switch (minutes) {
            case 5 -> Timeframe.FIVE_MINUTE;
            case 15 -> Timeframe.FIFTEEN_MINUTE;
            case 30 -> Timeframe.THIRTY_MINUTE;
            case 60 -> Timeframe.ONE_HOUR;
            default -> Timeframe.ONE_MINUTE;
        };
        Bars out = new Bars(src.getScripId(), tf, src.size() / minutes + 16);
        long curBucket = Long.MIN_VALUE;
        float o = 0, h = 0, l = 0, c = 0;
        long v = 0;
        for (int i = 0; i < src.size(); i++) {
            long b = src.getTimestamp(i) / bucketMs;
            if (b != curBucket) {
                if (curBucket != Long.MIN_VALUE) {
                    out.append(curBucket * bucketMs, o, h, l, c, v);
                }
                curBucket = b;
                o = src.getOpen(i);
                h = src.getHigh(i);
                l = src.getLow(i);
                c = src.getClose(i);
                v = src.getVolume(i);
            } else {
                h = Math.max(h, src.getHigh(i));
                l = Math.min(l, src.getLow(i));
                c = src.getClose(i);
                v += src.getVolume(i);
            }
        }
        if (curBucket != Long.MIN_VALUE) {
            out.append(curBucket * bucketMs, o, h, l, c, v);
        }
        log.info("Resampled to {} {}-min bars", out.size(), minutes);
        return out;
    }
}
