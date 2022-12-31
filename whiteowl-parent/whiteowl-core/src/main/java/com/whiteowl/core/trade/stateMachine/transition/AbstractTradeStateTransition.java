package com.whiteowl.core.trade.stateMachine.transition;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTradeStateTransition implements TradeStateTransition{

	@Getter private final TradeStatus initialStatus;
	private final PortfolioService portfolioService;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	
	@Override
	public void transition(@NonNull final Trade trade, @NonNull final Position position) {
		if(!getInitialStatus().equals(trade.getStatus())) throw new IllegalStateException();
		final Portfolio portfolio = position.getPortfolio();
		final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
				.getBrokerServiceProvider(portfolio.getBroker());
		try {
			trade.setStatus(TradeStatus.PENDING);
			doTransition(trade, portfolio, brokerServiceProvider);
			final double availableMargin = brokerServiceProvider.getAvailableMargin(portfolio);
			portfolio.setAvailableMargin(availableMargin);
			portfolioService.save(portfolio);
		} catch(Exception e) {
			trade.setStatus(getInitialStatus());
			log.error("", e);
			throw e;
		}
	}
	
	abstract protected void doTransition(Trade trade, Portfolio portfolio, 
			BrokerServiceProvider brokerServiceProvider);

}
