package com.whiteowl.core.backtest.v2.optimization;

import java.util.Map;
import java.util.function.Predicate;

/**
 * A validity constraint over a parameter combination. Combinations that fail
 * any constraint are rejected before backtesting (e.g.
 * {@code shortEma < longEma} for a crossover strategy).
 */
public interface ParameterConstraint {

    boolean isValid(Map<String, Number> combination);

    /** Human-readable description, used in rejection logs and audit output. */
    String description();

    static ParameterConstraint of(String description, Predicate<Map<String, Number>> predicate) {
        return new ParameterConstraint() {
            @Override
            public boolean isValid(Map<String, Number> combination) {
                return predicate.test(combination);
            }

            @Override
            public String description() {
                return description;
            }
        };
    }

    /** Constraint {@code a < b} on two numeric parameters. */
    static ParameterConstraint lessThan(String a, String b) {
        return of(a + " < " + b,
                c -> c.get(a).doubleValue() < c.get(b).doubleValue());
    }

    /** Constraint {@code a > b} on two numeric parameters. */
    static ParameterConstraint greaterThan(String a, String b) {
        return of(a + " > " + b,
                c -> c.get(a).doubleValue() > c.get(b).doubleValue());
    }

}
