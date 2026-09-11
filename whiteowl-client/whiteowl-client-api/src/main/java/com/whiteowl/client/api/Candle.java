package com.whiteowl.client.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class Candle {

    private final long timestamp;
    private final float open;
    private final float high;
    private final float low;
    private final float close;
    private final int volume;

}
