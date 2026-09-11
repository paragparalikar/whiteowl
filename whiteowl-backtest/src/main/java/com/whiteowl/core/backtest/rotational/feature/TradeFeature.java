package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.backtest.rotational.RotationalTrade;
import com.whiteowl.core.bar.model.Bars;

/**
 * Computes a single numeric feature for each trade in the backtest.
 *
 * <p>A {@code TradeFeature} operates on individual trades and produces
 * one value per trade. This is useful for post-hoc analysis of trade quality,
 * e.g. OR-range / ATR, gap size, volume profile, etc.</p>
 *
 * <p>Implementations receive a {@link TradeContext} containing the trade, the
 * scrip's daily and intraday bars, and precomputed indices so they can look
 * up prior-day indicators, OR-period volume, gap, etc. without lookahead.</p>
 *
 * <h3>Implementing a new trade feature</h3>
 * <ol>
 *   <li>Create a class that implements {@code TradeFeature}.</li>
 *   <li>Pass it to {@link TradeFeatureCollector} to compute values across all trades.</li>
 *   <li>Pass it to a {@link FeatureAnalyzer} for bucket/sweep analysis.</li>
 * </ol>
 *
 * @see OrbRangeAtrFeature
 * @see GapAtrFeature
 * @see OrbRvolFeature
 */
public interface TradeFeature {

    /**
     * Unique name for this feature (used as a column header in CSV output).
     */
    String name();

    /**
     * Compute the feature value for a single trade.
     *
     * @param ctx context containing the trade, daily/intraday bars, and indices
     * @return the feature value, or {@code Double.NaN} if it cannot be computed
     */
    double compute(TradeContext ctx);

    /**
     * Context passed to {@link TradeFeature#compute} for each trade.
     *
     * <p>All fields may be null or -1 if the corresponding data is unavailable.
     * Feature implementations must handle missing data gracefully by returning
     * {@code Double.NaN}.</p>
     *
     * @param trade            the trade being analyzed
     * @param dailyBars        the scrip's daily bars (for ATR, gap, etc.), may be null
     * @param dayIndex         index in dailyBars for the trade date, or -1
     * @param intradayBars     the scrip's intraday bars (for volume, OR computation), may be null
     * @param intradayDayStart first intraday bar index for the trade date, or -1
     * @param intradayDayCount number of intraday bars on the trade date, or 0
     * @param orBarCount       number of intraday bars forming the opening range
     */
    record TradeContext(RotationalTrade trade,
                        Bars dailyBars, int dayIndex,
                        Bars intradayBars, int intradayDayStart, int intradayDayCount,
                        int orBarCount) {}
}
