package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteSegmentMargin {

    private boolean enabled;
    private double net;
    private KiteAvailableMargin available;
    private KiteUtilizedMargin utilised;

}
