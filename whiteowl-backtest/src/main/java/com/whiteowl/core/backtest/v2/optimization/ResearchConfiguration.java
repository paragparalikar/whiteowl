package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * Central configuration for an optimization/research job.
 *
 * <p>Every threshold that influences optimization decisions lives here. Nothing
 * important is hard-coded in the engines. All fields have sensible defaults so
 * a bare {@code ResearchConfiguration.builder().build()} is usable.</p>
 */
@Getter
@Builder(toBuilder = true)
public final class ResearchConfiguration {

    // ── Execution ────────────────────────────────────────────────────────
    /** Worker threads for parallel evaluation. 0 → availableProcessors. */
    @Builder.Default
    private final int workerCount = 0;
    /** Hard cap on generated parameter combinations (grid explosion guard). */
    @Builder.Default
    private final int maxParameterCombinations = 1_000_000;

    // ── Backtest costs / capital (mirrors BacktestConfig defaults) ───────
    @Builder.Default
    private final float initialCapital = BacktestConfig.DEFAULT_INITIAL_CAPITAL;
    @Builder.Default
    private final float costPercent = BacktestConfig.DEFAULT_COST_PERCENT;
    @Builder.Default
    private final float slippagePercent = BacktestConfig.DEFAULT_SLIPPAGE_PERCENT;
    @Builder.Default
    private final float volumeParticipationPercent = BacktestConfig.DEFAULT_VOLUME_PARTICIPATION_PERCENT;

    // ── Sample adequacy ──────────────────────────────────────────────────
    /** A combination below this trade count is marked INSUFFICIENT_SAMPLE. */
    @Builder.Default
    private final int minTradesPerCombination = 30;
    /** A final strategy below this trade count gets a LOW_TRADE_COUNT warning. */
    @Builder.Default
    private final int minTradesForStrategy = 100;

    // ── Plateau detection ────────────────────────────────────────────────
    /**
     * Fraction of the curve optimum that defines plateau membership, e.g. 0.95
     * means values within 5% of the best metric qualify.
     */
    @Builder.Default
    private final double plateauThreshold = 0.95;
    /** Minimum number of contiguous grid points a plateau must span. */
    @Builder.Default
    private final int minPlateauPoints = 3;
    /**
     * Maximum allowed spread of raw metrics inside a plateau, expressed as a
     * fraction of the region's best metric (rejects noisy plateaus).
     */
    @Builder.Default
    private final double maxPlateauSpreadFraction = 0.15;
    /** Centered moving-average window for curve smoothing. 1 disables. */
    @Builder.Default
    private final int smoothingWindow = 1;

    // ── Robustness score weights (must sum to 1.0) ───────────────────────
    @Builder.Default
    private final double weightPerformance = 0.40;
    @Builder.Default
    private final double weightStability = 0.25;
    @Builder.Default
    private final double weightWidth = 0.20;
    @Builder.Default
    private final double weightTradeCount = 0.15;

    // ── Walk-forward ─────────────────────────────────────────────────────
    /** Coefficient-of-variation above which a parameter is flagged unstable. */
    @Builder.Default
    private final double parameterDriftCvThreshold = 0.30;
    /** OOS Sortino below this fraction of IS Sortino → POOR OOS warning. */
    @Builder.Default
    private final double oosDegradationThreshold = 0.70;

    // ── Phase 8: validation ──────────────────────────────────────────────
    /** Total hypotheses (combos + filter regions) above which →
     *  POSSIBLE_OVERFITTING (data-snooping) warning. */
    @Builder.Default
    private final int multipleTestingThreshold = 200;
    /** Monte-Carlo p99 max drawdown above this % → HIGH_DRAWDOWN warning. */
    @Builder.Default
    private final double maxDrawdownThresholdPercent = 50.0;

}
