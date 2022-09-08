package com.whiteowl.core.position;

import java.util.List;

import org.springframework.stereotype.Service;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.TradeExecutor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final TradeExecutor tradeExecutor;
	private final PositionRepository positionRepository;
	
	public Position save(@NonNull Position position) {
		position = positionRepository.saveAndFlush(position);
		final Portfolio portfolio = position.getPortfolio();
		position.getEntryTrades().forEach(trade -> tradeExecutor.execute(trade, portfolio));
		position.getExitTrades().forEach(trade -> tradeExecutor.execute(trade, portfolio));
		return position;
	}
	
	public List<Position> findByPortfolioAndStatusNot(Portfolio portfolio, PositionStatus status) {
		return positionRepository.findByPortfolioAndStatusNot(portfolio, status);
	}
	
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, Long configId, PositionStatus status){
		return positionRepository.findByScripAndTradingStrategyConfigIdAndStatus(scrip, configId, status);
	}
}
