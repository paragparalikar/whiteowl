package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;

public final class VolumeSmaIndicator implements DerivedIndicator {

    private final LongSmartValue volumeSource;
    private final FloatSmartValue output;
    private final int period;
    private final long[] window;
    private int index;
    private int count;
    private long sum;

    public VolumeSmaIndicator(LongSmartValue volumeSource, FloatSmartValue output, int period) {
        this.volumeSource = volumeSource;
        this.output = output;
        this.period = period;
        this.window = new long[period];
        this.index = 0;
        this.count = 0;
        this.sum = 0L;
    }

    @Override
    public void recompute() {
        long vol = volumeSource.value();
        if (count >= period) {
            sum -= window[index];
        }
        window[index] = vol;
        sum += vol;
        index = (index + 1) % period;
        if (count < period) {
            count++;
        }
        float result = count < period ? Float.NaN : (float) sum / period;
        output.push(result);
    }

    @Override
    public float value() {
        return output.value();
    }

}
