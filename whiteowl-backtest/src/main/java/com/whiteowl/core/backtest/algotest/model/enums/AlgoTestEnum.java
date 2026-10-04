package com.whiteowl.core.backtest.algotest.model.enums;

/**
 * Marker for enums that serialize to the "EnumType.Member" wire format used by
 * the AlgoTest API (e.g. "StrikeType.OTM4").
 */
public interface AlgoTestEnum {
    String getApiValue();
}
