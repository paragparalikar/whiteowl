package com.whiteowl.core.position;

import java.util.List;

import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final PositionRepository positionRepository;
	
	public Position save(Position position) {
		return positionRepository.saveAndFlush(position);
	}
	
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, Long configId, PositionStatus status){
		return positionRepository.findByScripAndTradingStrategyConfigIdAndStatus(scrip, configId, status);
	}
}
