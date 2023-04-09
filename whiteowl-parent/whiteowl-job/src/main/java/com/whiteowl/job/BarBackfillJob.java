package com.whiteowl.job;

import static com.whiteowl.core.bar.Timeframe.M3;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("data-backfill")
@RequiredArgsConstructor
@Scope(value = "prototype")
@Order(JobConstant.ORDER_BAR_BACKFILL)
public class BarBackfillJob implements CommandLineRunner {

	private final BarService barService;
	private final ScripService scripService;
	private final BarDataProvider barDataProvider;
	private final BarQueryTransformer barQueryTransformer;
	
	@Override
	public void run(String... args) throws Exception {
		scripService.findAll().stream()
			.filter(this::predicate)
			.forEach(scrip -> download(scrip, barDataProvider));
	}
	
	private boolean predicate(Scrip scrip) {
		return Index.isIndex(scrip.getCode())
				|| scrip.isUnderlying() 
				|| (scrip.isOption() 
				&& Exchange.NFO.equals(scrip.getExchange())
				&& null != scrip.getExpiry()
				&& scrip.getExpiry().isBefore(LocalDate.now().plusWeeks(5)));
	}
	
	private void download(Scrip scrip, BarDataProvider barDataProvider) {
		for(Timeframe timeframe : Arrays.asList(M3)) {
			final List<Bar> bars = download(scrip, timeframe, barDataProvider);
			if(!bars.isEmpty()) {
				barService.saveAll(scrip.getCode(), timeframe, bars);
				log.info("Downloaded {} bars for {} at {} timeframe", bars.size(), scrip.getCode(), timeframe);
			}
		}
	}
	
	private List<Bar> download(Scrip scrip, Timeframe timeframe, BarDataProvider barDataProvider) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime lastDownloadedTime = getLastDownloadTimestamp(scrip, timeframe);
		final ZonedDateTime nextDownloadTime = lastDownloadedTime.plus(timeframe.getDuration());
		log.info("Bars were last downloaded on {} for scrip {} and timeframe {}, next is {}", 
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
