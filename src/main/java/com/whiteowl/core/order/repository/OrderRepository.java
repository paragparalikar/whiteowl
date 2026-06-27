package com.whiteowl.core.order.repository;

import com.whiteowl.core.order.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByPortfolioId(String portfolioId);

    List<Order> findByScripId(String scripId);

    void deleteById(String id);

}
