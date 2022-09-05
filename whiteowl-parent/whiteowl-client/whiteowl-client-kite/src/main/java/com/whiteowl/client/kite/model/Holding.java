package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class Holding {

	private String tradingsymbol;
	
	private KiteExchange exchange;
	
	private String instrumentToken;
	
	private String isin;
	
	private Product product;
	
	private double price;
	
	private int quantity;
	
	private int t1Quantity;
	
	private int realisedQuantity;
	
	private int collateralQuantity;
	
	private String collateralType;
	
	private boolean discrepancy;
	
	private double averagePrice;
	
	private double lastPrice;
	
	private double closePrice;
	
	private double pnl;
	
	private double dayChange;
	
	private double dayChangePercentage;
	
}
