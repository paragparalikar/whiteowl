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
	
	public void setIndex(int index) {
		if(null == bars || bars.isEmpty()) throw new IllegalStateException();
		if(0 > index || index >= bars.size()) throw new IllegalArgumentException();
		this.index = index;
	}
	
	public boolean next() {
		if(index < bars.size() - 1) {
			index++;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public List<Bar> findByCodeAndTimeframe(@NonNull final String code, @NonNull final Timeframe timeframe) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		return bars.subList(0, index);
	}

	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Bar> findTopByCodeAndTimeframeOrderByTimeframeDesc(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		return Optional.of(bars.get(index));
	}

	@Override
	public List<Bar> findLatestByCodeAndTimeframe(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			long count) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		return index < count ? Collections.emptyList() : bars.subList(index - (int) count, index);
	}

	@Override
	public void saveAll(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			@NonNull final Collection<Bar> bars) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Bar> findLatestBar(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		return Optional.of(bars.get(index));
	}
	
}
