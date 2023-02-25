package com.whiteowl.core.position;

import java.util.List;
import java.util.Optional;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;

public interface PositionService {

	Position save(Position position);
	
	List<Position> findAll();
	
	Optional<Position> findByEntryTradesBrokerTradeId(String brokerTradeId);
	
	Optional<Position> findByExitTradesBrokerTradeId(String brokerTradeId);
	
	boolean existsByScripAndStatusNot(Scrip scrip, PositionStatus status);
	
	List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status);
	
	List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId, PositionStatus status);
	
	List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId, PositionStatus status);
	
}
