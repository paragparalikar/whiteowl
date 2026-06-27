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

    public static final int DEFAULT_PREFERRED_POSITION_COUNT = 10;

    private final String id;
    private String name;
    private BrokerType brokerType;
    private String userId;
    private String password;
    private String pin;
    @Builder.Default private int preferredPositionCount = DEFAULT_PREFERRED_POSITION_COUNT;

}
