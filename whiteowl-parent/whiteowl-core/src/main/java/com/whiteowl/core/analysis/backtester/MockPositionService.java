package com.whiteowl.core.analysis.backtester;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

public class MockPositionService implements PositionService {

	private final AtomicLong idGenerator = new AtomicLong();
	private final Set<Position> activePositions = new HashSet<>();
	private final Set<Position> terminalPositions = new HashSet<>();

	@Override
	public Position save(Position position) {
		if(null == position.getId() || 0 == position.getId()) {
			position.setId(idGenerator.incrementAndGet());
		}
		activePositions.remove(position);
		final Set<Position> positions = position.getStatus().isTerminal() ? terminalPositions : activePositions;
		positions.add(position);
		return position;
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
			}
		}
	}
	
	public void closeAll(double price, LocalDateTime timestamp) {
		activePositions.forEach(position -> close(position, price, timestamp));
		terminalPositions.addAll(activePositions);
		activePositions.clear();
	}

	public void close(Position position, double price, LocalDateTime timestamp) {
		position.getExitTrades().clear();
		position.getEntryTrades().stream()
			.map(entryTrade -> complement(entryTrade, price, timestamp))
			.forEach(position.getExitTrades()::add);
		position.setLastModifiedDate(timestamp);
		position.setStatus(PositionStatus.CLOSED);
	}
	
	public Trade complement(Trade entryTrade, double price, LocalDateTime timestamp) {
		final Trade exitTrade = entryTrade.complement();
		exitTrade.setAveragePrice(price);
		exitTrade.setBrokerTradeId(entryTrade.getBrokerTradeId() + "-exit");
		exitTrade.setCreatedDate(timestamp);
		exitTrade.setExchangeTimestamp(timestamp);
		exitTrade.setFilledQuantity(entryTrade.getFilledQuantity());
		exitTrade.setLastModifiedDate(timestamp);
		exitTrade.setStatus(TradeStatus.COMPLETE);
		exitTrade.setTimestamp(timestamp);
		return exitTrade;
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
