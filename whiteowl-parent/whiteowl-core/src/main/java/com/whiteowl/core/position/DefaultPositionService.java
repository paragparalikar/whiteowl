package com.whiteowl.core.position;

import java.util.List;

import org.springframework.stereotype.Service;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultPositionService implements PositionService {
		
	private final PositionRepository positionRepository;
	
	@Override
	public Position save(@NonNull final Position position) {
		return positionRepository.saveAndFlush(position);
	}
	
	@Override
	public boolean existsByScripAndStatusNot(
			@NonNull final Scrip scrip, 
			@NonNull final PositionStatus status) {
		return positionRepository.existsByScripAndStatusNot(scrip, status);
	}
	
	@Override
	public List<Position> findByTradingStrategyConfigIdAndStatusNot(
			@NonNull final String tradingStrategyConfigId,
			@NonNull final PositionStatus status) {
		return positionRepository.findByTradingStrategyConfigIdAndStatusNot(tradingStrategyConfigId, status);
	}
	
	@Override
	public List<Position> findByPortfolioAndStatusNot(
			@NonNull final Portfolio portfolio, 
			@NonNull final PositionStatus status) {
		return positionRepository.findByPortfolioAndStatusNot(portfolio, status);
	}
	
	@Override
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(
			@NonNull final Scrip scrip, 
			@NonNull final String configId, 
			@NonNull final PositionStatus status){
		return positionRepository.findByScripAndTradingStrategyConfigIdAndStatus(scrip, configId, status);
	}
	
}
