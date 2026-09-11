package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteAvailableMargin {

    private double adhocMargin;
    private double cash;
    private double openingBalance;
    private double liveBalance;
    private double collateral;
    private double intradayPayin;

}
