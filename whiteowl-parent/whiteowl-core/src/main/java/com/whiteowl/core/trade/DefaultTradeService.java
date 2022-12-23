package com.whiteowl.core.trade;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.broker.BrokerServiceProviderFactory;
import com.whiteowl.core.portfolio.Portfolio;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultTradeService implements TradeService {

	private final TradeRepository tradeRepository;
	private final BrokerServiceProviderFactory brokerServiceProviderFactory;
	
	@Override
	public Optional<Trade> findByBrokerTradeId(String brokerTradeId){
		return tradeRepository.findByBrokerTradeId(brokerTradeId);
	}
	
	@Override
	public Trade create(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
				.getBrokerServiceProvider(portfolio.getBroker());
		brokerServiceProvider.create(trade, portfolio);
		return trade;
	}

	@Override
	public Trade update(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
				.getBrokerServiceProvider(portfolio.getBroker());
		brokerServiceProvider.update(trade, portfolio);
		return trade;
	}

	@Override
	public Trade cancel(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		final BrokerServiceProvider brokerServiceProvider = brokerServiceProviderFactory
				.getBrokerServiceProvider(portfolio.getBroker());
		brokerServiceProvider.cancel(trade, portfolio);
		return trade;
	}
	
}
