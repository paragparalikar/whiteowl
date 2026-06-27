package com.whiteowl.core.bar.download;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.whiteowl.core.bar.download.MarketHours.DATE_TIME_FORMAT;

@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
public final class InvalidBarDataException extends Exception {

    private static final String MESSAGE_TEMPLATE = "Invalid data: %-15s %-5s %s, gap: %f and %f";

    private final String scripId;
    private final String timeframe;
    private final LocalDateTime timestamp;
    private final float previousClose;
    private final float currentOpen;

    @Override
    public String getMessage() {
        return String.format(MESSAGE_TEMPLATE, scripId, timeframe,
                DATE_TIME_FORMAT.format(timestamp), previousClose, currentOpen);
    }

}
