package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;

/**
 * Relative Volume indicator for the backtest engine.
 *
 * RVOL = current volume / SMA(volume, period)
 *
 * Uses a circular buffer for O(1) incremental SMA updates, identical
 * to {@link VolumeSmaIndicator} but divides the current volume by the
 * average instead of emitting the average itself.
 */
public final class RvolIndicator implements DerivedIndicator {

    private final LongSmartValue volumeSource;
    private final FloatSmartValue output;
    private final int period;
    private final long[] window;
    private int index;
    private int count;
    private long sum;

    public RvolIndicator(LongSmartValue volumeSource, FloatSmartValue output, int period) {
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
        if (count < period || sum <= 0) {
            output.push(Float.NaN);
        } else {
            float avg = (float) sum / period;
            output.push(vol / avg);
        }
    }

    @Override
    public float value() {
        return output.value();
    }
}
