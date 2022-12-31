package com.whiteowl.core.trade.stateMachine.transition;

import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

@Component
public class UpdateTradeStateTransition extends AbstractTradeStateTransition {

	public UpdateTradeStateTransition(
			PortfolioService portfolioService,
			BrokerServiceProviderFactory brokerServiceProviderFactory) {
		super(TradeStatus.UPDATABLE, portfolioService, brokerServiceProviderFactory);
	}

	@Override
	protected void doTransition(Trade trade, Portfolio portfolio, 
			BrokerServiceProvider brokerServiceProvider) {
		brokerServiceProvider.update(trade, portfolio);
	}

}
