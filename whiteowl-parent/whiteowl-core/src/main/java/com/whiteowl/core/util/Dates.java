package com.whiteowl.core.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.NonNull;

public final class Dates {

	private Dates() {}
	
	public static LocalDateTime toLocalDateTime(long epochMillis) {
		final ZoneId zoneId = ZoneId.systemDefault();
		final Instant instant = Instant.ofEpochMilli(epochMillis);
		return LocalDateTime.ofInstant(instant, zoneId);
	}
	
	public static ZonedDateTime truncateToDuration(
			@NonNull final ZonedDateTime zonedDateTime, 
			@NonNull final Duration duration) {
	    final ZonedDateTime startOfDay = zonedDateTime.truncatedTo(ChronoUnit.DAYS);
	    return startOfDay.plus(duration.multipliedBy(
	            Duration.between(startOfDay, zonedDateTime).dividedBy(duration)));
	}

}
