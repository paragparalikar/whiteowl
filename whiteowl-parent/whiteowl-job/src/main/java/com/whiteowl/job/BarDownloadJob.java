package com.whiteowl.job;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarDataProvider;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.BarsDownloadedEvent;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.TradingSessionAwareBarDataProvider;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BarDownloadJob {

	private final BarService barService;
	private final ScripService scripService;
	private final BarDataProvider barDataProvider;
	private final ApplicationEventPublisher eventPublisher;
	
	@Async @Scheduled(cron = "20 0 9 * * MON-FRI") public void downloadD() { download(Timeframe.D); }
	@Async @Scheduled(cron = "15 15 10-17 * * MON-FRI") public void downloadH1() { download(Timeframe.H1); }
	@Async @Scheduled(cron = "10 15,45 9-17 * * MON-FRI") public void downloadM30() { download(Timeframe.M30); }
	@Async @Scheduled(cron = "5 0/15 9-16 * * MON-FRI") public void downloadM15() { download(Timeframe.M15); }
	@Async @Scheduled(cron = "3 5-59/10 9-16 * * MON-FRI") public void downloadM10() { download(Timeframe.M10); }
	@Async @Scheduled(cron = "2 0/5 9-16 * * MON-FRI") public void downloadM5() { download(Timeframe.M5); }
	
	private void download(Timeframe timeframe) {
		final TradingSessionAwareBarDataProvider smartDataProvider = 
				new TradingSessionAwareBarDataProvider(barDataProvider);
		scripService.findAll().stream()
			.filter(getScripCriteria())
			.forEach(scrip -> download(scrip, timeframe, smartDataProvider));
	}
	
	private ScripCriteria getScripCriteria() {
		return new ScripCriteria().withIndex(Index.NIFTY50);
	}
	
	private void download(Scrip scrip, Timeframe timeframe, BarDataProvider barDataProvider) {
		final ZonedDateTime to = ZonedDateTime.now();
		final ZonedDateTime from = getLastDownloadTimestamp(scrip, timeframe);
		final List<Bar> bars = barDataProvider.getBars(scrip, timeframe, from, to);
		if(!bars.isEmpty()) {
			barService.saveAll(scrip.getCode(), timeframe, bars);
			final Bar lastBar = bars.get(bars.size() - 1);
			final Duration duration = lastBar.getTimePeriod();
			final ZonedDateTime endTime = lastBar.getBeginTime().plus(duration);
			eventPublisher.publishEvent(BarsDownloadedEvent.builder()
					.scrip(scrip)
					.timeframe(timeframe)
					.timestamp(endTime)
					.build());
		}
	}
	
	private ZonedDateTime getLastDownloadTimestamp(Scrip scrip, Timeframe timeframe) {
		return barService.findMaxBeginTimeByCodeAndTimeframe(scrip.getCode(), timeframe)
				//.map(time -> time.plus(timeframe.getDuration())) 
				// We want first bar to be re-fetched as it may not have been completely formed when fetched last time.
				.orElse(ZonedDateTime.now().minusYears(100));
	}
	
}
