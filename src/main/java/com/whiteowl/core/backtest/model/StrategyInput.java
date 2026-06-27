package com.whiteowl.core.backtest.model;

import lombok.Getter;

@Getter
public final class StrategyInput {

    private static final Number DEFAULT_STEP = 1;

    private final String name;
    private final Number defaultValue;
    private final Number minValue;
    private final Number maxValue;
    private final Number step;

    public StrategyInput(String name, Number defaultValue) {
        this(name, defaultValue, null, null, DEFAULT_STEP);
    }

    public StrategyInput(String name, Number defaultValue, Number minValue, Number maxValue) {
        this(name, defaultValue, minValue, maxValue, DEFAULT_STEP);
    }

    public StrategyInput(String name, Number defaultValue, Number minValue, Number maxValue, Number step) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step != null ? step : DEFAULT_STEP;
    }

    public boolean hasMin() {
        return minValue != null;
    }

    public boolean hasMax() {
        return maxValue != null;
    }

}
