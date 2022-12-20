package com.whiteowl.job;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.event.ScripBarDownloadedEvent;
import com.whiteowl.core.bar.event.TimeframeBarDownloadedEvent;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.event.PositionSynchronizedEvent;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public class TradingStrategyExecutionJob {

	private final TaskScheduler taskScheduler;
	private final List<TradingStrategyExecutor> tradingStrategyExecutors;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	
	@EventListener
	public void handle(final ApplicationReadyEvent event) {
		for(TradingStrategyExecutor executor : tradingStrategyExecutors) {
			final TradingStrategyTemplate template = executor.getTradingStrategyTemplate();
			for(TradingStrategyConfig config : tradingStrategyConfigService.findByEnabledAndTemplate(true, template)) {
				try {
					executor.execute(config, taskScheduler);
				}catch(Exception e) {
					log.error("", e);
				}
			}
		}
	}
	
	@EventListener
	public void handle(final PositionSynchronizedEvent event) {
		final Position position = event.getPosition();
		final String configId = position.getTradingStrategyConfigId();
		final TradingStrategyConfig config = tradingStrategyConfigService.findById(configId).orElseThrow();
		tradingStrategyExecutors.stream()
			.filter(executor -> Objects.equals(
					executor.getTradingStrategyTemplate(), 
					config.getTradingStrategyTemplate()))
			.findFirst().ifPresent(executor -> {
				try {
					executor.execute(config, position);
				}catch(Exception e) {
					log.error("", e);
				}
			});
	}

	@EventListener
	public void handle(final TimeframeBarDownloadedEvent event) {
		for(TradingStrategyExecutor executor : tradingStrategyExecutors) {
			final TradingStrategyTemplate template = executor.getTradingStrategyTemplate();
			for(TradingStrategyConfig config : tradingStrategyConfigService.findByEnabledAndTemplate(true, template)) {
				try {
					executor.execute(config, event.getTimeframe());
				}catch(Exception e) {
					log.error("", e);
				}
			}
		}
	}
	
	@EventListener
	public void handle(final ScripBarDownloadedEvent event) {
		for(TradingStrategyExecutor executor : tradingStrategyExecutors) {
			final TradingStrategyTemplate template = executor.getTradingStrategyTemplate();
			for(TradingStrategyConfig config : tradingStrategyConfigService.findByEnabledAndTemplate(true, template)) {
				try {
					executor.execute(config, event.getScrip(), event.getTimeframe());
				}catch(Exception e) {
					log.error("", e);
				}
			}
		}
	}
	
}
