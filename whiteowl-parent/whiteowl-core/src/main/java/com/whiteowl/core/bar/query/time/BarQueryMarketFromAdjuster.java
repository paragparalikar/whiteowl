package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_END_TIME;
import static com.whiteowl.core.util.Constant.NSE_START_TIME;
import static java.time.DayOfWeek.FRIDAY;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.whiteowl.core.bar.Timeframe;

public class BarQueryMarketFromAdjuster implements BarQueryMarketTimeAdjuster {

	@Override
	public ZonedDateTime apply(Timeframe timeframe, ZonedDateTime date) {
		if(isLastBarBeginTime(timeframe, date)) {
			final int days = FRIDAY.equals(date.getDayOfWeek()) ? 3 : 1;
			return (ZonedDateTime) NSE_START_TIME.adjustInto(date.plus(Duration.ofDays(days)));
		} else {
			final ZonedDateTime startOfMarket = (ZonedDateTime) NSE_START_TIME.adjustInto(date);
			final Duration duration = timeframe.getDuration();
			return startOfMarket.plus(duration.multipliedBy(Duration.between(startOfMarket, date).dividedBy(duration)));
		}
	}

	private boolean isLastBarBeginTime(Timeframe timeframe, ZonedDateTime date) {
		final LocalTime endTime = date.plus(timeframe.getDuration()).toLocalTime();
		return NSE_END_TIME.equals(endTime) || NSE_END_TIME.isBefore(endTime);
	}
	
}
