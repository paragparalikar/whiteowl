package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class KitePositions {

    private List<KitePosition> net;
    private List<KitePosition> day;

}
