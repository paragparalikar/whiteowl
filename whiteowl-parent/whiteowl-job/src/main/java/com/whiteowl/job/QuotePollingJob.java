package com.whiteowl.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.quote.DefaultQuoteService;

import lombok.RequiredArgsConstructor;

//TODO Use websockets instead of polling

@Component
@RequiredArgsConstructor
public class QuotePollingJob {

	private final DefaultQuoteService defaultQuoteService; // Concrete class as interface can not expose this api
	
	@Scheduled(
			initialDelayString = "${whiteowl.quote.poll.initial-delay:PT15S}", 
			fixedRateString = "${whiteowl.quote.poll.fixed-rate:PT15S}")
	public void poll() {
		defaultQuoteService.poll();
	}
}
