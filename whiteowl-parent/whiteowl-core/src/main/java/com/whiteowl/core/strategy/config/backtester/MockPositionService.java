package com.whiteowl.core.strategy.config.backtester;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;

public class MockPositionService implements PositionService {
	
	private final Map<String, Set<Position>> cache = new ConcurrentHashMap<>();

	@Override
	public Position save(Position position) {
		cache.computeIfAbsent(position.getTradingStrategyConfigId(), 
				key -> Collections.newSetFromMap(new IdentityHashMap<>()))
		.add(position);
		return position;
	}

	@Override
	public List<Position> findAll() {
		return cache.values().stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	@Override
	public Optional<Position> findByEntryTradesBrokerTradeId(String brokerTradeId) {
		return cache.values().stream()
				.flatMap(Collection::stream)
				.filter(position -> position.getEntryTrades().stream()
						.map(Trade::getBrokerTradeId)
						.anyMatch(id -> Objects.equals(id, brokerTradeId)))
				.findFirst();
	}

	@Override
	public Optional<Position> findByExitTradesBrokerTradeId(String brokerTradeId) {
		return cache.values().stream()
				.flatMap(Collection::stream)
				.filter(position -> position.getExitTrades().stream()
						.map(Trade::getBrokerTradeId)
						.anyMatch(id -> Objects.equals(id, brokerTradeId)))
				.findFirst();
	}

	@Override
	public boolean existsByScripAndStatusNot(Scrip scrip, PositionStatus status) {
		return cache.values().stream()
				.flatMap(Collection::stream)
				.anyMatch(position -> Objects.equals(scrip, position.getScrip()) 
						&& !Objects.equals(status, position.getStatus()));
	}

	@Override
	public List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status) {
		return cache.values().stream()
				.flatMap(Collection::stream)
				.filter(position -> Objects.equals(portfolio, position.getPortfolio()))
				.filter(position -> !Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());				
	}

	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId,
			PositionStatus status) {
		return cache.getOrDefault(tradingStrategyConfigId, Collections.emptySet()).stream()
				.filter(position -> !Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());		
	}

	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId,
			PositionStatus status) {
		return cache.getOrDefault(configId, Collections.emptySet()).stream()
				.filter(position -> Objects.equals(scrip, position.getScrip()))
				.filter(position -> Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());		
	}

}
