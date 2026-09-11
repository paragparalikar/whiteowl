package com.whiteowl.client.kite.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public final class KiteGttOrder {

    private KiteExchange exchange;
    private String tradingsymbol;
    private KiteTransactionType transactionType;
    private int quantity;
    private KiteLimitType orderType;
    private KiteProduct product;
    private float price;
    private Object result;

}
