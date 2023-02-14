package com.whiteowl.core.bar;

import java.util.Collection;
import java.util.List;

import org.ta4j.core.Bar;

public interface BarRepository {
	
	void saveAll(String code, Timeframe timeframe, Collection<Bar> bars);
	
	List<Bar> findLatestByCodeAndTimeframeOrderByBeginTimeAsc(String code, Timeframe timeframe, int count);
	
}

