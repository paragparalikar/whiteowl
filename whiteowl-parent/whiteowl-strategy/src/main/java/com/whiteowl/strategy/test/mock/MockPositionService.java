package com.whiteowl.strategy.test.mock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public class MockPositionService implements PositionService {
	
	private final AtomicLong idGenerator = new AtomicLong();
	private final Map<Long, Position> cache = new ConcurrentHashMap<>();

	@Override
	public Position save(@NonNull final Position position) {
		if(null == position.getId()) position.setId(idGenerator.incrementAndGet());
		cache.put(position.getId(), position);
		return position;
	}

	@Override
	public List<Position> findByPortfolioAndStatusNot(
			@NonNull final Portfolio portfolio, 
			@NonNull final PositionStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(
			@NonNull final String tradingStrategyConfigId,
			@NonNull final PositionStatus status) {
		return cache.values().stream()
				.filter(position -> !status.equals(position.getStatus()))
				.filter(position -> tradingStrategyConfigId.equals(position.getTradingStrategyConfigId()))
				.collect(Collectors.toList());
	}

	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(
			@NonNull final Scrip scrip, 
			@NonNull final String configId,
			@NonNull final PositionStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByScripAndStatusNot(Scrip scrip, PositionStatus status) {
		throw new UnsupportedOperationException();
	}

}
