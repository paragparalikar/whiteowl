package com.whiteowl.core.order.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderSide {

    BUY(1) {
        @Override
        public OrderSide complement() {
            return SELL;
        }
    },

    SELL(-1) {
        @Override
        public OrderSide complement() {
            return BUY;
        }
    };

    private final int multiplier;

    public abstract OrderSide complement();

}
