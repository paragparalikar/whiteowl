package com.whiteowl.core.backtest.v2.smartvalue;

public enum ArithmeticOp {

    ADD {
        @Override
        public float apply(float left, float right) {
            return left + right;
        }
    },
    SUBTRACT {
        @Override
        public float apply(float left, float right) {
            return left - right;
        }
    },
    MULTIPLY {
        @Override
        public float apply(float left, float right) {
            return left * right;
        }
    },
    DIVIDE {
        @Override
        public float apply(float left, float right) {
            return left / right;
        }
    };

    public abstract float apply(float left, float right);

}
