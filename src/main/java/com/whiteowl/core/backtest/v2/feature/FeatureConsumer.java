package com.whiteowl.core.backtest.v2.feature;

public interface FeatureConsumer {

    void accept(FeatureVector vector);

    void flush();

    void close();

}
