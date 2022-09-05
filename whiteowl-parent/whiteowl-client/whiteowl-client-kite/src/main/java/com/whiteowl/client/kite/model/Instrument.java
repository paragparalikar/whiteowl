package com.whiteowl.client.kite.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class Instrument {

	private long instrumentToken;
	private long exchangeToken;
	private String tradingsymbol;
	private String name;
	private double lastPrice;
	private String expiry;
	private double strike;
	private double tickSize;
	private int lotSize;
	private InstrumentType instrumentType;
	private String segment;
	private KiteExchange exchange;
	
	public Instrument(@NonNull final String text) {
		final String[] tokens = text.split(",");
		instrumentToken = Integer.parseInt(tokens[0]);
		exchangeToken = Integer.parseInt(tokens[1]);
		tradingsymbol = tokens[2];
		name = tokens[3];
		lastPrice = null == tokens[4] ? 0 : Double.parseDouble(tokens[4]);
		expiry = tokens[5];
		strike = null == tokens[6] ? 0 : Double.parseDouble(tokens[6]);
		tickSize = null == tokens[7] ? 0 : Double.parseDouble(tokens[7]);
		lotSize = null == tokens[8] ? 0 : Integer.parseInt(tokens[8]);
		instrumentType = null == tokens[9] ? null : InstrumentType.valueOf(tokens[9]);
		segment = tokens[10];
		exchange = null == tokens[11] ? null : KiteExchange.valueOf(tokens[11]);
	}
	
}
