package com.whiteowl.core.trendline;

import com.whiteowl.core.bar.model.Bars;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.whiteowl.core.trendline.TrendlineDirection.ASCENDING;
import static com.whiteowl.core.trendline.TrendlineDirection.DESCENDING;
import static com.whiteowl.core.trendline.TrendlineType.RESISTANCE;
import static com.whiteowl.core.trendline.TrendlineType.SUPPORT;

public final class TrendlineDetector {

    private static final int DEFAULT_PIVOT_LOOKBACK = 2;
    private static final double DEFAULT_TOUCH_TOLERANCE = 0.003;
    private static final int DEFAULT_MAX_VIOLATIONS = 5;
    private static final int DEFAULT_MIN_TOUCHES = 3;
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double DEFAULT_VIOLATION_PENALTY = 2.0;
    private static final double DEFAULT_RECENCY_WEIGHT = 0.5;
    private static final double DEFAULT_DISTRIBUTION_WEIGHT = 5.0;
    private static final double DEFAULT_MIN_SPAN_RATIO = 0.3;
    private static final double DEDUP_SLOPE_TOLERANCE = 0.0001;
    private static final double DEDUP_INTERCEPT_TOLERANCE_PCT = 0.005;

    private int pivotLookback = DEFAULT_PIVOT_LOOKBACK;
    private boolean useCloseForPivots = true;
    private double touchTolerance = DEFAULT_TOUCH_TOLERANCE;
    private int maxViolations = DEFAULT_MAX_VIOLATIONS;
    private int minTouches = DEFAULT_MIN_TOUCHES;
    private int maxResults = DEFAULT_MAX_RESULTS;
    private double violationPenalty = DEFAULT_VIOLATION_PENALTY;
    private double recencyWeight = DEFAULT_RECENCY_WEIGHT;
    private double distributionWeight = DEFAULT_DISTRIBUTION_WEIGHT;
    private double minSpanRatio = DEFAULT_MIN_SPAN_RATIO;

    public TrendlineDetector pivotLookback(int lookback) {
        this.pivotLookback = lookback;
        return this;
    }

    public TrendlineDetector useCloseForPivots(boolean useClose) {
        this.useCloseForPivots = useClose;
        return this;
    }

    public TrendlineDetector touchTolerance(double tolerance) {
        this.touchTolerance = tolerance;
        return this;
    }

    public TrendlineDetector maxViolations(int max) {
        this.maxViolations = max;
        return this;
    }

    public TrendlineDetector minTouches(int min) {
        this.minTouches = min;
        return this;
    }

    public TrendlineDetector maxResults(int max) {
        this.maxResults = max;
        return this;
    }

    public TrendlineDetector violationPenalty(double penalty) {
        this.violationPenalty = penalty;
        return this;
    }

    public TrendlineDetector recencyWeight(double weight) {
        this.recencyWeight = weight;
        return this;
    }

    public TrendlineDetector distributionWeight(double weight) {
        this.distributionWeight = weight;
        return this;
    }

    public TrendlineDetector minSpanRatio(double ratio) {
        this.minSpanRatio = ratio;
        return this;
    }

    public List<Trendline> findResistanceLines(Bars bars) {
        return findResistanceLines(bars, TrendlineDirection.ANY);
    }

    public List<Trendline> findResistanceLines(Bars bars, TrendlineDirection direction) {
        List<Pivot> pivots = PivotDetector.findPivotHighs(bars, pivotLookback, useCloseForPivots);
        return fitLines(bars, pivots, RESISTANCE, direction);
    }

    public List<Trendline> findSupportLines(Bars bars) {
        return findSupportLines(bars, TrendlineDirection.ANY);
    }

    public List<Trendline> findSupportLines(Bars bars, TrendlineDirection direction) {
        List<Pivot> pivots = PivotDetector.findPivotLows(bars, pivotLookback, useCloseForPivots);
        return fitLines(bars, pivots, SUPPORT, direction);
    }

    private List<Trendline> fitLines(Bars bars, List<Pivot> pivots, TrendlineType type,
                                     TrendlineDirection direction) {
        if (pivots.size() < minTouches) return List.of();
        List<Trendline> candidates = new ArrayList<>();
        for (int i = 0; i < pivots.size() - 1; i++) {
            for (int j = i + 1; j < pivots.size(); j++) {
                Trendline line = evaluateLine(bars, pivots, pivots.get(i), pivots.get(j), type, direction);
                if (line != null) {
                    candidates.add(line);
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Trendline::getScore).reversed());
        return deduplicate(candidates);
    }

    private Trendline evaluateLine(Bars bars, List<Pivot> allPivots, Pivot a, Pivot b,
                                   TrendlineType type, TrendlineDirection direction) {
        double slope = (double) (b.getPrice() - a.getPrice()) / (b.getIndex() - a.getIndex());
        if (!matchesDirection(slope, direction)) return null;
        double intercept = a.getPrice() - slope * a.getIndex();
        List<Pivot> touches = collectTouches(allPivots, slope, intercept);
        if (touches.size() < minTouches) return null;
        int startIndex = touches.stream().mapToInt(Pivot::getIndex).min().orElse(a.getIndex());
        int endIndex = touches.stream().mapToInt(Pivot::getIndex).max().orElse(bars.size() - 1);
        int span = endIndex - startIndex;
        int availableBars = endIndex - startIndex + 1;
        if (availableBars > 0 && (double) span / bars.size() < minSpanRatio) return null;
        int violations = countViolations(bars, slope, intercept, type, startIndex, endIndex + 1);
        if (violations > maxViolations) return null;
        double score = computeScore(touches, violations, bars.size(), availableBars);
        TrendlineDirection dir = slope < 0 ? DESCENDING : ASCENDING;
        return new Trendline(slope, intercept, type, dir, touches, violations, score, startIndex, endIndex);
    }

    private boolean matchesDirection(double slope, TrendlineDirection direction) {
        return switch (direction) {
            case ASCENDING -> slope > 0;
            case DESCENDING -> slope < 0;
            case ANY -> true;
        };
    }

    private List<Pivot> collectTouches(List<Pivot> pivots, double slope, double intercept) {
        List<Pivot> touches = new ArrayList<>();
        for (Pivot p : pivots) {
            double linePrice = slope * p.getIndex() + intercept;
            double distance = Math.abs(p.getPrice() - linePrice) / linePrice;
            if (distance <= touchTolerance) {
                touches.add(p);
            }
        }
        return touches;
    }

    private int countViolations(Bars bars, double slope, double intercept,
                                TrendlineType type, int from, int to) {
        int violations = 0;
        for (int i = from; i < to; i++) {
            if (Float.compare(bars.getOpen(i), bars.getHigh(i)) == 0) continue;
            double linePrice = slope * i + intercept;
            boolean violated = (type == RESISTANCE)
                    ? bars.getHigh(i) > linePrice * (1 + touchTolerance)
                    : bars.getLow(i) < linePrice * (1 - touchTolerance);
            if (violated) violations++;
        }
        return violations;
    }

    private double computeScore(List<Pivot> touches, int violations, int totalBars, int availableBars) {
        double touchScore = touches.size();
        double penaltyScore = violations * violationPenalty;
        double avgRecency = touches.stream()
                .mapToDouble(p -> (double) p.getIndex() / totalBars)
                .average()
                .orElse(0);
        double recencyBonus = avgRecency * recencyWeight;
        double distributionBonus = computeDistributionScore(touches, availableBars) * distributionWeight;
        return touchScore - penaltyScore + recencyBonus + distributionBonus;
    }

    private double computeDistributionScore(List<Pivot> touches, int availableBars) {
        if (touches.size() < 2) return 0;
        List<Pivot> sorted = touches.stream()
                .sorted(Comparator.comparingInt(Pivot::getIndex))
                .toList();
        double[] gaps = new double[sorted.size() - 1];
        double sumGaps = 0;
        for (int i = 0; i < gaps.length; i++) {
            gaps[i] = sorted.get(i + 1).getIndex() - sorted.get(i).getIndex();
            sumGaps += gaps[i];
        }
        double meanGap = sumGaps / gaps.length;
        double variance = 0;
        for (double gap : gaps) {
            double diff = gap - meanGap;
            variance += diff * diff;
        }
        variance /= gaps.length;
        double stdDev = Math.sqrt(variance);
        double cv = meanGap > 0 ? stdDev / meanGap : 0;
        double evenness = 1.0 / (1.0 + cv);
        int span = sorted.get(sorted.size() - 1).getIndex() - sorted.get(0).getIndex();
        double spanCoverage = availableBars > 0 ? (double) span / availableBars : 0;
        return evenness * spanCoverage;
    }

    private List<Trendline> deduplicate(List<Trendline> sorted) {
        List<Trendline> unique = new ArrayList<>();
        for (Trendline candidate : sorted) {
            if (unique.size() >= maxResults) break;
            boolean isDuplicate = false;
            for (Trendline existing : unique) {
                if (isSimilar(candidate, existing)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                unique.add(candidate);
            }
        }
        return unique;
    }

    private boolean isSimilar(Trendline a, Trendline b) {
        double slopeDiff = Math.abs(a.getSlope() - b.getSlope());
        double avgIntercept = (Math.abs(a.getIntercept()) + Math.abs(b.getIntercept())) / 2.0;
        double interceptDiff = avgIntercept > 0
                ? Math.abs(a.getIntercept() - b.getIntercept()) / avgIntercept
                : Math.abs(a.getIntercept() - b.getIntercept());
        return slopeDiff < DEDUP_SLOPE_TOLERANCE && interceptDiff < DEDUP_INTERCEPT_TOLERANCE_PCT;
    }

}
