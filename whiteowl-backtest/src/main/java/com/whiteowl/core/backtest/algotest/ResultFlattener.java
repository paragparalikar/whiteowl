package com.whiteowl.core.backtest.algotest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.backtest.algotest.model.AlgoStrategy;
import com.whiteowl.core.backtest.algotest.model.BacktestRequest;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.IndicatorNode;
import com.whiteowl.core.backtest.algotest.model.LegConfig;
import com.whiteowl.core.backtest.algotest.model.ResultSummary;
import com.whiteowl.core.backtest.algotest.model.TypedValue;
import com.whiteowl.core.backtest.algotest.model.enums.AlgoTestEnum;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flattens one backtest run into a single CSV row: every strategy input
 * variable followed by every summary metric and the result file path.
 */
public final class ResultFlattener {

    private static final ObjectMapper MAPPER = AlgoTestClient.mapper();

    private ResultFlattener() {
    }

    public static LinkedHashMap<String, String> flatten(BacktestRequest request,
                                                        BacktestResult result,
                                                        String error) {
        LinkedHashMap<String, String> row = inputColumns(request);

        row.put("BacktestId", result != null ? result.getId() : "");
        row.put("Status", result != null ? str(result.getStatus()) : "ERROR");
        row.put("RunTs", result != null ? str(result.getRunTs()) : "");

        ResultSummary s = result != null && result.getResults() != null
                ? result.getResults().getSummary() : null;
        putSummary(row, s);
        row.put("SortinoRatio", s != null ? str(s.getSortinoRatio()) : "");
        row.put("ResultFilePath", result != null && result.getResults() != null
                ? str(result.getResults().getResultFilePath()) : "");
        row.put("Error", error != null ? error : "");
        return row;
    }

    /**
     * Row for one DTE subset of a backtest: all input vars, the analysis context
     * (slippage/costs/DTE), net-of-costs summary metrics, expiry-day margin and
     * margin-normalized metrics, plus the raw server profit for comparison.
     */
    public static LinkedHashMap<String, String> flattenAnalyzed(BacktestRequest request,
                                                              BacktestResult result,
                                                              String dteLabel,
                                                              ResultSummary summary,
                                                              Double margin,
                                                              double slippagePct,
                                                              String costDesc,
                                                              String error) {
        return flattenAnalyzed(inputColumns(request), result, dteLabel, summary, margin,
                slippagePct, costDesc, error);
    }

    /** Same as above but inputs come from a prior CSV row (used by DteAnalysisDriver). */
    public static LinkedHashMap<String, String> flattenAnalyzed(Map<String, String> inputs,
                                                              BacktestResult result,
                                                              String dteLabel,
                                                              ResultSummary summary,
                                                              Double margin,
                                                              double slippagePct,
                                                              String costDesc,
                                                              String error) {
        LinkedHashMap<String, String> row = new LinkedHashMap<>(inputs);

        row.put("BacktestId", result != null ? result.getId() : "");
        row.put("Status", result != null ? str(result.getStatus()) : "ERROR");
        row.put("RunTs", result != null ? str(result.getRunTs()) : "");
        row.put("DteSet", dteLabel);
        row.put("SlippagePct", str(slippagePct));
        row.put("Cost", costDesc);
        putSummary(row, summary);
        row.put("SortinoRatio", summary != null ? str(summary.getSortinoRatio()) : "");
        row.put("MarginExpiryDay", margin != null ? str(Math.round(margin)) : "");
        if (margin != null && margin > 0 && summary != null
                && summary.getOverallProfit() != null && summary.getMaximumDrawdown() != null) {
            row.put("OverallProfitPctOfMargin",
                    str(Math.round(10000.0 * summary.getOverallProfit() / margin) / 100.0));
            row.put("MaxDrawdownPctOfMargin",
                    str(Math.round(10000.0 * summary.getMaximumDrawdown() / margin) / 100.0));
            row.put("AnnualReturnOnMargin",
                    str(Math.round(10000.0 * summary.getReturnOverMaximumDrawdown()
                            * -summary.getMaximumDrawdown() / margin) / 100.0));
        } else {
            row.put("OverallProfitPctOfMargin", "");
            row.put("MaxDrawdownPctOfMargin", "");
            row.put("AnnualReturnOnMargin", "");
        }
        ResultSummary server = result != null && result.getResults() != null
                ? result.getResults().getSummary() : null;
        row.put("ServerOverallProfit", server != null ? str(server.getOverallProfit()) : "");
        row.put("ResultFilePath", result != null && result.getResults() != null
                ? str(result.getResults().getResultFilePath()) : "");
        row.put("Error", error != null ? error : "");
        return row;
    }

    private static void putSummary(LinkedHashMap<String, String> row, ResultSummary s) {
        row.put("OverallProfit", s != null ? str(s.getOverallProfit()) : "");
        row.put("NumberOfTrades", s != null ? str(s.getNumberOfTrades()) : "");
        row.put("WinningRatio", s != null ? str(s.getWinningRatio()) : "");
        row.put("LosingRatio", s != null ? str(s.getLosingRatio()) : "");
        row.put("AverageProfitPerTrade", s != null ? str(s.getAverageProfitPerTrade()) : "");
        row.put("AverageProfitPerWinningTrade", s != null ? str(s.getAverageProfitPerWinningTrade()) : "");
        row.put("AverageProfitPerLosingTrade", s != null ? str(s.getAverageProfitPerLosingTrade()) : "");
        row.put("MaximumProfitInSingleTrade", s != null ? str(s.getMaximumProfitInSingleTrade()) : "");
        row.put("MinimumProfitInSingleTrade", s != null ? str(s.getMinimumProfitInSingleTrade()) : "");
        row.put("MaximumDrawdown", s != null ? str(s.getMaximumDrawdown()) : "");
        row.put("MaximumWinningStreak", s != null ? str(s.getMaximumWinningStreak()) : "");
        row.put("MaximumLosingStreak", s != null ? str(s.getMaximumLosingStreak()) : "");
        row.put("Expectancy", s != null ? str(s.getExpectancy()) : "");
        row.put("RewardToRiskRatio", s != null ? str(s.getRewardToRiskRatio()) : "");
        row.put("ReturnOverMaximumDrawdown", s != null ? str(s.getReturnOverMaximumDrawdown()) : "");
    }

    private static LinkedHashMap<String, String> inputColumns(BacktestRequest request) {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        AlgoStrategy strategy = request.getStrategy();

        row.put("Ticker", str(strategy.getTicker()));
        row.put("StartDate", request.getStartDate());
        row.put("EndDate", request.getEndDate());
        row.put("EntryTime", timeOf(strategy.getEntryIndicators()));
        row.put("ExitTime", timeOf(strategy.getExitIndicators()));
        row.put("StrategyType", str(strategy.getStrategyType()));
        row.put("MaxPositionInADay", str(strategy.getMaxPositionInADay()));
        row.put("ReentryTimeRestriction", str(strategy.getReentryTimeRestriction()));
        row.put("SkipInitialCandles", str(strategy.getSkipInitialCandles()));
        row.put("StopMonitoringTime", str(strategy.getStopMonitoringTime()));
        row.put("TakeUnderlyingFromCashOrNot", strategy.getTakeUnderlyingFromCashOrNot());
        row.put("TrailSLtoBreakeven", strategy.getTrailSLtoBreakeven());
        row.put("SquareOffAllLegs", strategy.getSquareOffAllLegs());
        row.put("WeeklyOldRegime", str(strategy.isWeeklyOldRegime()));
        putTyped(row, "OverallSL", strategy.getOverallSL());
        putTyped(row, "OverallTgt", strategy.getOverallTgt());
        putTyped(row, "OverallTrailSL", strategy.getOverallTrailSL());
        putTyped(row, "LockAndTrail", strategy.getLockAndTrail());
        putTyped(row, "OverallReentrySL", strategy.getOverallReentrySL());
        putTyped(row, "OverallReentryTgt", strategy.getOverallReentryTgt());
        putTyped(row, "OverallMomentum", strategy.getOverallMomentum());

        int legNo = 1;
        for (LegConfig leg : strategy.getListOfLegConfigs()) {
            String prefix = "Leg" + legNo++ + "_";
            row.put(prefix + "PositionType", str(leg.getPositionType()));
            row.put(prefix + "InstrumentKind", str(leg.getInstrumentKind()));
            row.put(prefix + "ExpiryKind", str(leg.getExpiryKind()));
            row.put(prefix + "EntryType", str(leg.getEntryType()));
            row.put(prefix + "StrikeParameter", str(leg.getStrikeParameter()));
            putTyped(row, prefix + "LotConfig", leg.getLotConfig());
            putTyped(row, prefix + "StopLoss", leg.getLegStopLoss());
            putTyped(row, prefix + "Target", leg.getLegTarget());
            putTyped(row, prefix + "TrailSL", leg.getLegTrailSL());
            putTyped(row, prefix + "Momentum", leg.getLegMomentum());
            putTyped(row, prefix + "ReentrySL", leg.getLegReentrySL());
            putTyped(row, prefix + "ReentryTP", leg.getLegReentryTP());
        }
        return row;
    }

    /** Extracts "HH:MM" from an indicator tree rooted at a single time indicator. */
    private static String timeOf(IndicatorNode node) {
        if (node == null) {
            return "";
        }
        JsonNode root = MAPPER.valueToTree(node);
        JsonNode params = root.path("Value").path(0).path("Value").path("Parameters");
        if (params.has("Hour")) {
            return String.format("%d:%02d", params.path("Hour").asInt(), params.path("Minute").asInt());
        }
        return str(node.getValue());
    }

    /**
     * Splits a TypedValue into two columns: {@code <name>Type} = the enum type
     * ("None" when unset), {@code <name>} = the scalar value as a pure number,
     * or a compact {k=v,...} rendering for object-shaped values.
     */
    private static void putTyped(LinkedHashMap<String, String> row, String name, TypedValue tv) {
        if (tv == null || tv.isNone()) {
            row.put(name + "Type", "None");
            row.put(name, "");
            return;
        }
        row.put(name + "Type", tv.getType());
        JsonNode value = MAPPER.valueToTree(tv.getValue());
        if (value.isObject()) {
            if (value.isEmpty()) {
                row.put(name, "");
                return;
            }
            StringBuilder sb = new StringBuilder("{");
            value.properties().forEach(e -> sb.append(e.getKey()).append('=')
                    .append(e.getValue().asText()).append(','));
            sb.setCharAt(sb.length() - 1, '}');
            row.put(name, sb.toString());
        } else {
            row.put(name, value.asText());
        }
    }

    private static String str(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof AlgoTestEnum e) {
            return e.getApiValue();
        }
        return String.valueOf(o);
    }
}
