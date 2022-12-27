package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_END_TIME;
import static java.time.DayOfWeek.SUNDAY;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class SundayToAdjuster implements BarQueryTimeAdjuster {

	@Override
	public boolean test(ZonedDateTime date) {
		return SUNDAY.equals(date.getDayOfWeek());
	}

	@Override
	public Temporal adjustInto(Temporal temporal) {
		return NSE_END_TIME.adjustInto(temporal).minus(Duration.ofDays(2));
	}

}
