package com.whiteowl.core.backtest.v2.indicator;

public interface Indicator {

    void update(float newValue);

    float value();

}
