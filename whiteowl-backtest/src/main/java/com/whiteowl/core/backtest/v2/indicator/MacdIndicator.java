package com.whiteowl.core.backtest.v2.indicator;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;

public final class MacdIndicator implements Indicator {

    private final EmaIndicator fastEma;
    private final EmaIndicator slowEma;
    private final EmaIndicator signalEma;
    private final FloatSmartValue signalOutput;
    private final FloatSmartValue histogramOutput;
    private float macdLine;
    private float signalLine;
    private float histogram;
    private final int slowPeriod;
    private int count;

    public MacdIndicator(int fastPeriod, int slowPeriod, int signalPeriod,
                         FloatSmartValue signalOutput, FloatSmartValue histogramOutput) {
        this.fastEma = new EmaIndicator(fastPeriod);
        this.slowEma = new EmaIndicator(slowPeriod);
        this.signalEma = new EmaIndicator(signalPeriod);
        this.signalOutput = signalOutput;
        this.histogramOutput = histogramOutput;
        this.slowPeriod = slowPeriod;
        this.macdLine = Float.NaN;
        this.signalLine = Float.NaN;
        this.histogram = Float.NaN;
        this.count = 0;
    }

    @Override
    public void update(float newValue) {
        count++;
        fastEma.update(newValue);
        slowEma.update(newValue);
        if (count >= slowPeriod) {
            macdLine = fastEma.value() - slowEma.value();
            if (!Float.isNaN(macdLine)) {
                signalEma.update(macdLine);
                signalLine = signalEma.value();
                histogram = Float.isNaN(signalLine) ? Float.NaN : macdLine - signalLine;
            }
        } else {
            macdLine = Float.NaN;
        }
        signalOutput.push(signalLine);
        histogramOutput.push(histogram);
    }

    @Override
    public float value() {
        return macdLine;
    }

    public FloatSmartValue getSignalOutput() {
        return signalOutput;
    }

    public FloatSmartValue getHistogramOutput() {
        return histogramOutput;
    }

}
