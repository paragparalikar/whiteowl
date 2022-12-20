package com.whiteowl.strategy.shortstrangle;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@Component
@RequiredArgsConstructor
public class ShortStraddleTradingStrategyExecutor implements TradingStrategyExecutor {

	private final ScripService scripService;
	private final OptionChainService optionChainService;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	
	@Override
	public void execute(@NonNull final TaskScheduler taskScheduler) {

	}

}
