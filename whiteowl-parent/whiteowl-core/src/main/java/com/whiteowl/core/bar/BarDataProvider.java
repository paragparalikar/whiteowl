package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.scrip.Scrip;

public interface BarDataProvider {
	
	List<Bar> getBars(Scrip scrip, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to);

}
