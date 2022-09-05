package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.Candle;
import com.whiteowl.core.bar.PersistentBar;
import com.whiteowl.core.bar.Timeframe;

public class BarMapper {

	public PersistentBar toPersistentBar(Candle candle, String code, Timeframe timeframe) {
		return PersistentBar.builder()
				.code(code)
				.timeframe(timeframe)
				.beginTime(candle.getTimestamp())
				.open(candle.getOpen())
				.high(candle.getHigh())
				.low(candle.getLow())
				.close(candle.getClose())
				.volume(candle.getVolume())
				.build();
	}
	
}
