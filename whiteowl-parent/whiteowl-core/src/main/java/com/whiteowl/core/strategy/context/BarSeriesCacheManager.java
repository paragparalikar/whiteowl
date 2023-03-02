package com.whiteowl.core.strategy.context;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Constant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarSeriesCacheManager {
	
	private final BarService barService;
	private final Map<String, BarSeries> cache = new ConcurrentHashMap<>();
	private final Map<String, Set<Consumer<BarSeries>>> barListeners = new ConcurrentHashMap<>();

	private String toCacheKey(String scripCode, Timeframe timeframe) {
		return scripCode + timeframe.name();
	}
	
	public void subscribeBarListener(Scrip scrip, Timeframe timeframe, Consumer<BarSeries> barListener) {
		final String key = toCacheKey(scrip.getCode(), timeframe);
		barListeners.computeIfAbsent(key, 
				k -> Collections.newSetFromMap(new IdentityHashMap<>()))
				.add(barListener);
		barListener.accept(getBarSeries(scrip.getCode(), timeframe));
	}
	
	public void unsubscribeBarListener(Consumer<BarSeries> barListener) {
		barListeners.values().forEach(listeners -> listeners.remove(barListener));
		final Iterator<Entry<String, Set<Consumer<BarSeries>>>> iterator = barListeners.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<String, Set<Consumer<BarSeries>>> entry = iterator.next();
			if(entry.getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}
	
	@Async
	@EventListener
	public void onScripBarDownloaded(ScripBarDownloadedEvent event) {
		final Timeframe timeframe = event.getTimeframe();
		final String scripCode = event.getScrip().getCode();
		final String key = toCacheKey(scripCode, timeframe);
		Optional.ofNullable(cache.get(key)).ifPresent(barSeries -> {
			event.getBars().forEach(barSeries::addBar);
			notifyBarListener(scripCode, timeframe, barSeries);
		});
	}
	
	private void notifyBarListener(String scripCode, Timeframe timeframe, BarSeries barSeries) {
		final String key = toCacheKey(scripCode, timeframe);
		barListeners.getOrDefault(key, Collections.emptySet())
			.forEach(barListener -> {
				try {
					barListener.accept(barSeries);
				}catch(Exception e) {
					log.error("Error while notifying bar listeners", e);
				}
			});
	}
	
	public BarSeries getBarSeries(String scripCode, Timeframe timeframe) {
		return cache.computeIfAbsent(scripCode + timeframe.name(), key -> 
			barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				scripCode, timeframe, Constant.BAR_COUNT_CACHED));
	}

}
