package com.whiteowl.core.strategy.context;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.config.TradingStrategyConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarSeriesCacheManager {
	
	private int maxBarCount;
	private final BarService barService;
	private final TradingStrategyConfigService configService;
	private final Map<String, BarSeries> cache = new ConcurrentHashMap<>();
	private final Map<String, Set<Consumer<Bar>>> barListeners = new ConcurrentHashMap<>();
	
	@PostConstruct
	public void init() {
		this.maxBarCount = configService.findAll().stream()
				.map(TradingStrategyConfig::getMinBarCount)
				.max(Comparator.naturalOrder())
				.orElse(200);
	}

	private String toCacheKey(String scripCode, Timeframe timeframe) {
		return scripCode + timeframe.name();
	}
	
	public void subscribe(Scrip scrip, Timeframe timeframe, Consumer<Bar> barListener) {
		final String key = toCacheKey(scrip.getCode(), timeframe);
		barListeners.computeIfAbsent(key, 
				k -> Collections.newSetFromMap(new IdentityHashMap<>()))
				.add(barListener);
		getBarSeries(scrip.getCode(), timeframe); // hydrate cache immediately
	}
	
	public void unsubscribeBarListener(Consumer<Bar> barListener) {
		barListeners.values().forEach(listeners -> listeners.remove(barListener));
		final Iterator<Entry<String, Set<Consumer<Bar>>>> iterator = barListeners.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<String, Set<Consumer<Bar>>> entry = iterator.next();
			if(entry.getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}
	
	@Async
	@EventListener
	public void onBarCreated(BarCreatedEvent event) {
		final Timeframe timeframe = event.getTimeframe();
		final String scripCode = event.getScrip().getCode();
		final String key = toCacheKey(scripCode, timeframe);
		Optional.ofNullable(cache.get(key)).ifPresent(barSeries -> {
			barSeries.addBar(event.getBar());
			notifyBarListener(scripCode, timeframe, event.getBar());
		});
	}
	
	private void notifyBarListener(String scripCode, Timeframe timeframe, Bar bar) {
		final String key = toCacheKey(scripCode, timeframe);
		barListeners.getOrDefault(key, Collections.emptySet())
			.forEach(barListener -> {
				try {
					barListener.accept(bar);
				}catch(Exception e) {
					log.error("Error while notifying bar listeners", e);
				}
			});
	}
	
	public BarSeries getBarSeries(String scripCode, Timeframe timeframe) {
		return cache.computeIfAbsent(scripCode + timeframe.name(), key -> 
			barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				scripCode, timeframe, maxBarCount));
	}

}
