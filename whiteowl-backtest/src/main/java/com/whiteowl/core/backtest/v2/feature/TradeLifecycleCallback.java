package com.whiteowl.core.backtest.v2.feature;

public interface TradeLifecycleCallback {

    void onTradeEvent(TradeLifecycleEvent event);

}
