package com.whiteowl.core.backtest.engine;

import com.whiteowl.core.backtest.dsl.Signal;
import com.whiteowl.core.bar.model.BarsArrays;

import java.util.List;

public record ScripData(BarsArrays arrays, List<Signal> signals) {

}
