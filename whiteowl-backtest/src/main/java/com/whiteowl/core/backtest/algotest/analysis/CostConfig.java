package com.whiteowl.core.backtest.algotest.analysis;

/**
 * Cost configuration mirroring the AlgoTest "Slippage, Brokerage & Taxes"
 * panel: taxes toggle, brokerage toggle + flat value charged per order or per
 * lot (always +18% GST on brokerage).
 */
public record CostConfig(boolean taxesEnabled, boolean brokerageEnabled,
                         double brokerageValue, BrokerageType brokerageType) {

    public enum BrokerageType {
        PER_ORDER("BrokerageType.PerOrder"), PER_LOT("BrokerageType.PerLot");

        private final String apiValue;

        BrokerageType(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }
    }

    /** No costs — raw strategy profit. */
    public static CostConfig none() {
        return new CostConfig(false, false, 0, BrokerageType.PER_ORDER);
    }

    public static CostConfig taxesOnly() {
        return new CostConfig(true, false, 0, BrokerageType.PER_ORDER);
    }

    /** AlgoTest default preset: taxes + flat fee per order (GST added). */
    public static CostConfig taxesPlusPerOrder(double perOrder) {
        return new CostConfig(true, true, perOrder, BrokerageType.PER_ORDER);
    }

    public static CostConfig taxesPlusPerLot(double perLot) {
        return new CostConfig(true, true, perLot, BrokerageType.PER_LOT);
    }
}
