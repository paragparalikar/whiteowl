package com.whiteowl.core.backtest.v2.model;

import lombok.Getter;

@Getter
public final class EquityCurve {

    private final float[] values;
    private final long[] timestamps;
    private final int size;

    public EquityCurve(float[] values, long[] timestamps, int size) {
        this.values = values;
        this.timestamps = timestamps;
        this.size = size;
    }

}
