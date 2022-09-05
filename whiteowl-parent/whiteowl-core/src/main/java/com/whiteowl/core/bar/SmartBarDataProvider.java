package com.whiteowl.core.bar;


import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Constant;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SmartBarDataProvider implements BarDataProvider {

	@NonNull private final BarDataProvider delegate;

	@Override
	public List<PersistentBar> getBars(Scrip scrip, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to) {
		if(Timeframe.D.equals(timeframe)) {
			to = to.truncatedTo(ChronoUnit.DAYS);
			from = from.truncatedTo(ChronoUnit.DAYS);
		}
		final ZonedDateTime snappedTo = snapTo(to);
		final ZonedDateTime snappedFrom = snapFrom(from);
		final long seconds = ChronoUnit.SECONDS.between(snappedFrom, snappedTo);
		if(seconds > timeframe.getDuration().toSeconds() / 2) {
			return delegate.getBars(scrip, timeframe, from, to);
		}
		log.info("DataProvider invocation skipped - Timeframe : {}, code : {}, from : {}, to : {}", 
				timeframe, scrip.getCode(), from, to);
		return Collections.emptyList();
	}
	
	private ZonedDateTime snapFrom(ZonedDateTime from) {
		if(DayOfWeek.SUNDAY.equals(from.getDayOfWeek())) {  	// If Sunday, move to Monday 9:15
			return from.plusDays(1).withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE);
		}
		if(DayOfWeek.SATURDAY.equals(from.getDayOfWeek())) {	// If Saturday, move to Monday 9:15
			return from.plusDays(2).withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE);
		}
		if(DayOfWeek.FRIDAY.equals(from.getDayOfWeek()) &&		// If Friday after market hours, move to Monday 9:15
				from.isAfter(from.withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE))) {
			return from.plusDays(3).withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE);
		}
		if(from.isAfter(from.withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE))) {	// If weekday after market hours, move to next day 9:15
			return from.plusDays(1).withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE);
		}
		if(from.isBefore(from.withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE))) {	// If weekday before market hours, move to same day 9:15
			return from.withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE);
		}
		return from;											// Since no condition matched, we are in middle of market hours
	}
	
	private ZonedDateTime snapTo(ZonedDateTime to) {
		if(DayOfWeek.SUNDAY.equals(to.getDayOfWeek())) {  	// If Sunday, move to Friday 3:35
			return to.minusDays(2).withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE);
		}
		if(DayOfWeek.SATURDAY.equals(to.getDayOfWeek())) {	// If Saturday, move to Friday 3:35
			return to.minusDays(1).withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE);
		}
		if(DayOfWeek.MONDAY.equals(to.getDayOfWeek()) && 		// If Monday before market hours, move to Friday 3.35
				to.isBefore(to.withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE))) {
			return to.minusDays(3).withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE);
		}
		if(to.isAfter(to.withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE))) {	// If weekday after market hours, move to same day day 3:35
			return to.withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE);
		}
		if(to.isBefore(to.withHour(Constant.NSE_START_HOUR).withMinute(Constant.NSE_START_MINUTE))) {	// If weekday before market hours, move to previous day 3:35
			return to.minusDays(1).withHour(Constant.NSE_STOP_HOUR).withMinute(Constant.NSE_STOP_MINUTE);
		}
		return to;											// Since no condition matched, we are in middle of market hours
	}
	
}
