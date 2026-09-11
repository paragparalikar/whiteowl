package com.whiteowl.core.trade.repository;

import com.whiteowl.core.trade.model.Trade;
import com.whiteowl.core.trade.model.TradeStatus;

import java.util.List;
import java.util.Optional;

public interface TradeRepository {

    void save(Trade trade);

    Optional<Trade> findById(String id);

    List<Trade> findByPortfolioId(String portfolioId);

    List<Trade> findByScripId(String scripId);

    List<Trade> findByStrategyId(String strategyId);

    List<Trade> findByStatus(TradeStatus status);

}
