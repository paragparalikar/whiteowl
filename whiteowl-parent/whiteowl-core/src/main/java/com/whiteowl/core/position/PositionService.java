package com.whiteowl.core.position;

import java.util.List;

import org.springframework.stereotype.Service;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final PositionRepository positionRepository;
	
	public Position save(@NonNull Position position) {
		return positionRepository.saveAndFlush(position);
	}
	
	public List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status) {
		return positionRepository.findByPortfolioAndStatusNot(portfolio, status);
	}
	
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, String configId, PositionStatus status){
		return positionRepository.findByScripAndTradingStrategyConfigIdAndStatus(scrip, configId, status);
	}
}
