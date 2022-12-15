package com.whiteowl.job;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whiteowl.core.prediction.PredictionProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PredictionProviderRebuildJob {

	private final Duration ttl = Duration.ofDays(7);
	private final List<PredictionProvider> predictionProviders;
	
	@Scheduled(cron = "0 0 9 * * *")
	//@EventListener(ApplicationReadyEvent.class)
	public void run() {
		predictionProviders.forEach(predictionProvider -> predictionProvider.rebuild(ttl));
	}
	
}
