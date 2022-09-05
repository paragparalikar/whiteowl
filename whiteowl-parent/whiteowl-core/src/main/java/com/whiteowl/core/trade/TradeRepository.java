package com.whiteowl.core.trade;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

	Optional<Trade> findByBrokerTradeId(String brokerTradeId);
	
}
