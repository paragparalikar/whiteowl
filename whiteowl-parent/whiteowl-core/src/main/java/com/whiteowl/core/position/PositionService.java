package com.whiteowl.core.position;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final PositionRepository positionRepository;
	
	public Position save(Position position) {
		return positionRepository.save(position);
	}
	
	public Set<Position> findByStatusNot(PositionStatus status) {
		return positionRepository.findByStatusNot(status);
	}
	
	public Set<Position> findByTradingStrategyConfigIdAndStatusNot(String tradingStrategyId, PositionStatus status){
		return positionRepository.findByTradingStrategyConfigIdAndStatusNot(tradingStrategyId, status);
	}

	public Set<Position> findByStatusIn(Collection<PositionStatus> statuses) {
		return positionRepository.findByStatusIn(statuses);
	}
	
}
