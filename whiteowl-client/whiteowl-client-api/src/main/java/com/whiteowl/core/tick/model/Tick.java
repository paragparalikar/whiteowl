package com.whiteowl.core.tick.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Tick {

    private String scripId;
    private long timestamp;
    private float lastTradedPrice;
    private long volume;
    private float open;
    private float high;
    private float low;
    private float close;

}
