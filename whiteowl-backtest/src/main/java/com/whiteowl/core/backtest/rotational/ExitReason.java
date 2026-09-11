package com.whiteowl.core.backtest.rotational;

/**
 * Describes why a trade was exited.
 *
 * <p>Each {@link ExitRule} implementation maps to one reason. The simulator
 * evaluates exit rules in priority order and the first one that fires
 * determines the exit reason for that trade.</p>
 */
public enum ExitReason {

    /** Exited via opening-range stop loss (OR-low for long, OR-high for short). */
    STOP_LOSS,

    /** Exited via a profit target (e.g. 2x stop-loss distance). */
    TARGET,

    /** Exited via a trailing stop. */
    TRAILING_STOP,

    /** Position held until market close (no intraday exit rule fired). */
    MARKET_CLOSE,

    /** Exited at a configured time of day (before market close). */
    TIME_EXIT
}
