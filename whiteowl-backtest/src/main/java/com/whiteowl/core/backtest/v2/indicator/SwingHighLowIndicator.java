package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class SwingHighLowIndicator implements DerivedIndicator {

    private final FloatSmartValue high;
    private final FloatSmartValue low;
    private final FloatSmartValue swingHighOutput;
    private final FloatSmartValue swingLowOutput;
    private final int strength;
    private float lastSwingHigh;
    private float lastSwingLow;
    private int count;

    public SwingHighLowIndicator(FloatSmartValue high, FloatSmartValue low,
                                  FloatSmartValue swingHighOutput, FloatSmartValue swingLowOutput,
                                  int strength) {
        this.high = high;
        this.low = low;
        this.swingHighOutput = swingHighOutput;
        this.swingLowOutput = swingLowOutput;
        this.strength = strength;
        this.lastSwingHigh = Float.NaN;
        this.lastSwingLow = Float.NaN;
        this.count = 0;
    }

    @Override
    public void recompute() {
        count++;
        if (count > strength) {
            detectSwingHigh();
            detectSwingLow();
        }
        swingHighOutput.push(lastSwingHigh);
        swingLowOutput.push(lastSwingLow);
    }

    private void detectSwingHigh() {
        float candidate = high.getAt(-strength);
        if (Float.isNaN(candidate)) {
            return;
        }
        for (int i = -strength + 1; i <= 0; i++) {
            if (high.getAt(i) > candidate) {
                return;
            }
        }
        for (int i = -strength - 1; i >= -strength * 2; i--) {
            float val = high.getAt(i);
            if (Float.isNaN(val)) {
                return;
            }
            if (val > candidate) {
                return;
            }
        }
        lastSwingHigh = candidate;
    }

    private void detectSwingLow() {
        float candidate = low.getAt(-strength);
        if (Float.isNaN(candidate)) {
            return;
        }
        for (int i = -strength + 1; i <= 0; i++) {
            if (low.getAt(i) < candidate) {
                return;
            }
        }
        for (int i = -strength - 1; i >= -strength * 2; i--) {
            float val = low.getAt(i);
            if (Float.isNaN(val)) {
                return;
            }
            if (val < candidate) {
                return;
            }
        }
        lastSwingLow = candidate;
    }

    @Override
    public float value() {
        return lastSwingHigh;
    }

    public FloatSmartValue getSwingHighOutput() {
        return swingHighOutput;
    }

    public FloatSmartValue getSwingLowOutput() {
        return swingLowOutput;
    }

}
