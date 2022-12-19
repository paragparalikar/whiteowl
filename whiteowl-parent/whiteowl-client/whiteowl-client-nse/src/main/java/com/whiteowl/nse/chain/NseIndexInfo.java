package com.whiteowl.nse.chain;

import com.whiteowl.core.derivative.option.IndexInfo;

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
	
	public IndexInfo toIndexInfo() {
		final IndexInfo indexInfo = new IndexInfo();
		indexInfo.setKey(key);
		indexInfo.setIndex(index);
		indexInfo.setIndexSymbol(indexSymbol);
		indexInfo.setLast(last);
		indexInfo.setVariation(variation);
		indexInfo.setPercentChange(percentChange);
		indexInfo.setOpen(open);
		indexInfo.setHigh(high);
		indexInfo.setLow(low);
		indexInfo.setPreviousClose(previousClose);
		indexInfo.setYearHigh(yearHigh);
		indexInfo.setYearLow(yearLow);
		indexInfo.setPe(pe);
		indexInfo.setPb(pb);
		indexInfo.setDy(dy);
		indexInfo.setDeclines(declines);
		indexInfo.setAdvances(advances);
		indexInfo.setUnchanged(unchanged);
		return indexInfo;
	}
	
}
