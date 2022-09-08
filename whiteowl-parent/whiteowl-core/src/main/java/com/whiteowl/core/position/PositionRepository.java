package com.whiteowl.core.position;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.whiteowl.core.scrip.Scrip;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
	
	List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, Long configId, PositionStatus status);
	
}
