package com.whiteowl.core.backtest.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class EquityCurve {

    private final float[] values;
    private final long[] timestamps;
    private final int size;

}
