package com.whiteowl.core.trade;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

	private final TradeRepository tradeRepository;
	private final BrokerServiceProvider brokerServiceProvider;
	
	public Optional<Trade> findByBrokerTradeId(String brokerTradeId){
		return tradeRepository.findByBrokerTradeId(brokerTradeId);
	}
	
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
