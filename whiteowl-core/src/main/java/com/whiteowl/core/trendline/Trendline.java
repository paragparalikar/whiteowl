package com.whiteowl.core.trendline;

import java.util.List;

public final class Trendline {

    private final double slope;
    private final double intercept;
    private final TrendlineType type;
    private final TrendlineDirection direction;
    private final List<Pivot> touches;
    private final int violations;
    private final double score;
    private final int startIndex;
    private final int endIndex;

    public Trendline(double slope, double intercept, TrendlineType type, TrendlineDirection direction,
                     List<Pivot> touches, int violations, double score, int startIndex, int endIndex) {
        this.slope = slope;
        this.intercept = intercept;
        this.type = type;
        this.direction = direction;
        this.touches = touches;
        this.violations = violations;
        this.score = score;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public double priceAt(int barIndex) {
        return slope * barIndex + intercept;
    }

    public double getSlope() {
        return slope;
    }

    public double getIntercept() {
        return intercept;
    }

    public TrendlineType getType() {
        return type;
    }

    public TrendlineDirection getDirection() {
        return direction;
    }

    public List<Pivot> getTouches() {
        return touches;
    }

    public int getViolations() {
        return violations;
    }

    public double getScore() {
        return score;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

}
