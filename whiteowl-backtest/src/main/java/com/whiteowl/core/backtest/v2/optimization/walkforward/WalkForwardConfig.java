package com.whiteowl.core.backtest.v2.optimization.walkforward;

import lombok.Builder;
import lombok.Getter;

/**
 * Walk-forward window geometry. All durations are epoch-millisecond lengths.
 */
@Getter
@Builder
public final class WalkForwardConfig {

    private static final double DAYS_PER_MONTH = 30.4375;
    private static final long MS_PER_DAY = 86_400_000L;

    /** Length of each optimization (in-sample) window. */
    private final long trainMillis;
    /** Length of each evaluation (out-of-sample) window. */
    private final long testMillis;
    /** How far each successive window steps forward. */
    private final long stepMillis;
    /**
     * Bars of history prepended to every slice for indicator warmup. Trades
     * entered during warmup never count toward window metrics.
     */
    @Builder.Default
    private final int warmupBars = 250;
    /**
     * When true the training start stays anchored at the first timestamp and
     * the train window expands; when false the train window rolls forward.
     */
    @Builder.Default
    private final boolean anchored = false;

    public static WalkForwardConfig ofMonths(double trainMonths, double testMonths,
                                              double stepMonths) {
        return builder()
                .trainMillis((long) (trainMonths * DAYS_PER_MONTH * MS_PER_DAY))
                .testMillis((long) (testMonths * DAYS_PER_MONTH * MS_PER_DAY))
                .stepMillis((long) (stepMonths * DAYS_PER_MONTH * MS_PER_DAY))
                .build();
    }

}
