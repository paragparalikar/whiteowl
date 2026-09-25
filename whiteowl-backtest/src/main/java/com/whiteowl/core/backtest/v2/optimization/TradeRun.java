package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.model.EquityCurve;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.backtest.v2.optimization.analysis.TradeObservation;
import com.whiteowl.core.backtest.v2.optimization.exit.Excursion;

import java.util.List;

/**
 * Detailed evaluation output: aggregate metrics plus the underlying data
 * needed by analysis phases — merged trades, merged equity curve, per-trade
 * MAE/MFE excursions, and feature observations at entry.
 */
public record TradeRun(OptimizationMetrics metrics,
                       List<TradeRecord> trades,
                       EquityCurve equityCurve,
                       List<Excursion> excursions,
                       List<TradeObservation> observations) {
}
