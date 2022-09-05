package com.whiteowl.job;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.attribute.AttributeService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripDataProvider;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScripDownloadJob {
	private static final String KEY = "whiteowl.data.download.scrips.nse.date";
	private final ScripService scripService;
	private final AttributeService attributeService;
	private final ScripDataProvider scripDataProvider;
	
	@Scheduled(cron = "0 0 9 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		if(shouldDownload()) {
			log.info("Initiating scrip download");
			final List<Scrip> scrips = scripDataProvider.getAllScrips();
			scripService.saveAll(scrips);
			log.info("Downloaded {} scrips", scrips.size());
			attributeService.set(KEY, LocalDate.now());
		}
	}
	
	private boolean shouldDownload() {
		final LocalDate now = LocalDate.now();
		final LocalDate lastDownloadDate = attributeService.getLocalDate(KEY);
		return null == lastDownloadDate || 
				(lastDownloadDate.isBefore(now.minusDays(1)) &&
				!DayOfWeek.SATURDAY.equals(now.getDayOfWeek()) &&
				!DayOfWeek.SUNDAY.equals(now.getDayOfWeek()));
	}
	
}
