package com.whiteowl.job;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

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
import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.bar.query.BarQuery;
import com.whiteowl.core.bar.query.BarQueryTransformer;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(value = "prototype")
@Order(JobConstant.ORDER_BAR_BACKFILL)
public class BarBackfillJob implements CommandLineRunner {

	private final BarService barService;
	private final ScripService scripService;
	private final BarDataProvider barDataProvider;
	private final BarQueryTransformer barQueryTransformer;
	private final ApplicationEventPublisher eventPublisher;
	
	@Override
	public void run(String... args) throws Exception {
		Arrays.stream(Timeframe.values()).forEach(this::download);
	}
	
	private void download(Timeframe timeframe) {
		scripService.findAll().stream()
			.filter(getScripCriteria())
			.forEach(scrip -> download(scrip, timeframe, barDataProvider));
	}
	
	private Predicate<Scrip> getScripCriteria() {
		return new ScripCriteria().withIndex(Index.NIFTY50)
				.or(new ScripCriteria()
						.withCode(Index.NIFTY50.getCode())
						.withCode(Index.NIFTYBANK.getCode())
						.withCode(Index.VIX.getCode()));
	}
	
	private void download(Scrip scrip, Timeframe timeframe, BarDataProvider barDataProvider) {
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
		final List<Bar> bars = barQueryTransformer.transform(barQuery)
				.map(barDataProvider::getBars)
				.orElse(Collections.emptyList());
		if(!bars.isEmpty()) {
			barService.saveAll(scrip.getCode(), timeframe, bars);
			eventPublisher.publishEvent(new ScripBarDownloadedEvent(scrip, timeframe));
		}
	}
	
	private ZonedDateTime getLastDownloadTimestamp(Scrip scrip, Timeframe timeframe) {
		final BarSeries barSeries = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(scrip.getCode(), timeframe, 1);
		return 0 < barSeries.getBarCount() ? barSeries.getLastBar().getBeginTime() : ZonedDateTime.now().minusYears(100);
	}
	
}
