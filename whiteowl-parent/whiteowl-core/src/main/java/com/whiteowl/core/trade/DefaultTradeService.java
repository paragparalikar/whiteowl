package com.whiteowl.core.trade;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultTradeService implements TradeService {

	private final TradeRepository tradeRepository;
	
	@Override
	public Optional<Trade> findByBrokerTradeId(String brokerTradeId){
		return tradeRepository.findByBrokerTradeId(brokerTradeId);
	}
	
}
