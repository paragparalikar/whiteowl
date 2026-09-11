package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class AdxIndicator implements DerivedIndicator {

    private final FloatSmartValue high;
    private final FloatSmartValue low;
    private final FloatSmartValue close;
    private final FloatSmartValue plusDiOutput;
    private final FloatSmartValue minusDiOutput;
    private final int period;
    private float smoothedPlusDm;
    private float smoothedMinusDm;
    private float smoothedTr;
    private float adxSum;
    private float adxValue;
    private int count;

    public AdxIndicator(FloatSmartValue high, FloatSmartValue low, FloatSmartValue close,
                        int period, FloatSmartValue plusDiOutput, FloatSmartValue minusDiOutput) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.period = period;
        this.plusDiOutput = plusDiOutput;
        this.minusDiOutput = minusDiOutput;
        this.smoothedPlusDm = 0f;
        this.smoothedMinusDm = 0f;
        this.smoothedTr = 0f;
        this.adxSum = 0f;
        this.adxValue = Float.NaN;
        this.count = 0;
    }

    @Override
    public void recompute() {
        count++;
        float tr = computeTrueRange();
        float plusDm = computePlusDm();
        float minusDm = computeMinusDm();
        if (count <= period) {
            smoothedTr += tr;
            smoothedPlusDm += plusDm;
            smoothedMinusDm += minusDm;
            pushDiOutputs(Float.NaN, Float.NaN);
        } else {
            smoothedTr = smoothedTr - (smoothedTr / period) + tr;
            smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / period) + plusDm;
            smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / period) + minusDm;
            float plusDi = computeDi(smoothedPlusDm, smoothedTr);
            float minusDi = computeDi(smoothedMinusDm, smoothedTr);
            pushDiOutputs(plusDi, minusDi);
            float dx = computeDx(plusDi, minusDi);
            computeAdx(dx);
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

    private float computePlusDm() {
        if (count == 1) {
            return 0f;
        }
        float upMove = high.value() - high.getAt(-1);
        float downMove = low.getAt(-1) - low.value();
        return (upMove > downMove && upMove > 0f) ? upMove : 0f;
    }

    private float computeMinusDm() {
        if (count == 1) {
            return 0f;
        }
        float upMove = high.value() - high.getAt(-1);
        float downMove = low.getAt(-1) - low.value();
        return (downMove > upMove && downMove > 0f) ? downMove : 0f;
    }

    private static final float HUNDRED = 100f;

    private float computeDi(float smoothedDm, float smoothedTrValue) {
        return smoothedTrValue == 0f ? 0f : (smoothedDm / smoothedTrValue) * HUNDRED;
    }

    private float computeDx(float plusDi, float minusDi) {
        float sum = plusDi + minusDi;
        return sum == 0f ? 0f : (Math.abs(plusDi - minusDi) / sum) * HUNDRED;
    }

    private void computeAdx(float dx) {
        int adxStart = period * 2;
        if (count <= adxStart) {
            adxSum += dx;
            if (count == adxStart) {
                adxValue = adxSum / period;
            }
        } else {
            adxValue = (adxValue * (period - 1) + dx) / period;
        }
    }

    private void pushDiOutputs(float plusDi, float minusDi) {
        plusDiOutput.push(plusDi);
        minusDiOutput.push(minusDi);
    }

    @Override
    public float value() {
        return adxValue;
    }

    public FloatSmartValue getPlusDiOutput() {
        return plusDiOutput;
    }

    public FloatSmartValue getMinusDiOutput() {
        return minusDiOutput;
    }

}
