package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.Collection;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public interface BarService {

	void saveAll(String code, Timeframe timeframe, Collection<Bar> bars);
	
	Bar findByCodeAndTimeframeAndBeginTime(String code, Timeframe timeframe, ZonedDateTime beginTime);
	
	BarSeries findLatestByCodeAndTimeframeOrderByBeginTimeAsc(String code, Timeframe timeframe, int count);
	
}