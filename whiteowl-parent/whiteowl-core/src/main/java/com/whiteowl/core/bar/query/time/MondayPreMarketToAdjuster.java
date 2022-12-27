package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_END_TIME;
import static com.whiteowl.core.util.Constant.NSE_START_TIME;
import static java.time.DayOfWeek.MONDAY;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class MondayPreMarketToAdjuster implements BarQueryTimeAdjuster {

	@Override
	public boolean test(ZonedDateTime date) {
		return MONDAY.equals(date.getDayOfWeek()) && NSE_START_TIME.isAfter(date.toLocalTime());
	}

	@Override
	public Temporal adjustInto(Temporal temporal) {
		return NSE_END_TIME.adjustInto(temporal).minus(Duration.ofDays(3));
	}

}
