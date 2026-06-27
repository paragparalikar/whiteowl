package com.whiteowl.core.portfolio.model;

import com.whiteowl.core.order.model.Product;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class Position {

    private final String scripId;
    private final Product product;
    private final int quantity;
    private final int overnightQuantity;
    private final float averagePrice;
    private final float lastPrice;
    private final float pnl;
    private final float unrealised;
    private final float realised;
    private final int buyQuantity;
    private final float buyPrice;
    private final int sellQuantity;
    private final float sellPrice;

}
