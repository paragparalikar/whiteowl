package com.whiteowl.core.backtest.algotest;

import com.whiteowl.core.backtest.algotest.model.AlgoStrategy;
import com.whiteowl.core.backtest.algotest.model.enums.StrikeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * One dimension of an optimization grid: a named set of values plus a mutator
 * that applies a value to the strategy template. The optimizer runs the
 * cartesian product of all sweeps.
 *
 * @param <T> value type (Double for numeric ranges, enums for strike types etc.)
 */
public class ParameterSweep<T> {

    private final String name;
    private final List<T> values;
    private final BiConsumer<AlgoStrategy, T> applier;

    public ParameterSweep(String name, List<T> values, BiConsumer<AlgoStrategy, T> applier) {
        this.name = name;
        this.values = values;
        this.applier = applier;
    }

    public String getName() {
        return name;
    }

    public List<T> getValues() {
        return values;
    }

    public void apply(AlgoStrategy strategy, T value) {
        applier.accept(strategy, value);
    }

    /** Inclusive numeric range: min, min+step, ... <= max. */
    public static ParameterSweep<Double> numericRange(String name, double min, double max, double step,
                                                      BiConsumer<AlgoStrategy, Double> applier) {
        List<Double> values = new ArrayList<>();
        for (double v = min; v <= max + 1e-9; v += step) {
            values.add(v);
        }
        return new ParameterSweep<>(name, values, applier);
    }

    /** Inclusive integer range: min, min+step, ... <= max. */
    public static ParameterSweep<Integer> intRange(String name, int min, int max, int step,
                                                   BiConsumer<AlgoStrategy, Integer> applier) {
        List<Integer> values = new ArrayList<>();
        for (int v = min; v <= max; v += step) {
            values.add(v);
        }
        return new ParameterSweep<>(name, values, applier);
    }

    /** Explicit list of values. */
    public static <T> ParameterSweep<T> of(String name, List<T> values,
                                           BiConsumer<AlgoStrategy, T> applier) {
        return new ParameterSweep<>(name, values, applier);
    }

    /** Inclusive enum range by declaration order (e.g. StrikeType.ATM to StrikeType.OTM10). */
    public static <E extends Enum<E>> ParameterSweep<E> enumRange(String name, E from, E to,
                                                                  BiConsumer<AlgoStrategy, E> applier) {
        E[] constants = from.getDeclaringClass().getEnumConstants();
        if (from.ordinal() > to.ordinal()) {
            throw new IllegalArgumentException(from + " is declared after " + to);
        }
        List<E> values = new ArrayList<>(to.ordinal() - from.ordinal() + 1);
        for (int i = from.ordinal(); i <= to.ordinal(); i++) {
            values.add(constants[i]);
        }
        return new ParameterSweep<>(name, values, applier);
    }

    /** Inclusive strike range, e.g. ATM to OTM10. */
    public static ParameterSweep<StrikeType> strikes(String name, StrikeType from, StrikeType to,
                                                     BiConsumer<AlgoStrategy, StrikeType> applier) {
        return new ParameterSweep<>(name, StrikeType.range(from, to), applier);
    }
}
