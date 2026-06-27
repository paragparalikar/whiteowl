package com.whiteowl.core.backtest.v2.indicator;

public interface DerivedIndicator {

    void recompute();

    float value();

}
