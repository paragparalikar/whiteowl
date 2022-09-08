package com.whiteowl.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChainProvider;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionChainDownloadJob {

	private final ScripService scripService;
	private final OptionChainService optionChainService;
	private final OptionChainProvider optionChainProvider;
	
	@Scheduled(cron = "0/15 0 9-16 * * MON-FRI")
	@EventListener(ApplicationReadyEvent.class)
	public void tryDownload() {
		scripService.findAll().stream()
			.filter(getScripCriteria())
			.map(optionChainProvider::get)
			.forEach(future -> future.thenAccept(optionChainService::save));
	}
	
	private ScripCriteria getScripCriteria() {
		return new ScripCriteria().withCode(Index.NIFTY50.getCode());
	}
	
}
