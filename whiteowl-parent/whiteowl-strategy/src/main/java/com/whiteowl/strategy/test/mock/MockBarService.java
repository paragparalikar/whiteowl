package com.whiteowl.strategy.test.mock;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
public class MockBarService implements BarService {
	
	private int index;
	@NonNull private final String code;
	@NonNull private final List<Bar> bars;
	@NonNull private final Timeframe timeframe;
	
	public boolean next() {
		if(index < bars.size() - 1) {
			index++;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public List<Bar> findByCodeAndTimeframe(String code, Timeframe timeframe) {
		return bars.subList(0, index);
	}

	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		return Optional.of(bars.get(index));
	}

	@Override
	public List<Bar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count) {
		return index < count ? Collections.emptyList() : bars.subList(index - (int) count, index);
	}

	@Override
	public void saveAll(String code, Timeframe timeframe, Collection<Bar> bars) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Bar> findLatestBar(String code, Timeframe timeframe) {
		return Optional.of(bars.get(index));
	}
	
}
