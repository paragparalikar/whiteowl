package com.whiteowl.job;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.BarDataProvider;
import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.PersistentBar;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BarDownloadJob {

	private final BarService barService;
	private final ScripService scripService;
	private final BarDataProvider barDataProvider;
	
	@Async @Scheduled(cron = "20 0 9 * * MON-FRI") public void downloadD() { download(Timeframe.D); }
	@Async @Scheduled(cron = "15 15 10-17 * * MON-FRI") public void downloadH1() { download(Timeframe.H1); }
	@Async @Scheduled(cron = "10 15,45 9-17 * * MON-FRI") public void downloadM30() { download(Timeframe.M30); }
	@Async @Scheduled(cron = "5 0/15 9-16 * * MON-FRI") public void downloadM15() { download(Timeframe.M15); }
	@Async @Scheduled(cron = "3 5-59/10 9-16 * * MON-FRI") public void downloadM10() { download(Timeframe.M10); }
	@Async @Scheduled(cron = "2 0/5 9-16 * * MON-FRI") public void downloadM5() { download(Timeframe.M5); }
	
	private void download(Timeframe timeframe) {
		scripService.findAll().stream()
			.filter(getScripCriteria())
			.forEach(scrip -> download(scrip, timeframe));
	}
	
	private ScripCriteria getScripCriteria() {
		return new ScripCriteria().withIndex(Index.NIFTY50);
	}
	
	private void download(Scrip scrip, Timeframe timeframe) {
		final ZonedDateTime to = ZonedDateTime.now();
		final ZonedDateTime from = getLastDownloadTimestamp(scrip, timeframe);
		final List<PersistentBar> bars = barDataProvider.getBars(scrip, timeframe, from, to);
		if(!bars.isEmpty()) {
			bars.forEach(barService::saveSafe);
			log.info("{} - {} : Downloaded {} bars", timeframe, scrip.getName(), bars.size());
		}
	}
	
	private ZonedDateTime getLastDownloadTimestamp(Scrip scrip, Timeframe timeframe) {
		return barService.findMaxBeginTimeByCodeAndTimeframe(scrip.getCode(), timeframe)
				.map(time -> time.plus(timeframe.getDuration()))
				.orElse(ZonedDateTime.now().minusYears(10));
	}
	
}
