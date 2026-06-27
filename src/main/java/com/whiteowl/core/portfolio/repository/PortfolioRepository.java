package com.whiteowl.core.portfolio.repository;

import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {

    void save(Portfolio portfolio);

    Optional<Portfolio> findById(String id);

    List<Portfolio> findAll();

    List<Holding> findHoldings(String portfolioId);

}
