package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class AvailableMargin {

	private double adhocMargin;
	
	private double cash;
	
	private double openingBalance;
	
	private double liveBalance;
	
	private double collateral;
	
	private double intradayPayin;
	
}
