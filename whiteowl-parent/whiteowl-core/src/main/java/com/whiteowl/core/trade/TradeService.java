package com.whiteowl.core.trade;

import java.util.Optional;

public interface TradeService {

	Optional<Trade> findByBrokerTradeId(String brokerTradeId);

}