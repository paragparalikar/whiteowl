package com.whiteowl.core.backtest.v2.engine;

import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.scrip.model.Scrip;

public interface BacktestListener {

    void onProgress(int completed, int total);

    void onScripCompleted(ScripResult result);

    void onError(Scrip scrip, String error);

}
