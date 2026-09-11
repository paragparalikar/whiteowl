package com.whiteowl.core.backtest.rotational;

import java.time.LocalDate;
import java.util.*;

/**
 * Composite ORB strategy that combines multiple single-side sub-strategies
 * into a unified portfolio. Each sub-strategy has its own config (stop/target/
 * filters/gap direction mode) but they share capital and a common ranker.
 *
 * <p>This implements the Composite design pattern: each sub-strategy is a
 * "leaf" with its own {@link RotationalBacktestConfig}, and this class is
 * the "composite" that orchestrates them into a single backtest.</p>
 *
 * <h3>Day-level execution flow</h3>
 * <ol>
 *   <li>Each sub-strategy runs its own {@link OrbSimulator} with its own
 *       stop/target/filter parameters.</li>
 *   <li>Each sub-strategy applies its own gap-direction post-filter.</li>
 *   <li>All surviving breakouts are tagged with their source sub-strategy
 *       and merged into a single candidate pool.</li>
 *   <li>A shared ranker scores and selects the top N picks from the
 *       merged pool.</li>
 *   <li>Capital is allocated across sub-strategies using the configured
 *       {@link CapitalAllocator} (default: {@link OrBreadthAllocator}).</li>
 *   <li>Trades are built using the entry/exit prices from each breakout's
 *       originating sub-strategy (preserving the sub-strategy's stop/target).</li>
 * </ol>
 */
public final class CompositeOrbStrategy {

    /**
     * A single sub-strategy component within the composite.
     *
     * @param name   human-readable label (e.g. "Long-Aligned")
     * @param config the fully-configured backtest parameters for this leg
     */
    public record SubStrategy(String name, RotationalBacktestConfig config) {}

    /**
     * A breakout result tagged with its source sub-strategy, so we can
     * trace which config produced each trade.
     */
    public record TaggedBreakout(
            String symbol,
            OrbSimulator.OrbResult orbResult,
            SubStrategy source
    ) {}

    /**
     * A day's merged breakout results across all sub-strategies.
     * Used by the composite engine to rank and select picks.
     */
    public record DayBreakouts(
            LocalDate date,
            /** All breakouts from all sub-strategies, keyed by symbol.
             *  If multiple sub-strategies trigger on the same symbol,
             *  only the first (by sub-strategy order) is kept. */
            Map<String, TaggedBreakout> breakouts
    ) {}

    private final List<SubStrategy> subStrategies;
    private final int totalPicks;
    private final double slippage;
    private final double initialCapital;
    private final CapitalAllocator allocator;

    /**
     * @param subStrategies   ordered list of sub-strategies
     * @param totalPicks      total picks per day across all sub-strategies
     * @param slippage        common slippage for all trades
     * @param initialCapital  starting capital
     * @param allocator       capital allocator (determines per-sub-strategy capital split)
     */
    public CompositeOrbStrategy(List<SubStrategy> subStrategies,
                                 int totalPicks,
                                 double slippage, double initialCapital,
                                 CapitalAllocator allocator) {
        this.subStrategies = List.copyOf(subStrategies);
        this.totalPicks = totalPicks;
        this.slippage = slippage;
        this.initialCapital = initialCapital;
        this.allocator = allocator;
    }

    /**
     * Backward-compatible constructor — defaults to {@link FixedProportionalAllocator}.
     */
    public CompositeOrbStrategy(List<SubStrategy> subStrategies,
                                 int totalPicks,
                                 double slippage, double initialCapital) {
        this(subStrategies, totalPicks,
                slippage, initialCapital, new FixedProportionalAllocator());
    }

    public List<SubStrategy> getSubStrategies() { return subStrategies; }
    public int getTotalPicks() { return totalPicks; }
    public double getSlippage() { return slippage; }
    public double getInitialCapital() { return initialCapital; }
    public CapitalAllocator getAllocator() { return allocator; }
}
