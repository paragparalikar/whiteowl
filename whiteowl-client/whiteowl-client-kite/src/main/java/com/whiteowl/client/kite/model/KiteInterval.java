package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KiteInterval {

    MINUTE("minute", 30),
    THREE_MINUTE("3minute", 90),
    FIVE_MINUTE("5minute", 90),
    TEN_MINUTE("10minute", 90),
    FIFTEEN_MINUTE("15minute", 180),
    THIRTY_MINUTE("30minute", 180),
    SIXTY_MINUTE("60minute", 365),
    TWO_HOUR("2hour", 365),
    THREE_HOUR("3hour", 365),
    DAY("day", 2000);

    private final String text;
    private final int historicalBatchLimitInDays;

}
