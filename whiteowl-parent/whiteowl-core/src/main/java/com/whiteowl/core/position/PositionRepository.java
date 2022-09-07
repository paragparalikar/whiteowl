package com.whiteowl.core.position;

import java.util.Collection;
import java.util.Set;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
@EnableScanCount
public interface PositionRepository extends PagingAndSortingRepository<Position, Long> {
	
	Set<Position> findByStatusNot(PositionStatus status);
	
	Set<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyId, PositionStatus status);

	Set<Position> findByStatusIn(Collection<PositionStatus> statuses);
	
}
