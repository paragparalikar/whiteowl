package com.whiteowl.analysis.backtester.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.whiteowl.analysis.backtester.listener.BacktestListener;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockPositionService implements PositionService {

	private final BacktestListener listener;
	private final AtomicLong idGenerator = new AtomicLong();
	private final Set<Position> activePositions = new HashSet<>();
	private final Set<Position> terminalPositions = new HashSet<>();

	@Override
	public Position save(Position position) {
		if(null == position.getId() || 0 == position.getId()) {
			position.setId(idGenerator.incrementAndGet());
			listener.onEntry(position);
		}
		if(position.getStatus().isTerminal()) {
			activePositions.remove(position);
			terminalPositions.add(position);
			listener.onExit(position);
		} else {
			activePositions.add(position);
		}
		return position;
	}
	
	public void flush() {
		activePositions.forEach(listener::onExit);
	}

	@Override
	public List<Position> findAll() {
		final List<Position> positions = new ArrayList<>(activePositions.size() + terminalPositions.size());
		positions.addAll(activePositions);
		positions.addAll(terminalPositions);
		return positions;
	}
	
	public void updateAll() {
		final Iterator<Position> iterator = activePositions.iterator();
		while(iterator.hasNext()) {
			final Position position = iterator.next();
			PositionStatus.update(position);
			if(position.getStatus().isTerminal()) {
				iterator.remove();
				terminalPositions.add(position);
				listener.onExit(position);
			}
		}
	}
	
	@Override
	public Optional<Position> findByEntryTradesBrokerTradeId(String brokerTradeId) {
		return activePositions.stream()
				.filter(position -> position.getEntryTrades().stream()
						.map(Trade::getBrokerTradeId)
						.anyMatch(id -> Objects.equals(id, brokerTradeId)))
				.findFirst();
	}

	@Override
	public Optional<Position> findByExitTradesBrokerTradeId(String brokerTradeId) {
		return activePositions.stream()
				.filter(position -> position.getExitTrades().stream()
						.map(Trade::getBrokerTradeId)
						.anyMatch(id -> Objects.equals(id, brokerTradeId)))
				.findFirst();
	}

	@Override
	public boolean existsByScripAndStatusNot(Scrip scrip, PositionStatus status) {
		return activePositions.stream()
				.anyMatch(position -> Objects.equals(scrip, position.getScrip()) 
						&& !Objects.equals(status, position.getStatus()));
	}

	@Override
	public List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status) {
		return activePositions.stream()
				.filter(position -> Objects.equals(portfolio, position.getPortfolio()))
				.filter(position -> !Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());				
	}

	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId,
			PositionStatus status) {
		return activePositions.stream()
				.filter(position -> !Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());		
	}

	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId,
			PositionStatus status) {
		return activePositions.stream()
				.filter(position -> Objects.equals(scrip, position.getScrip()))
				.filter(position -> Objects.equals(status, position.getStatus()))
				.collect(Collectors.toList());		
	}

}
