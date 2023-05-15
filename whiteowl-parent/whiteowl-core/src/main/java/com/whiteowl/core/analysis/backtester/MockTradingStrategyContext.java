package com.whiteowl.core.analysis.backtester;

import java.util.Arrays;
import java.util.function.Consumer;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;
import com.whiteowl.core.strategy.context.DefaultTradingStrategyContext;
import com.whiteowl.core.strategy.context.TradingStrategyContext;

import lombok.Getter;
import lombok.experimental.Delegate;

@Getter
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

	public MockTradingStrategyContext(Scrip scrip, TradingStrategyConfig config, Consumer<Position> callback,
			double initialMargin, double slippagePercentage) {
		this.mockBarService = new MockBarService();
		this.mockScripService = new MockScripService();
		this.mockQuoteService = new MockQuoteService();
		this.mockPortfolioService = new MockPortfolioService();
		this.mockPositionService = new MockPositionService(callback);
		this.mockTradingStrategyConfigService = new MockTradingStrategyConfigService();
		this.mockBrokerServiceProvider = new MockBrokerServiceProvider(initialMargin, slippagePercentage);
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
		
		mockPortfolioService.save(createPortfolio(initialMargin));
		mockScripService.saveAll(Arrays.asList(scrip));
		mockTradingStrategyConfigService.save(config);
		barSeriesCacheManager.init();
	}
	
	private Portfolio createPortfolio(double initialMargin) {
		final Portfolio portfolio = new Portfolio();
		portfolio.setBroker(Broker.TEST);
		portfolio.setAvailableMargin(initialMargin);
		portfolio.setMaxTradableAmount(initialMargin);
		return portfolio;
	}

}
