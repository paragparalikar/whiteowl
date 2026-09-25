package com.whiteowl.core.backtest.v2.optimization.exit;

import lombok.Builder;
import lombok.Getter;

/**
 * Candidate space for exit optimization. {@code null} arrays mean "derive
 * from the excursion distribution"; a disabled component is expressed by a
 * single-element array containing {@code Double.NaN} semantics — see
 * {@link #policies} for how values map to {@link com.whiteowl.core.backtest.v2.engine.StandardExitPolicy}.
 */
@Getter
@Builder
public final class ExitGrid {

    /** Candidate initial stops in ATR multiples; null → derive from MAE. */
    private final double[] stopAtr;
    /** Candidate targets in ATR multiples; null → derive from MFE. */
    private final double[] targetAtr;
    /** Candidate time stops in bars; null → derive from time-to-MFE. */
    private final int[] timeStopBars;
    /** ATR period used for both stops and excursion normalization. */
    @Builder.Default
    private final int atrPeriod = com.whiteowl.core.backtest.v2.engine.StandardExitPolicy.DEFAULT_ATR_PERIOD;
    /** Time stop applied while collecting the raw trade population. */
    @Builder.Default
    private final int rawTradeTimeStop = 500;
    /** Whether initial stop is enabled at all. */
    @Builder.Default
    private final boolean stopEnabled = true;
    /** Whether target is enabled at all. */
    @Builder.Default
    private final boolean targetEnabled = true;
    /** Whether time stop is enabled at all. */
    @Builder.Default
    private final boolean timeStopEnabled = true;

}
