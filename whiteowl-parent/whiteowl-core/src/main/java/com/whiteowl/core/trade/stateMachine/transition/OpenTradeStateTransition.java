package com.whiteowl.core.trade.stateMachine.transition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

@Component
public class OpenTradeStateTransition extends AbstractTradeStateTransition {

	@Autowired
	public OpenTradeStateTransition(
			PortfolioService portfolioService,
			BrokerServiceProviderFactory brokerServiceProviderFactory) {
		super(TradeStatus.NEW, portfolioService, brokerServiceProviderFactory);
	}

	@Override
	protected void doTransition(Trade trade, Portfolio portfolio, 
			BrokerServiceProvider brokerServiceProvider) {
		brokerServiceProvider.create(trade, portfolio);
	}

}
