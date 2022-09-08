package com.whiteowl.core.trade;

import org.springframework.stereotype.Component;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeExecutor {

	private final BrokerServiceProvider brokerServiceProvider;
	
	public void execute(@NonNull Trade trade, @NonNull Portfolio portfolio) {
		if(TradeStatus.NEW.equals(trade.getStatus())) {
			brokerServiceProvider.create(trade, portfolio);
		} else if(TradeStatus.UPDATABLE.equals(trade.getStatus())) {
			brokerServiceProvider.update(trade, portfolio);
		} else if(TradeStatus.CANCELLABLE.equals(trade.getStatus())) {
			brokerServiceProvider.cancel(trade, portfolio);
		}
	}
	
}
