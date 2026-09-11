package com.whiteowl.core.backtest.rotational.regime;

/**
 * Regime features for a single trading day. All values are backward-looking
 * (computed from data available before the day's market open).
 *
 * @param continuationRate  % of prior 20 days where first-30-min direction matched day close (0–1)
 * @param realizedVol       Annualized realized volatility of Nifty 50 over prior 20 days
 * @param crossSectionalVol Average cross-sectional std dev of universe stock returns (prior 20 days)
 * @param gapFillRate       % of gaps that were filled in the prior 20 days across the universe (0–1)
 * @param smaSlope          Annualized slope of Nifty 50's 20-day SMA (positive = uptrend)
 */
public record RegimeFeatures(
        double continuationRate,
        double realizedVol,
        double crossSectionalVol,
        double gapFillRate,
        double smaSlope
) {
    /** CSV header. */
    public static String csvHeader() {
        return "continuation_rate_20d,realized_vol_20d,cross_sectional_vol_20d,gap_fill_rate_20d,nifty_sma_slope_20d";
    }

    /** CSV row. */
    public String toCsvRow() {
        return String.format("%.4f,%.4f,%.6f,%.4f,%.4f",
                continuationRate, realizedVol, crossSectionalVol, gapFillRate, smaSlope);
    }
}
