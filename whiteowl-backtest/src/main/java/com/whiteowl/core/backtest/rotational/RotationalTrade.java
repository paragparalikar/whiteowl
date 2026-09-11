package com.whiteowl.core.backtest.rotational;

import java.time.LocalTime;

/**
 * A single trade in the rotational backtest.
 *
 * @param date        epoch millis of the trading date
 * @param symbol      scrip ID
 * @param side        LONG or SHORT
 * @param entryPrice  entry price (OR-high for long breakout, OR-low for short, or candle close)
 * @param exitPrice   exit price (stop, target, or market close — see {@link #exitReason})
 * @param shares      number of shares
 * @param grossPnl    gross P&L before slippage
 * @param netPnl      net P&L after slippage
 * @param modelRank   model's predicted rank for this symbol (0 if no model)
 * @param entryTime   time of day (IST) when the entry was triggered (null for daily proxy)
 * @param orHigh      opening range high
 * @param orLow       opening range low
 * @param exitReason  why the trade was exited (stop loss, target, market close, etc.)
 */
public record RotationalTrade(long date, String symbol, Side side,
                              double entryPrice, double exitPrice,
                              double shares, double grossPnl, double netPnl,
                              double modelRank,
                              LocalTime entryTime, float orHigh, float orLow,
                              ExitReason exitReason) {

    public enum Side { LONG, SHORT }

    /**
     * Gross return = grossPnl / (entryPrice * shares).
     */
    public double grossReturn() {
        double notional = entryPrice * shares;
        return (notional > 0) ? grossPnl / notional : 0;
    }

    /**
     * Net return = netPnl / (entryPrice * shares).
     */
    public double netReturn() {
        double notional = entryPrice * shares;
        return (notional > 0) ? netPnl / notional : 0;
    }

    /** Convenience: true if exited via stop loss. */
    public boolean stoppedOut() {
        return exitReason == ExitReason.STOP_LOSS;
    }

    /** Convenience: true if exited via target. */
    public boolean targetHit() {
        return exitReason == ExitReason.TARGET;
    }
}
