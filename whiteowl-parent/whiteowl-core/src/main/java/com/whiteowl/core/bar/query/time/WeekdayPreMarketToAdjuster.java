package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_END_TIME;
import static com.whiteowl.core.util.Constant.NSE_START_TIME;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class WeekdayPreMarketToAdjuster implements BarQueryTimeAdjuster {

	@Override
	public boolean test(ZonedDateTime date) {
		final DayOfWeek dayOfWeek = date.getDayOfWeek();
		return NSE_START_TIME.isAfter(date.toLocalTime()) 
				&& (TUESDAY.equals(dayOfWeek) 
				|| WEDNESDAY.equals(dayOfWeek)
				|| THURSDAY.equals(dayOfWeek)
				|| FRIDAY.equals(dayOfWeek));
	}

	@Override
	public Temporal adjustInto(Temporal temporal) {
		return NSE_END_TIME.adjustInto(temporal.minus(Duration.ofDays(1)));
	}

}
