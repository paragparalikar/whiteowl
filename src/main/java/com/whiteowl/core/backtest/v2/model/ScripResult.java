package com.whiteowl.core.backtest.v2.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public final class ScripResult {

    private final String scripId;
    private final List<TradeRecord> trades;
    private final EquityCurve equityCurve;
    private final float totalNetPnl;

}
