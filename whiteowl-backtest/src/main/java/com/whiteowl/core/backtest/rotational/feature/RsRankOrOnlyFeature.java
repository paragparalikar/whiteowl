package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

import java.time.LocalDate;
import java.util.Map;

/**
 * Cross-sectional trade feature: RS rank based on the return during
 * the opening range period only.
 *
 * <p>{@code return = (OR_end_close - OR_start_open) / OR_start_open}</p>
 *
 * <p>This isolates the intraday move during the OR, excluding the overnight
 * gap. It measures pure opening-session momentum. Scrips are ranked against
 * all others in the universe on the same day (percentile 0-100).</p>
 *
 * <p>A rank of 100 means the scrip had the strongest OR-period return
 * among all scrips that day; 0 means the weakest.</p>
 */
public final class RsRankOrOnlyFeature extends AbstractRsRankFeature {

    @Override
    public String name() {
        return "RS_RANK_OR_ONLY";
    }

    @Override
    protected double computeReturn(String symbol, Bars intradayBars,
                                    int dayStart, int dayCount, int orBarCount,
                                    Bars dailyBars, Map<LocalDate, Integer> dailyDateIndex,
                                    LocalDate date) {
        if (dayCount < orBarCount) return Double.NaN;

        // OR-start price: open of the first bar
        float orStartOpen = intradayBars.getOpen(dayStart);
        if (Float.isNaN(orStartOpen) || orStartOpen <= 0) return Double.NaN;

        // OR-end price: close of the last bar in the opening range
        int orEndIdx = dayStart + orBarCount - 1;
        float orEndClose = intradayBars.getClose(orEndIdx);
        if (Float.isNaN(orEndClose) || orEndClose <= 0) return Double.NaN;

        return (orEndClose - orStartOpen) / orStartOpen;
    }
}
