package com.whiteowl.client.kite.adapter.mapper;

import java.time.Duration;

import com.whiteowl.core.bar.Timeframe;

public class TimeframeMapper {

	public String toInterval(Timeframe timeframe) {
		if(null == timeframe) return null;
		switch(timeframe) {
		case D:return "day";
		case H1:return "60minute";
		case M10:return "10minute";
		case M15:return "15minute";
		case M30:return "30minute";
		case M5:return "5minute";
		case M1: return "minute";
		default:return null;
		}
	}
	
	public Duration getHistoricalDataBatchLimit(Timeframe timeframe) {
		if(null == timeframe) return null;
		switch(timeframe) {
		case D:return Duration.ofDays(2000);
		case H1:return Duration.ofDays(365);
		case M30:return Duration.ofDays(180);
		case M15:return Duration.ofDays(180);
		case M10:return Duration.ofDays(90);
		case M5:return Duration.ofDays(90);
		case M1: return Duration.ofDays(60);
		default: return Duration.ofDays(90);
		}
	}
	
}
