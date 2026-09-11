package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class KiteMarketDepth {

    private List<KiteDepth> buy;
    private List<KiteDepth> sell;

}
