package com.whiteowl.core.backtest.v2.smartvalue;

import com.whiteowl.core.backtest.v2.indicator.DerivedIndicator;

public final class ScalarDerivedFloatSmartValue extends FloatSmartValue implements DerivedIndicator {

    private final FloatSmartValue source;
    private final float scalar;
    private final ArithmeticOp op;

    ScalarDerivedFloatSmartValue(int capacity, FloatSmartValue source, float scalar, ArithmeticOp op) {
        super(capacity);
        this.source = source;
        this.scalar = scalar;
        this.op = op;
    }

    @Override
    public void recompute() {
        float result = op.apply(source.value(), scalar);
        push(result);
    }

    public FloatSmartValue getSource() {
        return source;
    }

}
