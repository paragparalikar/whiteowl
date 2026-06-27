package com.whiteowl.core.backtest.engine;

import com.whiteowl.core.backtest.dsl.Signal;
import com.whiteowl.core.bar.model.BarsArrays;

record ScripSignal(String scripId, Signal signal, BarsArrays arrays, long timestamp)
        implements Comparable<ScripSignal> {

    @Override
    public int compareTo(ScripSignal other) {
        int cmp = Long.compare(timestamp, other.timestamp);
        if (cmp != 0) return cmp;
        return scripId.compareTo(other.scripId);
    }

}
