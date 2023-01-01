package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public interface BarService {

	BarSeries findByCodeAndTimeframe(String code, Timeframe timeframe);

	Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe);

	Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe);

	BarSeries findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count);

	void saveAll(String code, Timeframe timeframe, Collection<Bar> bars);

	Optional<Bar> findLatestBar(String code, Timeframe timeframe);

}