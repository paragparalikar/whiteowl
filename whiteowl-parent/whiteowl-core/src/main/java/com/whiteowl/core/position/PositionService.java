package com.whiteowl.core.position;

import java.util.List;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;

public interface PositionService {

	Position save(Position position);
	
	List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status);
	
	List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId, PositionStatus status);
	
	List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId, PositionStatus status);
	
}
