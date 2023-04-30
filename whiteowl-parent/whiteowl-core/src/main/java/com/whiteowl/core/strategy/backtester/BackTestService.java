package com.whiteowl.core.strategy.backtester;

import java.util.Arrays;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;
import com.whiteowl.core.strategy.context.DefaultTradingStrategyContext;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class BackTestService {

	private final BarService barService;
	
	public double backtest(TradingStrategyConfig config, double initialMargine, double slippagePercentage) {
		final MockBarService mockBarService = new MockBarService();
		final MockScripService mockScripService = new MockScripService();
		final MockQuoteService mockQuoteService = new MockQuoteService();
		final MockPositionService mockPositionService = new MockPositionService();
		final MockPortfolioService mockPortfolioService = new MockPortfolioService();
		final MockTradingStrategyConfigService mockTradingStrategyConfigService = new MockTradingStrategyConfigService();
		final BarSeriesCacheManager barSeriesCacheManager = new BarSeriesCacheManager(mockBarService, mockTradingStrategyConfigService);
		final MockBrokerServiceProvider mockBrokerServiceProvider = new MockBrokerServiceProvider(initialMargine, slippagePercentage);
		final BrokerServiceProviderFactory brokerServiceProviderFactory = new BrokerServiceProviderFactory(Arrays.asList(mockBrokerServiceProvider));
		final TradingStrategyContext tradingStrategyContext = DefaultTradingStrategyContext.builder()
				.scripService(mockScripService)
				.quoteService(mockQuoteService)
				.positionService(mockPositionService)
				.portfolioService(mockPortfolioService)
				.barSeriesCacheManager(barSeriesCacheManager)
				.brokerServiceProviderFactory(brokerServiceProviderFactory)
				.build();
		
		
		return 0;
	}

}
