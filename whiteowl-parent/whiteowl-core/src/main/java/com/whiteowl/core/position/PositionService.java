package com.whiteowl.core.position;

import java.util.List;

import org.springframework.stereotype.Service;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.TradeService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

	private final TradeService tradeService;
	private final PositionRepository positionRepository;
	
	public Position save(@NonNull Position position) {
		position = positionRepository.saveAndFlush(position);
		final Portfolio portfolio = position.getPortfolio();
		position.getEntryTrades().forEach(trade -> tradeService.execute(trade, portfolio));
		position.getExitTrades().forEach(trade -> tradeService.execute(trade, portfolio));
		return position;
	}
	
	public List<Position> findByScripAndTradingStrategyConfigIdAndStatus(Scrip scrip, Long configId, PositionStatus status){
		return positionRepository.findByScripAndTradingStrategyConfigIdAndStatus(scrip, configId, status);
	}
}
