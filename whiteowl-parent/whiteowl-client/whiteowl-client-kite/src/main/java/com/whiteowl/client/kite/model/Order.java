package com.whiteowl.client.kite.model;

import java.util.Map;

import lombok.Data;

@Data
public class Order {

	private String orderId;
	private String parentOrderId;	
	private String exchangeOrderId;
	private String placedBy;
	private OrderVariety variety;
	private double averagePrice;
	private int pendingQuantity;
	private int filledQuantity;
	private int marketProtection;
	private String orderTimestamp;
	private String exhangeTimestamp;
	private String statusMessage;
	private OrderStatus status;
	private Map<String, String> meta;
	
	// REGULAR
	private String tradingsymbol;
	private KiteExchange exchange;
	private TransactionType transactionType;
	private OrderType orderType;
	private int quantity;
	private Product product;
	private double price;
	private double triggerPrice;
	private int disclosedQuantity;
	private OrderValidity validity;
	private String tag;
	
	//BO
	private double squareoff;
	private double stoploss;
	private double trailingStoploss;
	
}
