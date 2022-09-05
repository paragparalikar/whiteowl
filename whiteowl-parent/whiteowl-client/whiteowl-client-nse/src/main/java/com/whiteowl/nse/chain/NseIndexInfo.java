package com.whiteowl.nse.chain;

import lombok.Data;

@Data
public class NseIndexInfo {

	private String key;
	private String index;
	private String indexSymbol;
	private double last;
	private double variation;
	private double percentChange;
	private double open;
	private double high;
	private double low;
	private double previousClose;
	private double yearHigh;
	private double yearLow;
	private double pe;
	private double pb;
	private double dy;
	private int declines;
	private int advances;
	private int unchanged;
	
}
