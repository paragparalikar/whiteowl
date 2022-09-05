package com.whiteowl.core.position;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
	
	Set<Position> findByStatusNot(PositionStatus status);
	
	Set<Position> findByTradingStrategyIdAndStatusNot(String tradingStrategyId, PositionStatus status);

	Set<Position> findByStatusIn(Collection<PositionStatus> statuses);
	
}
