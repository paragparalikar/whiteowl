package com.whiteowl.core.position.stateMachine;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;

import org.springframework.beans.factory.annotation.Autowired;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.NonNull;

public class PositionEntityListener {
	
	@Autowired private PositionService positionService;
	@Autowired private PortfolioService portfolioService;
	@Autowired private PositionStateMachine positionStateMachine;

	@PrePersist
	public void prePersist(@NonNull final Position position) {
		if(null == position.getPortfolio()) {
			final List<Portfolio> portfolios = portfolioService.findAll();
			if(portfolios.isEmpty()) throw new IllegalStateException();
			position.setPortfolio(portfolios.get(0));
			portfolios.stream().skip(1)
				.map(position::withPortfolio)
				.forEach(positionService::save);
		}
	}
	
	@PostUpdate
	@PostPersist
	public void postPersist(@NonNull final Position position) {
		if(Stream.concat(
				position.getExitTrades().stream(), 
				position.getEntryTrades().stream())
			.map(Trade::getStatus)
			.anyMatch(TradeStatus::isActionable)) {
			positionStateMachine.handle(position);
		}
	}
	
}
