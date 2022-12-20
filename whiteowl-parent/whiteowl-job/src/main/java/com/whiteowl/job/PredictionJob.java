package com.whiteowl.job;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.prediction.PredictionCreatedEvent;
import com.whiteowl.core.prediction.PredictionProvider;
import com.whiteowl.core.prediction.PredictionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PredictionJob {

	private final PredictionService predictionService;
	private final PredictionProvider predictionProvider;
	private final ApplicationEventPublisher eventPublisher;
	
	@EventListener
	public void run(ScripBarDownloadedEvent event) {
		predictionProvider.predict(event.getScrip(), event.getTimeframe()).stream()
			.map(predictionService::save)
			.map(PredictionCreatedEvent::new)
			.forEach(eventPublisher::publishEvent);
	}

}
