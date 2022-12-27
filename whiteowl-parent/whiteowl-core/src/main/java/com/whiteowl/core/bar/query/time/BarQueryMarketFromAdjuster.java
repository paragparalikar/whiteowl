package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_START_TIME;

import java.time.Duration;
import java.time.ZonedDateTime;

import com.whiteowl.core.bar.Timeframe;

public class BarQueryMarketFromAdjuster implements BarQueryMarketTimeAdjuster {

	@Override
	public ZonedDateTime apply(Timeframe timeframe, ZonedDateTime date) {
		final ZonedDateTime startOfMarket = (ZonedDateTime) NSE_START_TIME.adjustInto(date);
		final Duration duration = timeframe.getDuration();
		return startOfMarket.plus(duration.multipliedBy(Duration.between(startOfMarket, date).dividedBy(duration)));
	}
	
}
