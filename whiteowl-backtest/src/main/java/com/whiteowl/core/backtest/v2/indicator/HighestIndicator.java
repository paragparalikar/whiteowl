package com.whiteowl.core.backtest.v2.indicator;

public final class HighestIndicator implements Indicator {

    private final int period;
    private final float[] window;
    private int index;
    private int count;

    public HighestIndicator(int period) {
        this.period = period;
        this.window = new float[period];
        this.index = 0;
        this.count = 0;
    }

    @Override
    public void update(float newValue) {
        window[index] = newValue;
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
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < period; i++) {
            if (window[i] > max) {
                max = window[i];
            }
        }
        return max;
    }

}
