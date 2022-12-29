package com.whiteowl.core.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
	
	boolean existsByScripAndStatusNot(Scrip scrip, PositionStatus status);
	
	List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status);
	
	List<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyConfigId, PositionStatus status);
	
	List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId, PositionStatus status);
	
}
