package com.whiteowl.core.backtest.rotational;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proportional capital allocator based on configured picks.
 *
 * <p>Distributes capital across sub-strategies in proportion to each
 * sub-strategy's configured pick count. A sub-strategy with more picks
 * receives a larger share of capital.</p>
 *
 * @see CapitalAllocator
 */
public final class OrBreadthAllocator implements CapitalAllocator {

    public OrBreadthAllocator() {}

    @Override
    public Map<String, Double> allocate(AllocationContext ctx) {
        // Count total configured picks across all sub-strategies
        int totalPicks = 0;
        for (var sub : ctx.subStrategies()) {
            totalPicks += sub.config().getPicks();
        }

        // Distribute fractions proportional to each sub-strategy's configured picks
        Map<String, Double> result = new LinkedHashMap<>();
        for (var sub : ctx.subStrategies()) {
            double frac = (totalPicks > 0)
                    ? (double) sub.config().getPicks() / totalPicks
                    : 1.0 / ctx.subStrategies().size();
            result.put(sub.name(), frac);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Proportional (by picks)";
    }
}
