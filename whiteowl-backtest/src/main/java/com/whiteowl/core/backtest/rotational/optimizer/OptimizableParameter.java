package com.whiteowl.core.backtest.rotational.optimizer;

/**
 * Defines a numeric parameter that can be optimized over a range.
 *
 * @param name       display name of the parameter (must match a config field)
 * @param fieldName  config builder method name (e.g. "stopMultiplier")
 * @param min        minimum value (inclusive)
 * @param max        maximum value (inclusive)
 * @param step       step size for grid generation
 */
public record OptimizableParameter(
        String name,
        String fieldName,
        double min,
        double max,
        double step
) {

    public OptimizableParameter {
        if (min > max) throw new IllegalArgumentException("min must be <= max: " + name);
        if (step <= 0) throw new IllegalArgumentException("step must be > 0: " + name);
    }

    /** Number of grid points for this parameter. */
    public int gridSize() {
        return (int) Math.floor((max - min) / step) + 1;
    }

    /** Value at the given grid index. */
    public double valueAt(int index) {
        return min + index * step;
    }
}
