package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.KiteOhlc;
import com.whiteowl.core.quote.Ohlc;

public class OhlcMapper {

	public Ohlc toOhlc(KiteOhlc kiteOhlc) {
		if(null == kiteOhlc) return null;
		final Ohlc ohlc = new Ohlc();
		ohlc.setOpen(kiteOhlc.getOpen());
		ohlc.setHigh(kiteOhlc.getHigh());
		ohlc.setLow(kiteOhlc.getLow());
		ohlc.setClose(kiteOhlc.getClose());
		return ohlc;
	}
	
}
