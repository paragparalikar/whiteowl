package com.whiteowl.workbench.surface;

/**
 * Comparison operators supported by filter rows. Numeric columns offer the
 * full set; categorical and boolean columns are restricted to
 * {@link #EQUALS} and {@link #NOT_EQUALS}.
 */
public enum FilterOperator {

    EQUALS("="),
    NOT_EQUALS("\u2260"),
    GREATER_THAN(">"),
    GREATER_OR_EQUAL("\u2265"),
    LESS_THAN("<"),
    LESS_OR_EQUAL("\u2264");

    private final String label;

    FilterOperator(String label) {
        this.label = label;
    }

    public boolean test(double value, double target) {
        return switch (this) {
            case EQUALS -> Double.compare(value, target) == 0;
            case NOT_EQUALS -> Double.compare(value, target) != 0;
            case GREATER_THAN -> value > target;
            case GREATER_OR_EQUAL -> value >= target;
            case LESS_THAN -> value < target;
            case LESS_OR_EQUAL -> value <= target;
        };
    }

    @Override
    public String toString() {
        return label;
    }

}
