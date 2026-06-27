package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteTick {

    private int token;
    private float lastTradedPrice;
    private long lastTradedTime;
    private int volumeTradedToday;

}
