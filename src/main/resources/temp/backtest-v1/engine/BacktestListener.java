package com.whiteowl.core.backtest.engine;

import com.whiteowl.core.backtest.model.BacktestResult;
import com.whiteowl.core.scrip.model.Scrip;

public interface BacktestListener {

    void onProgress(int completed, int total);

    void onScripCompleted(BacktestResult result);

    void onError(Scrip scrip, String error);

}
