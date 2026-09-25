package com.whiteowl.core.backtest.v2.optimization;

import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable point in the parameter grid. Parameter order is the order the
 * inputs were declared, so {@link #id()} is deterministic.
 */
@EqualsAndHashCode
public final class ParameterCombination {

    private final Map<String, Number> values;

    public ParameterCombination(Map<String, Number> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Number get(String name) {
        return values.get(name);
    }

    public Map<String, Number> values() {
        return values;
    }

    /** Deterministic id, e.g. {@code SHORT_EMA=9,LONG_EMA=42}. */
    public String id() {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(',');
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

    @Override
    public String toString() {
        return id();
    }

    public static ParameterCombination of(Map<String, Number> values) {
        return new ParameterCombination(values);
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private final Map<String, Number> map = new LinkedHashMap<>();
        Builder put(String name, Number value) {
            map.put(name, Objects.requireNonNull(value, name));
            return this;
        }
        ParameterCombination build() {
            return new ParameterCombination(map);
        }
    }

}
