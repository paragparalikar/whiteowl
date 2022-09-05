package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;

import com.whiteowl.core.scrip.Scrip;

public interface BarDataProvider {
	
	List<PersistentBar> getBars(Scrip scrip, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to);

}
