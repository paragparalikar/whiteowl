package com.whiteowl.core.backtest.v2.optimization.analysis;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Phase 5 — univariate filter analysis.
 *
 * <p>For each entry-time feature: order observations into equal-width bins,
 * compute per-bin trade statistics, then find contiguous runs of bins that
 * individually beat the unfiltered baseline. Each run seeds a candidate
 * region which is expanded in both directions while the pooled trades of
 * the expanded region still beat the baseline — the widest stable plateau,
 * not the single hottest bin (the same "plateau over spike" philosophy as
 * parameter selection). The region with the largest pooled improvement is
 * reported; it is accepted only when that improvement is meaningful.</p>
 */
@Slf4j
public final class FilterAnalyzer {

    public record NamedFeature(String name, ToDoubleFunction<TradeObservation> accessor) {
    }

    /** Features captured by {@link EntryFeatureCollector}. */
    public static final List<NamedFeature> DEFAULT_FEATURES = List.of(
            new NamedFeature("adx", TradeObservation::adx),
            new NamedFeature("rsi", TradeObservation::rsi),
            new NamedFeature("roc", TradeObservation::roc),
            new NamedFeature("volStdDevOverMa", TradeObservation::volStdDevOverMa),
            new NamedFeature("atrOverMa", TradeObservation::atrOverMa));

    private final int binCount;
    private final int minTradesPerBin;
    private final double improvementThreshold;

    public FilterAnalyzer(int binCount, int minTradesPerBin, double improvementThreshold) {
        this.binCount = binCount;
        this.minTradesPerBin = minTradesPerBin;
        this.improvementThreshold = improvementThreshold;
    }

    public List<FilterAnalysis> analyze(List<TradeObservation> trades,
                                         List<NamedFeature> features) {
        BucketStats baseline = BucketStats.of(Double.NaN, Double.NaN, "ALL", trades);
        List<FilterAnalysis> out = new ArrayList<>();
        for (NamedFeature f : features) {
            out.add(analyzeFeature(trades, f, baseline));
        }
        return out;
    }

    public FilterAnalysis analyzeFeature(List<TradeObservation> trades,
                                          NamedFeature feature,
                                          BucketStats baseline) {
        List<TradeObservation> valid = trades.stream()
                .filter(t -> !Double.isNaN(feature.accessor().applyAsDouble(t)))
                .sorted(Comparator.comparingDouble(feature.accessor()))
                .toList();
        if (valid.size() < minTradesPerBin * 2L) {
            return new FilterAnalysis(feature.name(), List.of(), false,
                    Double.NaN, Double.NaN, 0, "insufficient observations");
        }
        double lo = feature.accessor().applyAsDouble(valid.get(0));
        double hi = feature.accessor().applyAsDouble(valid.get(valid.size() - 1));
        List<BucketStats> bins = new ArrayList<>();
        double width = (hi - lo) / binCount;
        if (width <= 0) {
            return new FilterAnalysis(feature.name(), List.of(), false,
                    Double.NaN, Double.NaN, 0, "constant feature value");
        }
        List<List<TradeObservation>> binTrades = new ArrayList<>();
        for (int i = 0; i < binCount; i++) {
            double bLo = lo + i * width;
            double bHi = i == binCount - 1 ? hi + 1e-9 : lo + (i + 1) * width;
            final double flo = bLo, fhi = bHi;
            List<TradeObservation> inBin = valid.stream()
                    .filter(t -> {
                        double v = feature.accessor().applyAsDouble(t);
                        return v >= flo && v < fhi;
                    }).toList();
            binTrades.add(inBin);
            bins.add(BucketStats.of(bLo, bHi, null, inBin));
        }

        // Seeds: contiguous runs of bins that individually beat the baseline.
        // Each seed is expanded into the widest contiguous region whose
        // pooled trades still beat the baseline — the plateau around the
        // seed, not just the hottest bin. The candidate with the largest
        // pooled improvement wins; wider coverage breaks ties.
        double bestImprovement = 0, bestCoverage = 0;
        double bestLo = Double.NaN, bestHi = Double.NaN;
        int bestTrades = 0;
        String reason = "no stable improving region";
        int i = 0;
        while (i < bins.size()) {
            if (!qualifies(bins.get(i), baseline)) {
                i++;
                continue;
            }
            int start = i;
            while (i < bins.size() && qualifies(bins.get(i), baseline)) {
                i++;
            }
            int end = i - 1;

            int rLo = start, rHi = end;
            while (rLo > 0 && pooledSortino(binTrades, rLo - 1, rHi)
                    > baseline.sortino()) {
                rLo--;
            }
            while (rHi < bins.size() - 1 && pooledSortino(binTrades, rLo, rHi + 1)
                    > baseline.sortino()) {
                rHi++;
            }
            double improvement = pooledSortino(binTrades, rLo, rHi)
                    - baseline.sortino();
            int regionTrades = pooledTradeCount(binTrades, rLo, rHi);
            double coverage = (double) regionTrades / valid.size();
            if (improvement > bestImprovement
                    || (improvement == bestImprovement
                            && coverage > bestCoverage)) {
                bestImprovement = improvement;
                bestCoverage = coverage;
                bestLo = bins.get(rLo).lowerBound();
                bestHi = bins.get(rHi).upperBound();
                bestTrades = regionTrades;
            }
        }
        boolean accepted = !Double.isNaN(bestLo)
                && bestImprovement >= improvementThreshold;
        if (accepted) {
            reason = String.format(
                    "region [%.4g, %.4g] (%d trades, %.0f%% of population) "
                            + "improves trade Sortino by %+.3f over baseline %.3f",
                    bestLo, bestHi, bestTrades, bestCoverage * 100,
                    bestImprovement, baseline.sortino());
        }
        return new FilterAnalysis(feature.name(), bins, accepted,
                bestLo, bestHi, bestImprovement, reason);
    }

    /** Sortino of all trades pooled from bins {@code lo..hi} inclusive. */
    private double pooledSortino(List<List<TradeObservation>> binTrades,
                                 int lo, int hi) {
        List<TradeObservation> pooled = new ArrayList<>();
        for (int j = lo; j <= hi; j++) {
            pooled.addAll(binTrades.get(j));
        }
        return BucketStats.of(Double.NaN, Double.NaN, null, pooled).sortino();
    }

    private int pooledTradeCount(List<List<TradeObservation>> binTrades,
                                 int lo, int hi) {
        int n = 0;
        for (int j = lo; j <= hi; j++) {
            n += binTrades.get(j).size();
        }
        return n;
    }

    /** A bin qualifies when the trades inside it (and only those trades)
     *  would have produced a better trade-level Sortino than the whole set. */
    private boolean qualifies(BucketStats bin, BucketStats baseline) {
        return bin.tradeCount() >= minTradesPerBin
                && bin.sortino() > baseline.sortino();
    }

    /**
     * Multivariate check: AND-combine two accepted filters and measure the
     * joint population against the baseline. Staged — call for selected pairs
     * only, not the full combinatorial space.
     */
    public BucketStats combine(List<TradeObservation> trades,
                                FilterAnalysis a, ToDoubleFunction<TradeObservation> fa,
                                FilterAnalysis b, ToDoubleFunction<TradeObservation> fb) {
        List<TradeObservation> in = trades.stream()
                .filter(t -> {
                    double va = fa.applyAsDouble(t);
                    double vb = fb.applyAsDouble(t);
                    return va >= a.regionLower() && va < a.regionUpper()
                            && vb >= b.regionLower() && vb < b.regionUpper();
                }).toList();
        return BucketStats.of(a.regionLower(), b.regionUpper(),
                a.feature() + "+" + b.feature(), in);
    }

}
