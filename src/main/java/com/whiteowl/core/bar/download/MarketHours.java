package com.whiteowl.core.bar.download;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.time.DayOfWeek.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarketHours {

    public static final LocalTime NSE_OPEN = LocalTime.of(9, 15);
    public static final LocalTime NSE_CLOSE = LocalTime.of(15, 30);
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a");

    public static boolean isWeekend(DayOfWeek day) {
        return SATURDAY == day || SUNDAY == day;
    }

    public static boolean isWeekday(DayOfWeek day) {
        return !isWeekend(day);
    }

    public static boolean isBeforeMarket(LocalTime time) {
        return time.isBefore(NSE_OPEN);
    }

    public static boolean isAfterMarket(LocalTime time) {
        return !NSE_CLOSE.isAfter(time);
    }

    public static ZonedDateTime snapToMarketOpen(ZonedDateTime dateTime) {
        return dateTime.with(NSE_OPEN);
    }

    public static ZonedDateTime snapToMarketClose(ZonedDateTime dateTime) {
        return dateTime.with(NSE_CLOSE);
    }

    public static ZonedDateTime alignToTimeframe(ZonedDateTime dateTime, long timeframeSeconds) {
        ZonedDateTime marketOpen = snapToMarketOpen(dateTime);
        Duration duration = Duration.ofSeconds(timeframeSeconds);
        long elapsed = Duration.between(marketOpen, dateTime).dividedBy(duration);
        return marketOpen.plus(duration.multipliedBy(elapsed));
    }

    public static ZonedDateTime adjustFromDate(ZonedDateTime from, long timeframeSeconds) {
        if (timeframeSeconds >= ChronoUnit.DAYS.getDuration().getSeconds()) {
            return from.truncatedTo(ChronoUnit.DAYS);
        }
        from = skipWeekendForward(from);
        from = snapPreMarketToOpen(from);
        from = snapPostMarketToNextOpen(from);
        return alignToTimeframe(from, timeframeSeconds);
    }

    public static ZonedDateTime adjustToDate(ZonedDateTime to, long timeframeSeconds) {
        if (timeframeSeconds >= ChronoUnit.DAYS.getDuration().getSeconds()) {
            return to.truncatedTo(ChronoUnit.DAYS);
        }
        to = skipWeekendBackward(to);
        to = snapPreMarketToPreviousClose(to);
        to = snapPostMarketToClose(to);
        Duration duration = Duration.ofSeconds(timeframeSeconds);
        ZonedDateTime aligned = alignToTimeframe(to, timeframeSeconds);
        return isAfterMarket(to.toLocalTime()) ? aligned.plus(duration) : aligned;
    }

    private static ZonedDateTime skipWeekendForward(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (FRIDAY == day && isAfterMarket(dateTime.toLocalTime())) {
            return snapToMarketOpen(dateTime.plusDays(3));
        }
        if (SATURDAY == day) {
            return snapToMarketOpen(dateTime.plusDays(2));
        }
        if (SUNDAY == day) {
            return snapToMarketOpen(dateTime.plusDays(1));
        }
        return dateTime;
    }

    private static ZonedDateTime skipWeekendBackward(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (MONDAY == day && isBeforeMarket(dateTime.toLocalTime())) {
            return snapToMarketClose(dateTime.minusDays(3));
        }
        if (SATURDAY == day) {
            return snapToMarketClose(dateTime.minusDays(1));
        }
        if (SUNDAY == day) {
            return snapToMarketClose(dateTime.minusDays(2));
        }
        return dateTime;
    }

    private static ZonedDateTime snapPreMarketToOpen(ZonedDateTime dateTime) {
        if (isWeekday(dateTime.getDayOfWeek()) && isBeforeMarket(dateTime.toLocalTime())) {
            return snapToMarketOpen(dateTime);
        }
        return dateTime;
    }

    private static ZonedDateTime snapPostMarketToNextOpen(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (isWeekday(day) && !FRIDAY.equals(day) && isAfterMarket(dateTime.toLocalTime())) {
            return snapToMarketOpen(dateTime.plusDays(1));
        }
        return dateTime;
    }

    private static ZonedDateTime snapPreMarketToPreviousClose(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (isWeekday(day) && !MONDAY.equals(day) && isBeforeMarket(dateTime.toLocalTime())) {
            return snapToMarketClose(dateTime.minusDays(1));
        }
        return dateTime;
    }

    private static ZonedDateTime snapPostMarketToClose(ZonedDateTime dateTime) {
        if (isWeekday(dateTime.getDayOfWeek()) && isAfterMarket(dateTime.toLocalTime())) {
            return snapToMarketClose(dateTime);
        }
        return dateTime;
    }

}
