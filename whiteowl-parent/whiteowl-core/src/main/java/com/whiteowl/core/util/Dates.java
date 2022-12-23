package com.whiteowl.core.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Dates {

	private Dates() {}
	
	public static LocalDateTime toLocalDateTime(long epochMillis) {
		final ZoneId zoneId = ZoneId.systemDefault();
		final Instant instant = Instant.ofEpochMilli(epochMillis);
		return LocalDateTime.ofInstant(instant, zoneId);
	}

}
