package com.whiteowl.core.trendline;

public final class TrendlineSettings {

    private static final int DEFAULT_PIVOT_LOOKBACK = 2;
    private static final double DEFAULT_TOUCH_TOLERANCE_PCT = 0.3;
    private static final int DEFAULT_MIN_TOUCHES = 3;
    private static final int DEFAULT_MAX_VIOLATIONS = 5;
    private static final double DEFAULT_MIN_SCORE = 2.0;
    private static final int DEFAULT_MAX_LINES = 5;
    private static final boolean DEFAULT_SHOW_RESISTANCE = true;
    private static final boolean DEFAULT_SHOW_SUPPORT = true;

    private int pivotLookback = DEFAULT_PIVOT_LOOKBACK;
    private double touchTolerancePct = DEFAULT_TOUCH_TOLERANCE_PCT;
    private int minTouches = DEFAULT_MIN_TOUCHES;
    private int maxViolations = DEFAULT_MAX_VIOLATIONS;
    private double minScore = DEFAULT_MIN_SCORE;
    private int maxLines = DEFAULT_MAX_LINES;
    private boolean showResistance = DEFAULT_SHOW_RESISTANCE;
    private boolean showSupport = DEFAULT_SHOW_SUPPORT;

    public int getPivotLookback() {
        return pivotLookback;
    }

    public void setPivotLookback(int pivotLookback) {
        this.pivotLookback = pivotLookback;
    }

    public double getTouchTolerancePct() {
        return touchTolerancePct;
    }

    public void setTouchTolerancePct(double touchTolerancePct) {
        this.touchTolerancePct = touchTolerancePct;
    }

    public int getMinTouches() {
        return minTouches;
    }

    public void setMinTouches(int minTouches) {
        this.minTouches = minTouches;
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public void setMaxViolations(int maxViolations) {
        this.maxViolations = maxViolations;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public boolean isShowResistance() {
        return showResistance;
    }

    public void setShowResistance(boolean showResistance) {
        this.showResistance = showResistance;
    }

    public boolean isShowSupport() {
        return showSupport;
    }

    public void setShowSupport(boolean showSupport) {
        this.showSupport = showSupport;
    }

    public TrendlineDetector buildDetector() {
        return new TrendlineDetector()
                .pivotLookback(pivotLookback)
                .useCloseForPivots(false)
                .touchTolerance(touchTolerancePct / 100.0)
                .minTouches(minTouches)
                .maxViolations(maxViolations)
                .maxResults(maxLines);
    }

}
