package com.whiteowl.workbench.charting;

public final class ChartViewport {

    private static final int DEFAULT_VISIBLE_BARS = 120;
    private static final int MIN_VISIBLE_BARS = 20;
    private static final int MAX_VISIBLE_BARS = 2000;
    private static final double BAR_BODY_RATIO = 0.6;
    private static final double RIGHT_OVERSHOOT_RATIO = 0.5;
    private static final double RIGHT_MARGIN_RATIO = 0.1;

    private int startIndex;
    private int visibleBars;
    private int totalBars;

    public ChartViewport() {
        this.visibleBars = DEFAULT_VISIBLE_BARS;
    }

    public void configure(int totalBars) {
        this.totalBars = totalBars;
        int rightMargin = computeRightMargin();
        this.startIndex = Math.max(computeMinStart(), totalBars - visibleBars + rightMargin);
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return startIndex + visibleBars;
    }

    public int getVisibleBars() {
        return visibleBars;
    }

    public int getTotalBars() {
        return totalBars;
    }

    public double computeBarWidth(double chartWidth) {
        return chartWidth / visibleBars;
    }

    public double computeBodyWidth(double barWidth) {
        return barWidth * BAR_BODY_RATIO;
    }

    public void panBy(int barDelta) {
        int newStart = startIndex + barDelta;
        int maxStart = totalBars - visibleBars + computeRightOvershoot();
        startIndex = Math.max(computeMinStart(), Math.min(newStart, maxStart));
    }

    public void zoomBy(int delta) {
        int newVisible = visibleBars + delta;
        newVisible = Math.max(MIN_VISIBLE_BARS, Math.min(newVisible, MAX_VISIBLE_BARS));
        int center = startIndex + visibleBars / 2;
        visibleBars = newVisible;
        int maxStart = totalBars - visibleBars + computeRightOvershoot();
        startIndex = Math.max(computeMinStart(), Math.min(center - visibleBars / 2, maxStart));
    }

    public boolean canPanLeft() {
        return startIndex > computeMinStart();
    }

    public boolean canPanRight() {
        return startIndex + visibleBars < totalBars + computeRightOvershoot();
    }

    private int computeRightOvershoot() {
        return (int) (visibleBars * RIGHT_OVERSHOOT_RATIO);
    }

    private int computeRightMargin() {
        return (int) (visibleBars * RIGHT_MARGIN_RATIO);
    }

    private int computeMinStart() {
        return -(int) (visibleBars * RIGHT_OVERSHOOT_RATIO);
    }

}
