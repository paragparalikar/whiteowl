package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.backtest.rotational.RotationalTrade;

import java.util.List;
import java.util.Map;

/**
 * Analyzes the relationship between a trade feature and trade outcomes
 * (P&L, win rate) to find optimal filter thresholds.
 *
 * <p>Implementations produce console-friendly analysis reports. Each analyzer
 * can run different styles of analysis (buckets, sweeps, combinations, etc.).</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   FeatureAnalyzer analyzer = new BucketAnalyzer("OR_RANGE_ATR", 0.1);
 *   analyzer.analyze(trades, featureValues);
 * </pre>
 *
 * @see BucketAnalyzer
 */
public interface FeatureAnalyzer {

    /**
     * Run the analysis and log results.
     *
     * @param trades        the backtest trade log
     * @param featureValues per-trade feature maps (parallel to trades list)
     */
    void analyze(List<RotationalTrade> trades, List<Map<String, Double>> featureValues);
}
