package com.whiteowl.core.backtest.v2.smartvalue;

import com.whiteowl.core.backtest.v2.indicator.DerivedIndicator;

public final class DerivedFloatSmartValue extends FloatSmartValue implements DerivedIndicator {

    private final FloatSmartValue left;
    private final FloatSmartValue right;
    private final ArithmeticOp op;

    DerivedFloatSmartValue(int capacity, FloatSmartValue left, FloatSmartValue right, ArithmeticOp op) {
        super(capacity);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void recompute() {
        float result = op.apply(left.value(), right.value());
        push(result);
    }

    public FloatSmartValue getLeft() {
        return left;
    }

    public FloatSmartValue getRight() {
        return right;
    }

}
