package com.whiteowl.core.order.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public final class Order {

    private final String id;
    private String exchangeOrderId;
    private String portfolioId;
    private String scripId;
    private OrderSide side;
    private LimitType limitType;
    private Product product;
    private Variety variety;
    private Validity validity;
    private OrderStatus status;
    private String message;
    private long timestamp;
    private int quantity;
    private int filledQuantity;
    private int cancelledQuantity;
    private float price;
    private float triggerPrice;
    private float averagePrice;

}
