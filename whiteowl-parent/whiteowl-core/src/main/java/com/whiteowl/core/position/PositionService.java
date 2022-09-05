package com.whiteowl.core.position;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final PositionRepository positionRepository;
	
	public Position saveAndFlush(Position position) {
		return positionRepository.saveAndFlush(position);
	}
	
	public Set<Position> findByStatusNot(PositionStatus status) {
		return positionRepository.findByStatusNot(status);
	}
	
	public Set<Position> findByTradingStrategyIdAndStatusNot(String tradingStrategyId, PositionStatus status){
		return positionRepository.findByTradingStrategyIdAndStatusNot(tradingStrategyId, status);
	}

	public Set<Position> findByStatusIn(Collection<PositionStatus> statuses) {
		return positionRepository.findByStatusIn(statuses);
	}
	
}
