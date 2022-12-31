package com.whiteowl.strategy.test.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.scheduling.TaskScheduler;
import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.position.stateMachine.PositionStateMachine;
import com.whiteowl.core.position.stateMachine.transition.ClosePositionStateTransition;
import com.whiteowl.core.position.stateMachine.transition.OpenPositionStateTransition;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.trade.stateMachine.TradeStateMachine;
import com.whiteowl.core.trade.stateMachine.transition.CancelTradeStateTransition;
import com.whiteowl.core.trade.stateMachine.transition.OpenTradeStateTransition;
import com.whiteowl.core.trade.stateMachine.transition.UpdateTradeStateTransition;
import com.whiteowl.strategy.TradingStrategy;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyFactory;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.config.TradingStrategyConfigService;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
public class MockContext {
	
	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final TradingStrategyConfig config;

	private final MockBarService barService;
	private final MockQuoteService quoteService;
	private final ScripService scripService;
	private final TaskScheduler taskScheduler;
	private final PositionService positionService;
	private final TradingStrategy tradingStrategy;
	private final PortfolioService portfolioService;
	private final TradeStateMachine tradeStateMachine;
	private final OptionChainService optionChainService;
	private final PositionStateMachine positionStateMachine;
	private final MockBrokerServiceProvider brokerServiceProvider;
	private final TradingStrategyFactory tradingStrategyFactory;
	private final TradingStrategyExecutor tradingStrategyExecutor;
	private final TradingStrategyConfigService tradingStrategyConfigService;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	
	@Builder
	public MockContext(
			@NonNull final Scrip scrip, 
			@NonNull final List<Bar> bars, 
			@NonNull final Timeframe timeframe,
			@NonNull final TradingStrategyConfig config) {
		this.bars = bars;
		this.scrip = scrip;
		this.config = config;
		this.timeframe = timeframe;
		
		final Portfolio portfolio = new Portfolio();
		portfolio.setMaxTradableAmount(100000);
		this.portfolioService = new MockPortfolioService(portfolio);
		this.quoteService = new MockQuoteService();
		this.taskScheduler = new MockTaskScheduler();
		this.scripService = new MockScripService(scrip);
		this.optionChainService = new MockOptionChainService();
		this.positionService = new MockPositionService(this::onPositionSaved);
		this.barService = new MockBarService(config.getMinBarCount() - 1, scrip.getCode(), bars, timeframe);
		this.brokerServiceProvider = new MockBrokerServiceProvider(timeframe, barService);
		this.tradingStrategyFactory = new TradingStrategyFactory(barService, scripService, optionChainService);
		this.tradingStrategy = tradingStrategyFactory.getTradingStrategy(config);
		this.brokerServiceProviderFactory = new BrokerServiceProviderFactory(Collections.singletonList(brokerServiceProvider));
		this.tradeStateMachine = new TradeStateMachine(Arrays.asList(
				new OpenTradeStateTransition(portfolioService, brokerServiceProviderFactory), 
				new UpdateTradeStateTransition(portfolioService, brokerServiceProviderFactory),
				new CancelTradeStateTransition(portfolioService, brokerServiceProviderFactory)));
		this.positionStateMachine = new PositionStateMachine(positionService, Arrays.asList(
				new OpenPositionStateTransition(tradeStateMachine),
				new ClosePositionStateTransition(tradeStateMachine)));
		this.tradingStrategyConfigService = new MockTradingStrategyConfigService(config);
		final TradingStrategyFactory mockTradingStrategyFactory = new MockTradingStrategyFactory(config, tradingStrategy);
		this.tradingStrategyExecutor = new TradingStrategyExecutor(
				quoteService, taskScheduler, positionService, mockTradingStrategyFactory, tradingStrategyConfigService);
		tradingStrategyExecutor.onApplicationReady();
	}
	
	private void onPositionSaved(Position position) {
		positionStateMachine.handle(position);
	}
	
	public boolean next() {
		if(barService.next()) {
			final Bar bar = barService.findLatestBar(scrip.getCode(), timeframe).orElseThrow();
			final TradeType tradeType = TradeType.BUY; // TODO ???
			quoteService.publish(bar, scrip, tradeType);
			tradingStrategyExecutor.onScripBarDownloaded(scrip, timeframe);
			portfolioService.findAll().stream()
				.flatMap(portfolio -> Stream.concat(
						positionService.findByPortfolioAndStatusNot(portfolio, PositionStatus.CLOSED).stream(), 
						positionService.findByPortfolioAndStatusNot(portfolio, PositionStatus.CLOSED).stream()))
				.forEach(tradingStrategyExecutor::onPositionSynchronized);
			brokerServiceProvider.execute(timeframe);
		};
		return false;
	}

}