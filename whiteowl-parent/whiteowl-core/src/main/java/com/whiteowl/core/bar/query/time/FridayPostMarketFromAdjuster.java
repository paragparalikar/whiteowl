package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_END_TIME;
import static com.whiteowl.core.util.Constant.NSE_START_TIME;
import static java.time.DayOfWeek.FRIDAY;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class FridayPostMarketFromAdjuster implements BarQueryTimeAdjuster {

	@Override
	public boolean test(ZonedDateTime date) {
		return FRIDAY.equals(date.getDayOfWeek()) && date.toLocalTime().isAfter(NSE_END_TIME);
	}
	
	@Override
	public Temporal adjustInto(Temporal temporal) {
		return NSE_START_TIME.adjustInto(temporal.plus(Duration.ofDays(3)));
	}

}
