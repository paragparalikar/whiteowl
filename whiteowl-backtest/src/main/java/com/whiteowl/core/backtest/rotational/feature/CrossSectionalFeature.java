package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

import java.time.LocalDate;
import java.util.Map;

/**
 * A trade feature that requires cross-sectional data (all scrips on the same day)
 * to compute a value, such as a percentile rank.
 *
 * <p>Unlike {@link TradeFeature} which computes per-trade independently,
 * a {@code CrossSectionalFeature} first pre-computes values across all scrips
 * for each trading day via {@link #preCompute}, then supplies per-trade lookups
 * via the standard {@link TradeFeature#compute} method.</p>
 *
 * <p>The {@link TradeFeatureCollector} calls {@link #preCompute} once before
 * iterating over trades, so the feature can build any internal lookup tables
 * it needs.</p>
 *
 * <h3>Implementing a new cross-sectional feature</h3>
 * <ol>
 *   <li>Implement this interface.</li>
 *   <li>In {@link #preCompute}, iterate over all scrips' bars, compute the
 *       raw metric per day per scrip, then rank/transform as needed.</li>
 *   <li>In {@link #compute}, look up the pre-computed value by symbol + date.</li>
 * </ol>
 *
 * @see RsRankCloseToOrFeature
 * @see RsRankOrOnlyFeature
 */
public interface CrossSectionalFeature extends TradeFeature {

    /**
     * Pre-compute cross-sectional values across all scrips and all trading days.
     *
     * <p>Called once before any {@link #compute} calls. Implementations should
     * build internal lookup structures (e.g. date+symbol -> rank) here.</p>
     *
     * @param intradayBarsMap per-scrip intraday bars, keyed by scripId
     * @param dailyBarsMap    per-scrip daily bars, keyed by scripId (may be null)
     * @param orBarCount      number of intraday bars forming the opening range
     */
    void preCompute(Map<String, Bars> intradayBarsMap,
                    Map<String, Bars> dailyBarsMap,
                    int orBarCount);
}
