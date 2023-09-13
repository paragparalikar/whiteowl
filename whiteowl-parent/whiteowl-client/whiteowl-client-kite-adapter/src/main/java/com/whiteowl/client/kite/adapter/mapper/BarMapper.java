package com.whiteowl.client.kite.adapter.mapper;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.client.kite.model.Candle;
import com.whiteowl.core.bar.Timeframe;

public class BarMapper {

	public Bar toBar(Candle candle, Timeframe timeframe) {
		return BaseBar.builder()
				.openPrice(DoubleNum.valueOf(candle.getOpen()))
				.highPrice(DoubleNum.valueOf(candle.getHigh()))
				.lowPrice(DoubleNum.valueOf(candle.getLow()))
				.closePrice(DoubleNum.valueOf(candle.getClose()))
				.volume(DoubleNum.valueOf(candle.getVolume()))
				.openInterest(DoubleNum.valueOf(candle.getOpenInterest()))
				.timePeriod(timeframe.getDuration())
				.endTime(candle.getTimestamp().plus(timeframe.getDuration()))
				.build();
	}
	
}
