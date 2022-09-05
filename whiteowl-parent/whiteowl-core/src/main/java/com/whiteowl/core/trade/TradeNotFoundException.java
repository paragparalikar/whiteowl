package com.whiteowl.core.trade;

public class TradeNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7775238906741242906L;

	public TradeNotFoundException(String message) {
		super(message);
	}
	
}
