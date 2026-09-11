package com.whiteowl.core.backtest.v2.indicator;

public final class SmaIndicator implements Indicator {

    private final int period;
    private final float[] window;
    private int index;
    private int count;
    private float sum;

    public SmaIndicator(int period) {
        this.period = period;
        this.window = new float[period];
        this.index = 0;
        this.count = 0;
        this.sum = 0f;
    }

    @Override
    public void update(float newValue) {
        if (count >= period) {
            sum -= window[index];
        }
        window[index] = newValue;
        sum += newValue;
        index = (index + 1) % period;
        if (count < period) {
            count++;
        }
    }

    @Override
    public float value() {
        if (count < period) {
            return Float.NaN;
        }
        return sum / period;
    }

}
