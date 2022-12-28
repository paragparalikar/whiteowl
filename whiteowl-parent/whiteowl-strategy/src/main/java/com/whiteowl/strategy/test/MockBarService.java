package com.whiteowl.strategy.test;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;

public class MockBarService implements BarService {

	@Override
	public List<Bar> findByCodeAndTimeframe(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Bar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveAll(String code, Timeframe timeframe, Collection<Bar> bars) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Optional<Bar> findLatestBar(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
