package com.whiteowl.core.backtest.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public final class BacktestResult {

    private final String scripId;
    private final List<TradeRecord> trades;
    private final EquityCurve equityCurve;

}
