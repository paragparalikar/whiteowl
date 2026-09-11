package com.whiteowl.core.bar.aggregation;

import com.whiteowl.core.bar.model.Timeframe;

public interface BarCompletionListener {

    void onBarCompleted(String scripId, Timeframe timeframe, long timestamp,
                        float open, float high, float low, float close, long volume);

    default void onBarUpdated(String scripId, Timeframe timeframe, long timestamp,
                              float open, float high, float low, float close, long volume) {}

}
