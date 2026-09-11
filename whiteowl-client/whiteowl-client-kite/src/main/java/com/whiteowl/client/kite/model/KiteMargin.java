package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteMargin {

    private KiteSegmentMargin equity;
    private KiteSegmentMargin commodity;

}
