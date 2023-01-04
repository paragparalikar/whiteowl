package com.whiteowl.client.kite.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Order {

	private String instrumentToken;
	private String orderId;
	private String parentOrderId;	
	private String exchangeOrderId;
	private String placedBy;
	private boolean modified;
	private OrderVariety variety;
	private double averagePrice;
	private int pendingQuantity;
	private int filledQuantity;
	private int cancelledQuantity;
	private int marketProtection;
	private String orderTimestamp;
	private String exchangeTimestamp;
	private String exchangeUpdateTimestamp;
	private String statusMessage;
	private String statusMessageRaw;
	private OrderStatus status;
	private Map<String, Object> meta;
	private String tag;
	private List<String> tags;
	private String guid;
	
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
	private int validityTtl;
	
	
	//BO
	private double squareoff;
	private double stoploss;
	private double trailingStoploss;
	
}
