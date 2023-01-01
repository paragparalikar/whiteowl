package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultBarService implements BarService {

	private final BarRepository barRepository;
	
	@Override
	public BarSeries findByCodeAndTimeframe(String code, Timeframe timeframe) {
		final List<Bar> bars = barRepository.findByCodeAndTimeframe(code, timeframe);
		return new BaseBarSeries(bars);
	}
	
	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe){
		return barRepository.findMaxBeginTimeByCodeAndTimeframe(code, timeframe);
	}
	
	@Override
	public Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		return barRepository.findTopByCodeAndTimeframeOrderByTimeframeDesc(code, timeframe);
	}
	
	@Override
	public BarSeries findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count){
		final List<Bar> bars = barRepository.findLatestByCodeAndTimeframe(code, timeframe, count);
		return new BaseBarSeries(bars);
	}
	
	@Override
	public void saveAll(String code, Timeframe timeframe, Collection<Bar> bars) {
		barRepository.saveAll(code, timeframe, bars);
	}
	
	@Override
	public Optional<Bar> findLatestBar(String code, Timeframe timeframe){
		return findTopByCodeAndTimeframeOrderByTimeframeDesc(code, timeframe);
	}

}
