package com.whiteowl.core.backtest.v2.optimization.analysis;

import com.whiteowl.core.backtest.v2.engine.Side;

/**
 * One closed trade plus the market features present when its entry signal was
 * generated (the bar before the fill bar — no look-ahead). This is the row
 * unit for filter analysis and exploratory analysis.
 */
public record TradeObservation(
        int positionId,
        String scripId,
        Side side,
        int entryBarIndex,
        int exitBarIndex,
        long entryTimestamp,
        long exitTimestamp,
        int holdingBars,
        float netPnl,
        float netPnlPercent,
        float rMultiple,
        // features at signal bar
        float adx,
        float rsi,
        float roc,
        float volStdDevOverMa,
        float atrOverMa,
        float entryAtr,
        // calendar fields derived from entryTimestamp (IST)
        int minutesFromOpen,
        int dayOfWeek,
        int weekOfMonth,
        int monthOfYear) {

    public boolean win() {
        return netPnl > 0;
    }

}
