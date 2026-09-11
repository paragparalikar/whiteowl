package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Intraday TWAP (Time-Weighted Average Price) indicator.
 * Computes the running average of typical price ((H+L+C)/3) since market open,
 * with equal weighting per bar. Resets at the start of each trading day (IST).
 */
public final class TwapIndicator implements DerivedIndicator {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final FloatSmartValue high;
    private final FloatSmartValue low;
    private final FloatSmartValue close;
    private final LongSmartValue timestamp;
    private final FloatSmartValue twapOutput;

    private double cumTp;
    private int barCount;
    private long currentDateEpoch;
    private float twapValue;

    public TwapIndicator(FloatSmartValue high, FloatSmartValue low, FloatSmartValue close,
                         LongSmartValue timestamp, FloatSmartValue twapOutput) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.timestamp = timestamp;
        this.twapOutput = twapOutput;
        this.twapValue = Float.NaN;
        this.currentDateEpoch = Long.MIN_VALUE;
    }

    @Override
    public void recompute() {
        long ts = timestamp.value();
        long dateEpoch = Instant.ofEpochMilli(ts).atZone(IST).toLocalDate().toEpochDay();

        if (dateEpoch != currentDateEpoch) {
            currentDateEpoch = dateEpoch;
            cumTp = 0;
            barCount = 0;
        }

        float tp = (high.value() + low.value() + close.value()) / 3f;
        cumTp += tp;
        barCount++;
        twapValue = (float) (cumTp / barCount);
        twapOutput.push(twapValue);
    }

    @Override
    public float value() {
        return twapValue;
    }

    public FloatSmartValue getTwapOutput() {
        return twapOutput;
    }

}
