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
 * compute per-bin trade statistics, then look for a contiguous region whose
 * bins beat the baseline trade-level metric with adequate samples and stable
 * neighbors — the same "plateau over spike" philosophy as parameter
 * selection. A region is accepted only when it shows meaningful, stable
 * improvement over the unfiltered baseline.</p>
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
        for (int i = 0; i < binCount; i++) {
            double bLo = lo + i * width;
            double bHi = i == binCount - 1 ? hi + 1e-9 : lo + (i + 1) * width;
            final double flo = bLo, fhi = bHi;
            List<TradeObservation> inBin = valid.stream()
                    .filter(t -> {
                        double v = feature.accessor().applyAsDouble(t);
                        return v >= flo && v < fhi;
                    }).toList();
            bins.add(BucketStats.of(bLo, bHi, null, inBin));
        }

        // Contiguous runs whose per-bin trade-level Sortino (computed on the
        // filtered trade subset alone) beats the unfiltered baseline.
        double bestImprovement = 0;
        double bestLo = Double.NaN, bestHi = Double.NaN;
        String reason = "no stable improving region";
        int i = 0;
        while (i < bins.size()) {
            if (qualifies(bins.get(i), baseline)) {
                int start = i;
                double sum = 0;
                int cnt = 0;
                while (i < bins.size() && qualifies(bins.get(i), baseline)) {
                    sum += bins.get(i).sortino();
                    cnt++;
                    i++;
                }
                double improvement = sum / cnt - baseline.sortino();
                if (improvement > bestImprovement) {
                    bestImprovement = improvement;
                    bestLo = bins.get(start).lowerBound();
                    bestHi = bins.get(i - 1).upperBound();
                }
            } else {
                i++;
            }
        }
        boolean accepted = !Double.isNaN(bestLo) && bestImprovement >= improvementThreshold;
        if (accepted) {
            reason = String.format(
                    "region [%.4g, %.4g] improves trade Sortino by %+.3f over baseline %.3f",
                    bestLo, bestHi, bestImprovement, baseline.sortino());
        }
        return new FilterAnalysis(feature.name(), bins, accepted,
                bestLo, bestHi, bestImprovement, reason);
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
