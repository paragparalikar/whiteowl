package com.whiteowl.developer;

public class Trade {

	public final long entryDate, exitDate;
	public final float entryPrice, exitPrice;
	
	public Trade(long entryDate, long exitDate, float entryPrice, float exitPrice) {
		this.entryDate = entryDate;
		this.exitDate = exitDate;
		this.entryPrice = entryPrice;
		this.exitPrice = exitPrice;
	}
	
}
