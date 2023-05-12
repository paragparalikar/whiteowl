package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.Collection;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultBarService implements BarService {

	private final BarRepository barRepository;
	
	@Override
	public BarSeries findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			int limit, final int offset) {
		return new BaseBarSeries(barRepository.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(code, 
				timeframe, limit, offset));
	}
	
	@Override
	public void saveAll(String code, Timeframe timeframe, Collection<Bar> bars) {
		barRepository.saveAll(code, timeframe, bars);
	}
	
	@Override
	public Bar findByCodeAndTimeframeAndBeginTime(
			String code, Timeframe timeframe, ZonedDateTime beginTime) {
		return barRepository.findByCodeAndTimeframeAndBeginTime(code, timeframe, beginTime);
	}

}
