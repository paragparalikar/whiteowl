package com.whiteowl.core.trade.stateMachine.transition;

import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OpenTradeStateTransition implements TradeStateTransition {
	
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;

	@Override
	public TradeStatus getInitialStatus() {
		return TradeStatus.NEW;
	}

	@Override
	public void transition(@NonNull final Trade trade, @NonNull final Position position) {
		if(!getInitialStatus().equals(trade.getStatus())) throw new IllegalStateException();
		final Portfolio portfolio = position.getPortfolio();
		final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
				.getBrokerServiceProvider(portfolio.getBroker());
		brokerServiceProvider.create(trade, portfolio);
		trade.setStatus(TradeStatus.PENDING);
	}

}
