package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class RocIndicator implements DerivedIndicator {

    private static final float HUNDRED = 100f;

    private final FloatSmartValue source;
    private final FloatSmartValue output;
    private final int period;
    private int count;

    public RocIndicator(FloatSmartValue source, FloatSmartValue output, int period) {
        this.source = source;
        this.output = output;
        this.period = period;
        this.count = 0;
    }

    @Override
    public void recompute() {
        count++;
        if (count <= period) {
            output.push(Float.NaN);
            return;
        }
        float current = source.value();
        float previous = source.getAt(-period);
        float result = Float.isNaN(previous) || previous == 0f
                ? Float.NaN
                : ((current - previous) / previous) * HUNDRED;
        output.push(result);
    }

    @Override
    public float value() {
        return output.value();
    }

}
