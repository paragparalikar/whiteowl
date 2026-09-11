package com.whiteowl.core.backtest.rotational;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed proportional capital allocator.
 *
 * <p>Allocates capital to each sub-strategy proportional to its
 * configured pick count. The allocation is static — it doesn't
 * change based on daily breakout counts.</p>
 *
 * <p>Example: with sub-strategies of 6, 2, 9 picks (total 17),
 * fractions are 6/17 ≈ 35%, 2/17 ≈ 12%, 9/17 ≈ 53%.</p>
 *
 * @see CapitalAllocator
 */
public final class FixedProportionalAllocator implements CapitalAllocator {

    @Override
    public Map<String, Double> allocate(AllocationContext ctx) {
        int totalPicks = 0;
        for (var sub : ctx.subStrategies()) {
            totalPicks += sub.config().getPicks();
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (var sub : ctx.subStrategies()) {
            int subPicks = sub.config().getPicks();
            result.put(sub.name(), totalPicks > 0 ? (double) subPicks / totalPicks : 0);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Fixed Proportional";
    }
}
