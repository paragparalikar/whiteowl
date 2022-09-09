package com.whiteowl.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.strategy.shortstrangle.ShortStraddleConfig;
import com.whiteowl.strategy.shortstrangle.ShortStraddleTradingStrategy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyExecutor implements TradingStrategyExecutor {
	
	private final ScripService scripService;	
	private final PositionService positionService;
	private final PortfolioService portfolioService;
	private final OptionChainService optionChainService;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	private final Map<TradingStrategyTemplate, BiFunction<Scrip, TradingStrategyConfig, 
		Optional<TradingStrategy>>> builders = new HashMap<>();
	
	@PostConstruct
	public void postConstruct() {
		builders.put(TradingStrategyTemplate.SHORT_STRADDLE, this::buildShortStraddle);
	}
	
	@Async
	@Override
	public void execute(@NonNull TradingStrategyConfig config) {
		final BiFunction<Scrip, TradingStrategyConfig, Optional<TradingStrategy>>
			builder = builders.get(config.getTemplate());
		if(null != builder) {
			for(Scrip scrip : scripService.findAll().stream()
					.filter(config.getScripCriteria()).toList()) {
				final TradingStrategy strategy = builder.apply(scrip, config).orElse(null);
				if(null != strategy) execute(scrip, config, strategy);
			}
		}
	}
	
	private void execute(Scrip scrip, TradingStrategyConfig config, TradingStrategy tradingStrategy) {
		resolvePositions(scrip, config).stream()
			.filter(tradingStrategy::handle)
			.flatMap(position -> PositionStatus.NEW.equals(position.getStatus()) ?
						portfolioService.findAll().stream().map(position::withPortfolio) :
						Stream.of(position))
			.map(this::execute)
			.map(positionService::save);
	}
	
	private Position execute(Position position) {
		final Portfolio portfolio = position.getPortfolio();
		position.getExitTrades().forEach(trade -> execute(trade, portfolio));
		position.getEntryTrades().forEach(trade -> execute(trade, portfolio));
		if(PositionStatus.NEW.equals(position.getStatus())) position.setStatus(PositionStatus.OPEN);
		return position;
	}
	
	public void execute(@NonNull Trade trade, @NonNull Portfolio portfolio) {
		final BrokerServiceProvider brokerServiceProvider = 
				brokerServiceProviderFactory.getBrokerServiceProvider(portfolio.getBroker());
		if(TradeStatus.NEW.equals(trade.getStatus())) {
			brokerServiceProvider.create(trade, portfolio);
			trade.setStatus(TradeStatus.PENDING);
		} else if(TradeStatus.UPDATABLE.equals(trade.getStatus())) {
			brokerServiceProvider.update(trade, portfolio);
			trade.setStatus(TradeStatus.PENDING);
		} else if(TradeStatus.CANCELLABLE.equals(trade.getStatus())) {
			brokerServiceProvider.cancel(trade, portfolio);
			trade.setStatus(TradeStatus.PENDING);
		}
	}
	
	private List<Position> resolvePositions(Scrip scrip, TradingStrategyConfig config){
		final List<Position> positions = positionService.findByScripAndTradingStrategyConfigIdAndStatus(
				scrip, config.getId(), PositionStatus.OPEN);
		if(positions.isEmpty()) positions.add(createNewPosition(scrip, config));
		return positions;
	}
	
	private Position createNewPosition(Scrip scrip, TradingStrategyConfig config) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setStatus(PositionStatus.NEW);
		position.setTradingStrategyConfigId(config.getId());
		return position;
	}
	
	private Optional<TradingStrategy> buildShortStraddle(
			@NonNull Scrip scrip, 
			@NonNull TradingStrategyConfig config) {
		return optionChainService.findByScrip(scrip)
			.map(optionChain -> new ShortStraddleTradingStrategy(
					optionChain, (ShortStraddleConfig) config));
	}
	
}
