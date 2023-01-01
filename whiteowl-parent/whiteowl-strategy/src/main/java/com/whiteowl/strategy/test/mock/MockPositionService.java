package com.whiteowl.strategy.test.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockPositionService implements PositionService {
	
	@NonNull private final Consumer<Position> saveCallback;
	private final AtomicLong idGenerator = new AtomicLong();
	private final Map<Long, Position> terminalPositions = new ConcurrentHashMap<>();
	private final Map<Long, Position> nonTerminalPositions = new ConcurrentHashMap<>();

	@Override
	public Position save(@NonNull final Position position) {
		if(null == position.getId()) position.setId(idGenerator.incrementAndGet());
		position.updateStatus();
		if(position.getStatus().isTerminal()) {
			nonTerminalPositions.remove(position.getId());
			terminalPositions.put(position.getId(), position);
		} else {
			terminalPositions.remove(position.getId());
			nonTerminalPositions.put(position.getId(), position);
		}
		saveCallback.accept(position);
		return position;
	}

	@Override
	public List<Position> findByPortfolioAndStatusNot(
			@NonNull final Portfolio portfolioIgnored, 
			@NonNull final PositionStatus status) {
		final Map<Long, Position> cache = status.isTerminal() ? nonTerminalPositions : terminalPositions;
		return new ArrayList<>(cache.values());
	}

	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(
			@NonNull final String ignored,
			@NonNull final PositionStatus status) {
		final Map<Long, Position> cache = status.isTerminal() ? nonTerminalPositions : terminalPositions;
		return new ArrayList<>(cache.values());
	}

	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(
			@NonNull final Scrip scripIgnored, 
			@NonNull final String configIdIgnored,
			@NonNull final PositionStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByScripAndStatusNot(
			@NonNull final Scrip scripIgnored, 
			@NonNull final PositionStatus status) {
		return status.isTerminal() ? !nonTerminalPositions.isEmpty() : !terminalPositions.isEmpty();
	}

}
