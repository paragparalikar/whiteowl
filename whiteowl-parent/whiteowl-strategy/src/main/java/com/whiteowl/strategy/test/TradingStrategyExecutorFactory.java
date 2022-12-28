package com.whiteowl.strategy.test;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.donchian.DonchianBreakoutTradingStrategyExecutor;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class TradingStrategyExecutorFactory {

	private final BarService barService;
	private final QuoteService quoteService;
	private final PositionService positionService;
	
	public <T extends TradingStrategyConfig> TradingStrategyExecutor<T> getTradingStrategyExecutor(
			@NonNull final TradingStrategyTemplate tradingStrategyTemplate){
		switch(tradingStrategyTemplate) {
		case DONCHIAN: return buildDonchianBreakoutExecutor();
		default: throw new IllegalArgumentException(String.format(
				"TradingStrategyTemplate %s is not supported", tradingStrategyTemplate.name()));
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends TradingStrategyConfig> TradingStrategyExecutor<T> buildDonchianBreakoutExecutor() {
		return (TradingStrategyExecutor<T>) DonchianBreakoutTradingStrategyExecutor.builder()
				.barService(barService)
				.positionService(positionService)
				.quoteService(quoteService)
				.build();
	}

}
