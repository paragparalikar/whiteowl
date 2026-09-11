package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class BarsAggregator {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private BarsAggregator() {}

    public static Bars aggregate(Bars dailyBars, Timeframe target) {
        if (dailyBars == null || dailyBars.size() == 0) {
            return new Bars(dailyBars != null ? dailyBars.getScripId() : "", target, 0);
        }
        Bars result = new Bars(dailyBars.getScripId(), target, dailyBars.size());
        int i = 0;
        while (i < dailyBars.size()) {
            long barTs = dailyBars.getTimestamp(i);
            float open = dailyBars.getOpen(i);
            float high = dailyBars.getHigh(i);
            float low = dailyBars.getLow(i);
            float close = dailyBars.getClose(i);
            long volume = dailyBars.getVolume(i);
            long periodTs = barTs;
            i++;
            while (i < dailyBars.size() && isSamePeriod(periodTs, dailyBars.getTimestamp(i), target)) {
                high = Math.max(high, dailyBars.getHigh(i));
                low = Math.min(low, dailyBars.getLow(i));
                close = dailyBars.getClose(i);
                volume += dailyBars.getVolume(i);
                i++;
            }
            result.append(periodTs, open, high, low, close, volume);
        }
        return result;
    }

    private static boolean isSamePeriod(long firstTs, long currentTs, Timeframe target) {
        LocalDate firstDate = toLocalDate(firstTs);
        LocalDate currentDate = toLocalDate(currentTs);
        return switch (target) {
            case WEEKLY -> isSameWeek(firstDate, currentDate);
            case MONTHLY -> firstDate.getYear() == currentDate.getYear()
                    && firstDate.getMonthValue() == currentDate.getMonthValue();
            default -> false;
        };
    }

    private static boolean isSameWeek(LocalDate first, LocalDate current) {
        LocalDate weekStart = first.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        return !current.isBefore(weekStart) && !current.isAfter(weekEnd);
    }

    private static LocalDate toLocalDate(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalDate();
    }

}
