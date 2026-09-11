package com.whiteowl.core.backtest.rotational;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Allocates capital across composite sub-strategies for each trading day.
 *
 * <p>Implementations receive the day's breakout counts and sub-strategy
 * configurations, and return a fraction of total capital for each
 * sub-strategy. The composite engine uses these fractions to size
 * positions — each sub-strategy's allocated capital is divided equally
 * among its actual picks that day.</p>
 *
 * @see OrBreadthAllocator
 * @see FixedProportionalAllocator
 */
public interface CapitalAllocator {

    /**
     * Compute the fraction of total capital allocated to each sub-strategy.
     *
     * @param context day-level allocation context (breakout counts, capital, sub-strategies)
     * @return map from sub-strategy name → fraction of total capital (should sum to ≤ 1.0)
     */
    Map<String, Double> allocate(AllocationContext context);

    /**
     * Context provided to the allocator for each trading day.
     *
     * @param date                  the trading date
     * @param totalCapital          current portfolio equity
     * @param subStrategies         the composite's sub-strategies
     */
    record AllocationContext(
            LocalDate date,
            double totalCapital,
            List<CompositeOrbStrategy.SubStrategy> subStrategies
    ) {}
}
