package com.whiteowl.strategy.test.mock;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;

import lombok.NonNull;
import lombok.Setter;

@Setter
public class MockBarService implements BarService {
	
	private int index;
	@NonNull private String code;
	@NonNull private List<Bar> bars;
	@NonNull private Timeframe timeframe;
	
	public void setIndex(int index) {
		if(null == bars || bars.isEmpty()) throw new IllegalStateException();
		if(0 > index || index >= bars.size()) throw new IllegalArgumentException();
		this.index = index;
	}

	@Override
	public List<Bar> findByCodeAndTimeframe(@NonNull final String code, @NonNull final Timeframe timeframe) {
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		return bars;
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
		if(!code.equalsIgnoreCase(this.code)) throw new IllegalArgumentException();
		if(!timeframe.equals(this.timeframe)) throw new IllegalArgumentException();
		if(null == bars || bars.isEmpty()) throw new IllegalArgumentException();
		this.bars = new ArrayList<>(bars);
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
