package com.whiteowl.core.strategy.backtester;

import java.util.Arrays;

import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;
import com.whiteowl.core.strategy.context.DefaultTradingStrategyContext;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.experimental.Delegate;

public class MockTradingStrategyContext implements TradingStrategyContext {
	
	private final MockBarService mockBarService;
	private final MockScripService mockScripService;
	private final MockQuoteService mockQuoteService;
	private final MockPositionService mockPositionService;
	private final MockPortfolioService mockPortfolioService;
	private final BarSeriesCacheManager barSeriesCacheManager;
	private final MockBrokerServiceProvider mockBrokerServiceProvider;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	private final MockTradingStrategyConfigService mockTradingStrategyConfigService;
	@Delegate private final TradingStrategyContext delegate;

	public MockTradingStrategyContext(double initialMargine, double slippagePercentage) {
		this.mockBarService = new MockBarService();
		this.mockScripService = new MockScripService();
		this.mockQuoteService = new MockQuoteService();
		this.mockPositionService = new MockPositionService();
		this.mockPortfolioService = new MockPortfolioService();
		this.mockTradingStrategyConfigService = new MockTradingStrategyConfigService();
		this.mockBrokerServiceProvider = new MockBrokerServiceProvider(initialMargine, slippagePercentage);
		this.barSeriesCacheManager = new BarSeriesCacheManager(mockBarService, mockTradingStrategyConfigService);
		this.brokerServiceProviderFactory = new BrokerServiceProviderFactory(Arrays.asList(mockBrokerServiceProvider));
		this.delegate = DefaultTradingStrategyContext.builder()
				.scripService(mockScripService)
				.quoteService(mockQuoteService)
				.positionService(mockPositionService)
				.portfolioService(mockPortfolioService)
				.barSeriesCacheManager(barSeriesCacheManager)
				.brokerServiceProviderFactory(brokerServiceProviderFactory)
				.build();
	}

}
