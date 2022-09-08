package com.whiteowl.core.trade;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

	private final TradeRepository tradeRepository;
	
	public Trade save(Trade trade) {
		return tradeRepository.saveAndFlush(trade);
	}
	
	public Optional<Trade> findByBrokerTradeId(String brokerTradeId){
		return tradeRepository.findByBrokerTradeId(brokerTradeId);
	}
	
}
