package com.whiteowl.core.bar.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Timeframe {

    ONE_MINUTE(60L, "1min", "1m", "1 Minute", 1, null),
    TWO_MINUTE(120L, "2min", "2m", "2 Minutes", 1, null),
    THREE_MINUTE(180L, "3min", "3m", "3 Minutes", 1, null),
    FIVE_MINUTE(300L, "5min", "5m", "5 Minutes", 1, null),
    TEN_MINUTE(600L, "10min", "10m", "10 Minutes", 1, null),
    FIFTEEN_MINUTE(900L, "15min", "15m", "15 Minutes", 1, null),
    THIRTY_MINUTE(1800L, "30min", "30m", "30 Minutes", 1, null),
    ONE_HOUR(3600L, "1h", "1h", "1 Hour", 2, null),
    TWO_HOUR(7200L, "2h", "2h", "2 Hours", 2, null),
    THREE_HOUR(10800L, "3h", "3h", "3 Hours", 2, null),
    DAILY(86400L, "daily", "D", "Daily", 20, null),
    WEEKLY(604800L, "weekly", "W", "Weekly", 20, DAILY),
    MONTHLY(2592000L, "monthly", "M", "Monthly", 20, DAILY);

    private final long seconds;
    private final String label;
    private final String code;
    private final String displayLabel;
    private final int maxLookbackYears;
    private final Timeframe sourceTimeframe;

    public boolean isAggregated() {
        return sourceTimeframe != null;
    }

}
