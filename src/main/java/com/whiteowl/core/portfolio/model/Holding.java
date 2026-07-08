package com.whiteowl.core.portfolio.model;

import com.whiteowl.core.scrip.model.Scrip;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class Holding {

    private final String portfolioId;
    private final Scrip scrip;
    private final String exchange;
    private final String isin;
    private final String product;
    private final int quantity;
    private final int usedQuantity;
    private final int t1Quantity;
    private final int realisedQuantity;
    private final int authorisedQuantity;
    private final int openingQuantity;
    private final int shortQuantity;
    private final int collateralQuantity;
    private final String collateralType;
    private final boolean discrepancy;
    private final float averagePrice;
    private final float lastPrice;
    private final float closePrice;
    private final float pnl;
    private final float dayChange;
    private final float dayChangePercentage;

}
