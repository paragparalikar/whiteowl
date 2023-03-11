package com.whiteowl.job;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.BarDataProvider;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.query.BarQuery;
import com.whiteowl.core.bar.query.BarQueryTransformer;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(value = "prototype")
@Order(JobConstant.ORDER_BAR_BACKFILL)
public class BarBackfillJob implements CommandLineRunner {
	
	@Value
	public static class BarsBackfilledEvent {
		@NonNull private final Scrip scrip;
		@NonNull private final List<Bar> bars;
		@NonNull private final Timeframe timeframe;
	}
	
	@Value
	public static class ScripBarsBackfilledEvent {
		@NonNull private final Scrip scrip;
	}

	private final BarService barService;
	private final ScripService scripService;
	private final BarDataProvider barDataProvider;
	private final BarQueryTransformer barQueryTransformer;
	private final ApplicationEventPublisher eventPublisher;
	
	@Override
	public void run(String... args) throws Exception {
		scripService.findAll().stream()
			.filter(ScripCriteria.INSTANCE)
			.forEach(scrip -> download(scrip, barDataProvider));
	}
	
	private void download(Scrip scrip, BarDataProvider barDataProvider) {
		for(Timeframe timeframe : Timeframe.values()) {
			final List<Bar> bars = download(scrip, timeframe, barDataProvider);
			if(!bars.isEmpty()) {
				barService.saveAll(scrip.getCode(), timeframe, bars);
				eventPublisher.publishEvent(new BarsBackfilledEvent(scrip, bars, timeframe));
			}
		}
		eventPublisher.publishEvent(new ScripBarsBackfilledEvent(scrip));
	}
	
	private List<Bar> download(Scrip scrip, Timeframe timeframe, BarDataProvider barDataProvider) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime lastDownloadedTime = getLastDownloadTimestamp(scrip, timeframe);
		final ZonedDateTime nextDownloadTime = lastDownloadedTime.plus(timeframe.getDuration());
		if(log.isDebugEnabled()) log.debug("Bars were last downloaded on {} for scrip {} and timeframe {}, next is {}", 
				lastDownloadedTime, scrip.getCode(), timeframe, nextDownloadTime);
		final BarQuery barQuery = BarQuery.builder()
				.to(now)
				.from(nextDownloadTime)
				.scrip(scrip)
				.timeframe(timeframe)
				.build();
		return barQueryTransformer.transform(barQuery)
				.map(barDataProvider::getBars)
				.orElse(Collections.emptyList());
	}
	
	private ZonedDateTime getLastDownloadTimestamp(Scrip scrip, Timeframe timeframe) {
		final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(scrip.getCode(), timeframe, 1);
		return 0 < barSeries.getBarCount() ? barSeries.getLastBar().getBeginTime() : ZonedDateTime.now().minusYears(100);
	}
	
}
