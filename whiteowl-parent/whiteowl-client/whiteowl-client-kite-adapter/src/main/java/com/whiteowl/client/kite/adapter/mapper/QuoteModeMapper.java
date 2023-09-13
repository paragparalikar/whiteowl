package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.core.quote.QuoteMode;

public class QuoteModeMapper {

	public KiteQuoteMode toKiteQuoteMode(QuoteMode quoteMode) {
		if(null == quoteMode) return null;
		switch(quoteMode) {
		case FULL:return KiteQuoteMode.FULL;
		case LTP:return KiteQuoteMode.LTP;
		case OHLC:return KiteQuoteMode.OHLC;
		default:throw new IllegalArgumentException(String.format("QuoteMode %s is not supported", quoteMode.name()));
		}
	}
	
}
