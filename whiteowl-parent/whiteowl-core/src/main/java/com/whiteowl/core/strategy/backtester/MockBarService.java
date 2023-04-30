package com.whiteowl.core.strategy.backtester;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.util.Tuple2;

public class MockBarService implements BarService {

	private Map<Tuple2<String, Timeframe>, List<Bar>> cache = new ConcurrentHashMap<>();

	@Override
	public void saveAll(String code, Timeframe timeframe, Collection<Bar> bars) {
		final Tuple2<String, Timeframe> tuple = Tuple2.of(code, timeframe);
		final List<Bar> cacheBars = cache.computeIfAbsent(tuple, key -> new ArrayList<>(bars.size()));
		bars.stream().sorted(Comparator.comparing(Bar::getBeginTime)).forEach(cacheBars::add);
	}

	@Override
	public Bar findByCodeAndTimeframeAndBeginTime(String code, Timeframe timeframe, ZonedDateTime beginTime) {
		return Optional.ofNullable(cache.get(Tuple2.of(code, timeframe)))
			.map(Collection::stream)
			.orElse(Stream.empty())
			.filter(bar -> beginTime.equals(bar.getBeginTime()))
			.findFirst().orElse(null);
	}

	@Override
	public BarSeries findLatestByCodeAndTimeframeOrderByBeginTimeAsc(String code, Timeframe timeframe, int count) {
		final List<Bar> bars = Optional.ofNullable(cache.get(Tuple2.of(code, timeframe)))
				.orElse(Collections.emptyList());
		final int startIndex = bars.size() - count;
		return new BaseBarSeries(bars.subList(startIndex >= 0 ? startIndex : 0, bars.size()));
	}

}
