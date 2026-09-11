package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class AtrIndicator implements DerivedIndicator {

    private final FloatSmartValue high;
    private final FloatSmartValue low;
    private final FloatSmartValue close;
    private final int period;
    private float atrValue;
    private int count;
    private float trSum;

    public AtrIndicator(FloatSmartValue high, FloatSmartValue low, FloatSmartValue close, int period) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.period = period;
        this.atrValue = Float.NaN;
        this.count = 0;
        this.trSum = 0f;
    }

    @Override
    public void recompute() {
        count++;
        float tr = computeTrueRange();
        if (count <= period) {
            trSum += tr;
            if (count == period) {
                atrValue = trSum / period;
            }
        } else {
            atrValue = (atrValue * (period - 1) + tr) / period;
        }
    }

    private float computeTrueRange() {
        float h = high.value();
        float l = low.value();
        if (count == 1) {
            return h - l;
        }
        float prevClose = close.getAt(-1);
        float hl = h - l;
        float hc = Math.abs(h - prevClose);
        float lc = Math.abs(l - prevClose);
        return Math.max(hl, Math.max(hc, lc));
    }

    @Override
    public float value() {
        return atrValue;
    }

}
