package com.whiteowl.job;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionChainDownloadJob {

	private final ScripService scripService;
	private final OptionChainService optionChainService;
	private final OptionChainProvider optionChainProvider;
	
	@Scheduled(cron = "0/15 * 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void run() {
		if(shouldDownload()) {
			getUnderlyings()
				.map(optionChainProvider::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(optionChainService::save);
		}
	}
	
	private Stream<Scrip> getUnderlyings(){
		return Stream.of(scripService.findByCode(Index.NIFTY50.getCode()));
	}
	
	private boolean shouldDownload() {
		final LocalDate now = LocalDate.now();
		return !DayOfWeek.SATURDAY.equals(now.getDayOfWeek()) &&
				!DayOfWeek.SUNDAY.equals(now.getDayOfWeek());
	}

}
