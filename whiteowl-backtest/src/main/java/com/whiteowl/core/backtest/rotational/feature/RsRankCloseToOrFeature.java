package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

import java.time.LocalDate;
import java.util.Map;

/**
 * Cross-sectional trade feature: RS rank based on the return from
 * yesterday's close to the end of the opening range.
 *
 * <p>{@code return = (OR_end_close - prior_day_close) / prior_day_close}</p>
 *
 * <p>This captures the overnight gap <em>plus</em> the opening range move,
 * measuring total momentum from prior close into the OR. Scrips are ranked
 * against all others in the universe on the same day (percentile 0-100).</p>
 *
 * <p>A rank of 100 means the scrip had the strongest prior-close-to-OR-end
 * return among all scrips that day; 0 means the weakest.</p>
 */
public final class RsRankCloseToOrFeature extends AbstractRsRankFeature {

    @Override
    public String name() {
        return "RS_RANK_CLOSE_TO_OR";
    }

    @Override
    protected double computeReturn(String symbol, Bars intradayBars,
                                    int dayStart, int dayCount, int orBarCount,
                                    Bars dailyBars, Map<LocalDate, Integer> dailyDateIndex,
                                    LocalDate date) {
        if (dailyBars == null || dailyDateIndex == null) return Double.NaN;

        // Find prior day's close in daily bars
        Integer dayIdx = dailyDateIndex.get(date);
        if (dayIdx == null || dayIdx < 1) return Double.NaN;
        float priorClose = dailyBars.getClose(dayIdx - 1);
        if (priorClose <= 0 || Float.isNaN(priorClose)) return Double.NaN;

        // OR-end price: close of the last bar in the opening range
        int orEndIdx = dayStart + orBarCount - 1;
        if (orEndIdx >= dayStart + dayCount) return Double.NaN;
        float orEndClose = intradayBars.getClose(orEndIdx);
        if (Float.isNaN(orEndClose) || orEndClose <= 0) return Double.NaN;

        return (orEndClose - priorClose) / priorClose;
    }
}
