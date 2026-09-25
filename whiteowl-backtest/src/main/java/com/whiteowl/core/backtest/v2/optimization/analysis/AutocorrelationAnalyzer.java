package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7 — autocorrelation of the trade-to-trade return series (never the
 * cumulative equity curve, which is non-stationary by construction).
 * Includes a Ljung-Box Q statistic and ±1.96/√n significance bounds.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AutocorrelationAnalyzer {

    public record Result(double[] acf, double ljungBoxQ, double significanceBound,
                         int lags, int samples, boolean significant) {
    }

    public static Result analyze(List<TradeObservation> trades, int maxLag) {
        double[] x = trades.stream()
                .mapToDouble(TradeObservation::netPnlPercent)
                .filter(v -> !Double.isNaN(v)).toArray();
        int n = x.length;
        int lags = Math.min(maxLag, n - 1);
        if (lags < 1) {
            return new Result(new double[0], 0, Double.NaN, 0, n, false);
        }
        double mean = 0;
        for (double v : x) mean += v;
        mean /= n;
        double var = 0;
        for (double v : x) var += (v - mean) * (v - mean);

        double[] acf = new double[lags + 1];
        acf[0] = 1.0;
        double q = 0;
        boolean any = false;
        double bound = 1.96 / Math.sqrt(n);
        for (int k = 1; k <= lags; k++) {
            double cov = 0;
            for (int i = k; i < n; i++) {
                cov += (x[i] - mean) * (x[i - k] - mean);
            }
            acf[k] = var > 0 ? cov / var : 0;
            q += n * (n + 2.0) * acf[k] * acf[k] / (n - k);
            if (Math.abs(acf[k]) > bound) any = true;
        }
        return new Result(acf, q, bound, lags, n, any);
    }

}
