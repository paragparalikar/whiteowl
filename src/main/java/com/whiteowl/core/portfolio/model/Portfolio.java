package com.whiteowl.core.portfolio.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public final class Portfolio {

    private final String id;
    private BrokerType brokerType;
    private float margin;
    private boolean enabledForTrading;

}
