package com.whiteowl.client.kite.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class KiteCandle {

    private final long timestamp;
    private final float open;
    private final float high;
    private final float low;
    private final float close;
    private final int volume;
    private final int openInterest;

}
