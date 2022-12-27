package com.whiteowl.core.bar.query.time;

import static com.whiteowl.core.util.Constant.NSE_START_TIME;
import static java.time.DayOfWeek.SATURDAY;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

public class SaturdayFromAdjuster implements BarQueryTimeAdjuster {

	@Override
	public boolean test(ZonedDateTime date) {
		return SATURDAY.equals(date.getDayOfWeek());
	}

	@Override
	public Temporal adjustInto(Temporal temporal) {
		return NSE_START_TIME.adjustInto(temporal.plus(Duration.ofDays(2)));
	}

}
