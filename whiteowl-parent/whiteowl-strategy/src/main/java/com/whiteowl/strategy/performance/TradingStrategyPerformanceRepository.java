package com.whiteowl.strategy.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingStrategyPerformanceRepository extends JpaRepository<TradingStrategyPerformance, String> {

}
