package com.whiteowl.core.bar;

import java.time.Duration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Timeframe {

	D(1, Duration.ofDays(1)), 
	H1(7, Duration.ofHours(1)), 
	M30(13, Duration.ofMinutes(30)), 
	M15(25, Duration.ofMinutes(15)), 
	M10(38, Duration.ofMinutes(10)), 
	M5(75, Duration.ofMinutes(5));
	
	public static Timeframe findByDuration(Duration duration) {
		final long minutes = duration.toMinutes();
		if(5 == minutes) return M5;
		if(10 == minutes) return M10;
		if(15 == minutes) return M15;
		if(30 == minutes) return M30;
		if(60 == minutes) return H1;
		if(1440 == minutes) return D;
		else throw new IllegalArgumentException("Invalid duration value, minutes :  " + minutes);
	}
	
	private final int dayMultiple;
	private final Duration duration;
}
