package com.whiteowl.core.trade;

import java.util.Optional;

import com.whiteowl.core.portfolio.Portfolio;

public interface TradeService {

	Optional<Trade> findByBrokerTradeId(String brokerTradeId);

	Trade create(Trade trade, Portfolio portfolio);

	Trade update(Trade trade, Portfolio portfolio);

	Trade cancel(Trade trade, Portfolio portfolio);

}