package com.whiteowl.strategy.test.mock;

import java.util.List;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;

public class MockPositionService implements PositionService {

	@Override
	public Position save(Position position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId,
			PositionStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId,
			PositionStatus status) {
		throw new UnsupportedOperationException();
	}

}
