package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.ta4j.core.Bar;

public interface BarRepository {
	
	Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe);
	
	List<Bar> findByCodeAndTimeframe(String code, Timeframe timeframe);
	
	List<Bar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count);

	Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe);

	void saveAll(String code, Timeframe timeframe, Collection<Bar> bars);
}

