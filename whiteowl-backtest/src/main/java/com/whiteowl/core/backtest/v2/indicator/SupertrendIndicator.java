package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class SupertrendIndicator implements DerivedIndicator {

    private final FloatSmartValue high;
    private final FloatSmartValue low;
    private final FloatSmartValue close;
    private final FloatSmartValue supertrendOutput;
    private final FloatSmartValue directionOutput;
    private final int period;
    private final float multiplier;

    private float atrValue;
    private float trSum;
    private int count;

    private float upperBand;
    private float lowerBand;
    private boolean bullish;
    private float supertrendValue;
    private float directionValue;

    public SupertrendIndicator(FloatSmartValue high, FloatSmartValue low, FloatSmartValue close,
                               int period, float multiplier,
                               FloatSmartValue supertrendOutput, FloatSmartValue directionOutput) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.period = period;
        this.multiplier = multiplier;
        this.supertrendOutput = supertrendOutput;
        this.directionOutput = directionOutput;
        this.atrValue = Float.NaN;
        this.supertrendValue = Float.NaN;
        this.directionValue = Float.NaN;
        this.trSum = 0f;
        this.count = 0;
        this.upperBand = Float.NaN;
        this.lowerBand = Float.NaN;
        this.bullish = true;
    }

    @Override
    public void recompute() {
        count++;
        float tr = computeTrueRange();
        computeAtr(tr);
        computeSupertrend();
        supertrendOutput.push(supertrendValue);
        directionOutput.push(directionValue);
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

    private void computeAtr(float tr) {
        if (count <= period) {
            trSum += tr;
            if (count == period) {
                atrValue = trSum / period;
            }
        } else {
            atrValue = (atrValue * (period - 1) + tr) / period;
        }
    }

    private void computeSupertrend() {
        if (count <= period || Float.isNaN(atrValue)) {
            supertrendValue = Float.NaN;
            directionValue = Float.NaN;
            return;
        }
        float h = high.value();
        float l = low.value();
        float c = close.value();
        float mid = (h + l) / 2f;
        float newUpper = mid + multiplier * atrValue;
        float newLower = mid - multiplier * atrValue;
        if (count == period + 1) {
            upperBand = newUpper;
            lowerBand = newLower;
            bullish = c > mid;
        } else {
            float prevClose = close.getAt(-1);
            lowerBand = newLower > lowerBand ? newLower : (prevClose > lowerBand ? lowerBand : newLower);
            upperBand = newUpper < upperBand ? newUpper : (prevClose < upperBand ? upperBand : newUpper);
            if (bullish && c < lowerBand) {
                bullish = false;
            } else if (!bullish && c > upperBand) {
                bullish = true;
            }
        }
        supertrendValue = bullish ? lowerBand : upperBand;
        directionValue = bullish ? 1f : -1f;
    }

    @Override
    public float value() {
        return supertrendValue;
    }

    public FloatSmartValue getDirectionOutput() {
        return directionOutput;
    }

    public FloatSmartValue getSupertrendOutput() {
        return supertrendOutput;
    }

}
