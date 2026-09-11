package com.whiteowl.core.backtest.v2.smartvalue;

import com.whiteowl.core.backtest.v2.indicator.DerivedIndicator;
import com.whiteowl.core.backtest.v2.indicator.Indicator;

public final class IndicatorNode {

    private final FloatSmartValue output;
    private final FloatSmartValue source;
    private final Indicator indicator;
    private final DerivedIndicator derivedIndicator;
    private final String signature;

    private IndicatorNode(FloatSmartValue output, FloatSmartValue source,
                          Indicator indicator, DerivedIndicator derivedIndicator,
                          String signature) {
        this.output = output;
        this.source = source;
        this.indicator = indicator;
        this.derivedIndicator = derivedIndicator;
        this.signature = signature;
    }

    public static IndicatorNode ofSingleSource(FloatSmartValue output, FloatSmartValue source,
                                               Indicator indicator, String signature) {
        return new IndicatorNode(output, source, indicator, null, signature);
    }

    public static IndicatorNode ofDerived(FloatSmartValue output, DerivedIndicator derivedIndicator,
                                          String signature) {
        return new IndicatorNode(output, null, null, derivedIndicator, signature);
    }

    public void update() {
        if (indicator != null) {
            indicator.update(source.value());
            output.push(indicator.value());
        } else if (derivedIndicator != null) {
            derivedIndicator.recompute();
        }
    }

    public FloatSmartValue getOutput() {
        return output;
    }

    public String getSignature() {
        return signature;
    }

    public Indicator getIndicator() {
        return indicator;
    }

    public DerivedIndicator getDerivedIndicator() {
        return derivedIndicator;
    }

}
