package com.whiteowl.strategy;

import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.shortstrangle.ShortStraddleConfig;
import com.whiteowl.strategy.shortstrangle.ShortStraddleTradingStrategy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradingStrategyFactory {

	private final BarService barService;
	private final ScripService scripService;
	private final OptionChainService optionChainService;
	
	public TradingStrategy getTradingStrategy(
			@NonNull final TradingStrategyConfig config) {
		switch(config.getTradingStrategyTemplate()) {
		case SHORT_STRADDLE: return buildShortStraddleTradingStrategy(config);
		case DONCHIAN: return null;
		}
		return null;
	}
	
	private TradingStrategy buildShortStraddleTradingStrategy(TradingStrategyConfig config) {
		final ShortStraddleConfig shortStraddleConfig = (ShortStraddleConfig) config;
		final Scrip scrip = scripService.findByCode(config.getScripCode());
		final OptionChain optionChain = optionChainService.findByScrip(scrip).orElseThrow();
		return new ShortStraddleTradingStrategy(optionChain, shortStraddleConfig);
	}
	
}
