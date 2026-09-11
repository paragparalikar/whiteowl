package com.whiteowl.core.backtest.v2.indicator;

public final class EmaIndicator implements Indicator {

    private final int period;
    private final float multiplier;
    private float emaValue;
    private int count;

    public EmaIndicator(int period) {
        this.period = period;
        this.multiplier = 2f / (period + 1);
        this.emaValue = Float.NaN;
        this.count = 0;
    }

    @Override
    public void update(float newValue) {
        count++;
        if (count == 1) {
            emaValue = newValue;
            return;
        }
        emaValue = (newValue - emaValue) * multiplier + emaValue;
    }

    @Override
    public float value() {
        if (count < period) {
            return Float.NaN;
        }
        return emaValue;
    }

}
