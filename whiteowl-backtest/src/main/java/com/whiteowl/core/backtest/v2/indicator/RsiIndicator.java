package com.whiteowl.core.backtest.v2.indicator;

public final class RsiIndicator implements Indicator {

    private final int period;
    private float avgGain;
    private float avgLoss;
    private float prevValue;
    private int count;

    public RsiIndicator(int period) {
        this.period = period;
        this.avgGain = 0f;
        this.avgLoss = 0f;
        this.prevValue = Float.NaN;
        this.count = 0;
    }

    @Override
    public void update(float newValue) {
        count++;
        if (count == 1) {
            prevValue = newValue;
            return;
        }
        float change = newValue - prevValue;
        prevValue = newValue;
        float gain = Math.max(change, 0f);
        float loss = Math.max(-change, 0f);
        if (count <= period + 1) {
            avgGain += gain / period;
            avgLoss += loss / period;
        } else {
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }
    }

    @Override
    public float value() {
        if (count <= period) {
            return Float.NaN;
        }
        if (avgLoss == 0f) {
            return 100f;
        }
        float rs = avgGain / avgLoss;
        return 100f - (100f / (1f + rs));
    }

}
