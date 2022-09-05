package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class Position {

	private String tradingsymbol;
	private KiteExchange exchange;
	private int instrumentToken;
	private Product product;
	private int quantity;
	private int overnightQuantity;
	private int multiplier;
	private double averagePrice;
	private double closePrice;
	private double lastPrice;
	private double value;
	private double pnl;
	private double m2m;
	private double realised;
	private double unrealised;
	private int buyQuantity;
	private double buyPrice;
	private double buyValue;
	private double buyM2m;
	private int dayBuyQuantity;
	private double dayBuyPrice;
	private double dayBuyValue;
	private int sellQuantity;
	private double sellPrice;
	private double sellValue;
	private int daySellQuantity;
	private double daySellPrice;
	private double daySellValue;
	
}
