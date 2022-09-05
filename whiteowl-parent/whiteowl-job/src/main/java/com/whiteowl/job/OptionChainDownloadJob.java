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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionChainDownloadJob {

	private final OptionChainService optionChainService;
	private final OptionChainProvider optionChainProvider;
	
	@Scheduled(cron = "0/15 * 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void run() {
		if(shouldDownload()) {
			getUnderlyingCodes()
				.map(optionChainProvider::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(optionChainService::save);
		}
	}
	
	private Stream<String> getUnderlyingCodes(){
		return Stream.of("NIFTY");
	}
	
	private boolean shouldDownload() {
		final LocalDate now = LocalDate.now();
		return !DayOfWeek.SATURDAY.equals(now.getDayOfWeek()) &&
				!DayOfWeek.SUNDAY.equals(now.getDayOfWeek());
	}

}
