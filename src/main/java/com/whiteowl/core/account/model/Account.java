package com.whiteowl.core.account.model;

import com.whiteowl.core.portfolio.model.BrokerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public final class Account {

    public static final double DEFAULT_POSITION_SIZE = 100000;

    private final String id;
    private String name;
    private BrokerType brokerType;
    private String userId;
    private String password;
    private String pin;
    @Builder.Default private double positionSize = DEFAULT_POSITION_SIZE;

}
