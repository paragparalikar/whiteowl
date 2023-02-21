package com.whiteowl.core.strategy.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingStrategyPerformanceRepository extends JpaRepository<TradingStrategyPerformance, String> {

}
