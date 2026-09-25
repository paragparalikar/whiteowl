package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Phase 7 — market-regime analysis against an externally supplied series
 * (India VIX, market breadth, ...). The series is injected as a function of
 * the trade (e.g. a timestamp-indexed VIX lookup); when data is unavailable,
 * the analysis is simply not run.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegimeAnalyzer {

    public record Result(String seriesName, List<BucketStats> bins,
                         double[] stableRegion /* lo,hi or null */) {
    }

    public static Result analyze(String seriesName, List<TradeObservation> trades,
                                  ToDoubleFunction<TradeObservation> seriesValue,
                                  int binCount, int minTradesPerBin) {
        List<TradeObservation> valid = trades.stream()
                .filter(t -> !Double.isNaN(seriesValue.applyAsDouble(t)))
                .sorted(Comparator.comparingDouble(seriesValue))
                .toList();
        if (valid.size() < minTradesPerBin * 2L) {
            return new Result(seriesName, List.of(), null);
        }
        double lo = seriesValue.applyAsDouble(valid.get(0));
        double hi = seriesValue.applyAsDouble(valid.get(valid.size() - 1));
        double width = (hi - lo) / binCount;
        if (width <= 0) {
            return new Result(seriesName, List.of(), null);
        }
        List<BucketStats> bins = new ArrayList<>();
        for (int i = 0; i < binCount; i++) {
            double bLo = lo + i * width;
            double bHi = i == binCount - 1 ? hi + 1e-9 : lo + (i + 1) * width;
            final double flo = bLo, fhi = bHi;
            List<TradeObservation> inBin = valid.stream()
                    .filter(t -> {
                        double v = seriesValue.applyAsDouble(t);
                        return v >= flo && v < fhi;
                    }).toList();
            bins.add(BucketStats.of(bLo, bHi, null, inBin));
        }
        // Stable profitable region = longest contiguous run of bins that are
        // profitable AND adequately sampled.
        double[] best = null;
        int i = 0;
        while (i < bins.size()) {
            if (bins.get(i).tradeCount() >= minTradesPerBin
                    && bins.get(i).avgPnlPercent() > 0) {
                int start = i;
                while (i < bins.size() && bins.get(i).tradeCount() >= minTradesPerBin
                        && bins.get(i).avgPnlPercent() > 0) {
                    i++;
                }
                if (best == null || i - start > runLen(best, bins)) {
                    best = new double[]{bins.get(start).lowerBound(),
                            bins.get(i - 1).upperBound(), start, i - 1};
                }
            } else {
                i++;
            }
        }
        double[] region = best == null ? null : new double[]{best[0], best[1]};
        return new Result(seriesName, bins, region);
    }

    private static int runLen(double[] best, List<BucketStats> bins) {
        return (int) (best[3] - best[2] + 1);
    }

}
