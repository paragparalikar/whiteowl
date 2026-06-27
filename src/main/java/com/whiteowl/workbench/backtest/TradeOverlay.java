package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.Timeframe;

import java.util.List;

public record TradeOverlay(List<TradeRecord> trades, String scripId, Timeframe timeframe) {
}
