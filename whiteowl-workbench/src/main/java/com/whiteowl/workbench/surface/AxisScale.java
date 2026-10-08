package com.whiteowl.workbench.surface;

import java.util.ArrayList;
import java.util.List;

/**
 * Labeling strategy for one surface axis. A numeric axis shows evenly spaced
 * formatted values; a categorical axis maps each distinct column value to an
 * integer position and labels the ticks with the category names.
 */
public record AxisScale(List<String> categories) {

    public static final AxisScale NUMERIC = new AxisScale(List.of());

    public boolean isCategorical() {
        return !categories.isEmpty();
    }

    /**
     * Tick positions for this axis: an even spread across {@code [min, max]}
     * for numeric axes, or one tick per category for categorical axes (evenly
     * subsampled when there are more categories than {@code maxTicks}).
     */
    public List<Double> ticks(double min, double max, int maxTicks) {
        List<Double> ticks = new ArrayList<>();
        if (!isCategorical()) {
            for (int i = 0; i < maxTicks; i++) {
                double t = maxTicks > 1 ? (double) i / (maxTicks - 1) : 0.5;
                ticks.add(min + t * (max - min));
            }
            return ticks;
        }
        int step = Math.max(1, (int) Math.ceil(categories.size() / (double) maxTicks));
        for (int i = 0; i < categories.size(); i += step) {
            ticks.add((double) i);
        }
        return ticks;
    }

    /** Label for a position on this axis. */
    public String format(double value) {
        if (!isCategorical()) {
            return formatTick(value);
        }
        int i = (int) Math.round(value);
        return i >= 0 && i < categories.size() ? categories.get(i) : "";
    }

    private static String formatTick(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        if (Math.abs(value) >= 1000) {
            return String.format("%,.0f", value);
        }
        return String.format("%.4g", value);
    }

}
