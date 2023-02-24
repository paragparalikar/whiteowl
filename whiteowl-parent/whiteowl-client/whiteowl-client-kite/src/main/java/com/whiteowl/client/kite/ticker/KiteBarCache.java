package com.whiteowl.client.kite.ticker;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.core.util.Dates;
import com.whiteowl.core.util.Tuple3;

import lombok.NonNull;

public class KiteBarCache {
	private static final List<Duration> DURATIONS = Arrays.asList(
			Duration.ofMinutes(1), Duration.ofMinutes(3), Duration.ofMinutes(5), 
			Duration.ofMinutes(10), Duration.ofMinutes(15), Duration.ofMinutes(30),
			Duration.ofHours(1));
	private final BarSeriesByInstrumentToken cache = new BarSeriesByInstrumentToken();
	private final Set<Consumer<Tuple3<Long, Duration, Bar>>> barCompletionListeners  = 
			Collections.newSetFromMap(new IdentityHashMap<>());
	
	public void addBarCompletionListener(@NonNull final Consumer<Tuple3<Long, Duration, Bar>> barListener) {
		barCompletionListeners.add(barListener);
	}
	
	private void notifyBarCompletionListeners(Long instrumentToken, Duration duration, Bar bar) {
		final Tuple3<Long, Duration, Bar> tuple = Tuple3.of(instrumentToken, duration, bar);
		barCompletionListeners.forEach(listener -> listener.accept(tuple));
	}
	
	public BarSeries getBarSeries(@NonNull final Instrument instrument, @NonNull final Duration duration) {
		final BarSeries barSeries = cache.getBarSeries(instrument.getInstrumentToken(), duration);
		synchronized(barSeries) { // Get the lock on barSeries to make sure it is not currently being modified before returning
			return barSeries;
		}
	}
	
	public void addTick(@NonNull final KiteTick tick) {
		for(Duration duration : DURATIONS) {
			final BarSeries barSeries = cache.getBarSeries(tick.getToken(), duration);
			synchronized(barSeries) {
				if(barSeries.isEmpty()) {
					barSeries.addBar(toBar(tick, duration));
				} else {
					final Bar bar = barSeries.getLastBar();
					final ZonedDateTime tickTime = ZonedDateTime.of(tick.getLastTradedTime(), 
							ZoneId.systemDefault()).plus(duration);
					if(!tickTime.isBefore(bar.getEndTime())) {
						barSeries.addBar(toBar(tick, duration));
						notifyBarCompletionListeners(tick.getToken(), duration, bar);
					} else {
						final Num lastTradedPrice = DoubleNum.valueOf(tick.getLastTradedPrice());
						final Num lastTradedQuantity = DoubleNum.valueOf(tick.getLastTradedQuantity());
						barSeries.addTrade(lastTradedQuantity, lastTradedPrice);
					}
				}
			}
		}
	}
	
	private Bar toBar(KiteTick tick, Duration duration) {
		final Num lastTradedPrice = DoubleNum.valueOf(tick.getLastTradedPrice());
		final Num lastTradedQuantity = DoubleNum.valueOf(tick.getLastTradedQuantity());
		final ZonedDateTime beginTime = Dates.truncateToDuration(
				ZonedDateTime.of(tick.getLastTradedTime(), ZoneId.systemDefault()), 
				duration);
		final ZonedDateTime endTime = beginTime.plus(duration);
		return BaseBar.builder()
				.openPrice(lastTradedPrice)
				.highPrice(lastTradedPrice)
				.lowPrice(lastTradedPrice)
				.closePrice(lastTradedPrice)
				.volume(lastTradedQuantity)
				.endTime(endTime)
				.timePeriod(duration)
				.build();
	}

}

class BarSeriesByInstrumentToken {
	
	private final Map<Long, BarSeriesByDuration> cache = new ConcurrentHashMap<>();
	
	public BarSeries getBarSeries(Long instrumentToken, Duration duration) {
		return cache.computeIfAbsent(instrumentToken, key -> new BarSeriesByDuration()).getBarSeries(duration);
	}
}

class BarSeriesByDuration {
	
	private final Map<Duration, BarSeries> cache = new ConcurrentHashMap<>();
	
	public BarSeries getBarSeries(Duration duration) {
		return cache.computeIfAbsent(duration, key -> new BaseBarSeries("", DoubleNum::valueOf));
	}
}