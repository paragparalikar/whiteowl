package com.whiteowl.core.gtt.model;

import com.whiteowl.core.order.model.LimitType;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.order.model.Product;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public final class OcoGttOrder {

    private int id;
    private String scripId;
    private OrderSide side;
    private LimitType limitType;
    private Product product;
    private int quantity;
    private float stoplossTriggerPrice;
    private float stoplossOrderPrice;
    private float targetTriggerPrice;
    private float targetOrderPrice;
    private float lastPrice;
    private float trailingPoints;
    private GttStatus status;
    private String expiresAt;
    private String createdAt;
    private String updatedAt;

}
