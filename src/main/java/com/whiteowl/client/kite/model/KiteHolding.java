package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteHolding {

    private String tradingsymbol;
    private KiteExchange exchange;
    private int instrumentToken;
    private String isin;
    private KiteProduct product;
    private float price;
    private int quantity;
    private int usedQuantity;
    private int t1Quantity;
    private int realisedQuantity;
    private int authorisedQuantity;
    private String authorisedDate;
    private int openingQuantity;
    private int shortQuantity;
    private int collateralQuantity;
    private String collateralType;
    private boolean discrepancy;
    private float averagePrice;
    private float lastPrice;
    private float closePrice;
    private float pnl;
    private float dayChange;
    private float dayChangePercentage;

}
