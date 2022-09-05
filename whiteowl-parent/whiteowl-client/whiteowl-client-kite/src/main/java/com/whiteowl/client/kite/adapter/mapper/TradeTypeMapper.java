package com.whiteowl.client.kite.adapter.mapper;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.client.kite.model.TransactionType;

public class TradeTypeMapper {

	public TradeType toTradeType(TransactionType transactionType) {
		if(null == transactionType) return null;
		switch(transactionType) {
		case BUY: return TradeType.BUY;
		case SELL: return TradeType.SELL;
		default: throw new IllegalArgumentException(String.format("TransactionType %s is not supported", transactionType.name()));
		}
	}
	
	public TransactionType toTransactionType(TradeType tradeType) {
		if(null == tradeType) return null;
		switch(tradeType) {
		case BUY: return TransactionType.BUY;
		case SELL: return TransactionType.SELL;
		default: throw new IllegalArgumentException(String.format("TradeType %s is not supported", tradeType.name()));
		}
	}
	
}
