package com.whiteowl.client.kite.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public final class KiteOrder {

    private int instrumentToken;
    private String orderId;
    private String parentOrderId;
    private String exchangeOrderId;
    private String placedBy;
    private boolean modified;
    private KiteOrderVariety variety;
    private float averagePrice;
    private int pendingQuantity;
    private int filledQuantity;
    private int cancelledQuantity;
    private int marketProtection;
    private String orderTimestamp;
    private String exchangeTimestamp;
    private String exchangeUpdateTimestamp;
    private String statusMessage;
    private String statusMessageRaw;
    private KiteOrderStatus status;
    private String tag;
    private String tradingsymbol;
    private KiteExchange exchange;
    private KiteTransactionType transactionType;
    private KiteLimitType limitType;
    private int quantity;
    private KiteProduct product;
    private float price;
    private float triggerPrice;
    private int disclosedQuantity;
    private KiteOrderValidity validity;
    private float squareoff;
    private float stoploss;
    private float trailingStoploss;

}
