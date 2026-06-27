package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class BollingerBandsIndicator implements Indicator {

    private final int period;
    private final float stdDevMult;
    private final float[] window;
    private final FloatSmartValue upperOutput;
    private final FloatSmartValue lowerOutput;
    private int index;
    private int count;
    private float sum;
    private float middle;
    private float upper;
    private float lower;

    public BollingerBandsIndicator(int period, float stdDevMult,
                                   FloatSmartValue upperOutput, FloatSmartValue lowerOutput) {
        this.period = period;
        this.stdDevMult = stdDevMult;
        this.window = new float[period];
        this.upperOutput = upperOutput;
        this.lowerOutput = lowerOutput;
        this.index = 0;
        this.count = 0;
        this.sum = 0f;
        this.middle = Float.NaN;
        this.upper = Float.NaN;
        this.lower = Float.NaN;
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
        if (count >= period) {
            middle = sum / period;
            float variance = computeVariance();
            float stdDev = (float) Math.sqrt(variance);
            upper = middle + stdDevMult * stdDev;
            lower = middle - stdDevMult * stdDev;
        }
        upperOutput.push(upper);
        lowerOutput.push(lower);
    }

    private float computeVariance() {
        float sumSq = 0f;
        for (int i = 0; i < period; i++) {
            float diff = window[i] - middle;
            sumSq += diff * diff;
        }
        return sumSq / period;
    }

    @Override
    public float value() {
        return middle;
    }

    public FloatSmartValue getUpperOutput() {
        return upperOutput;
    }

    public FloatSmartValue getLowerOutput() {
        return lowerOutput;
    }

}
