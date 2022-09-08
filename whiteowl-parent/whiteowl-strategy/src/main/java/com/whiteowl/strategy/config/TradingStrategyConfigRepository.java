package com.whiteowl.strategy.config;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingStrategyConfigRepository extends JpaRepository<PersistentTradingStrategyConfig, Long> {

	public List<PersistentTradingStrategyConfig> findByEnabled(boolean value);
	
}
