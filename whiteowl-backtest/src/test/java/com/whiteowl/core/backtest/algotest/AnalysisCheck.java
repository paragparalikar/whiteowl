package com.whiteowl.core.backtest.algotest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.backtest.algotest.analysis.CostConfig;
import com.whiteowl.core.backtest.algotest.analysis.ParsedTrade;
import com.whiteowl.core.backtest.algotest.analysis.TradeAnalyzer;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.ResultSummary;
import com.whiteowl.core.backtest.algotest.model.enums.Ticker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalysisCheck {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = AlgoTestClient.mapper();
        String file = Files.readString(Path.of("src/main/resources/response-sample.txt"));
        String body = file.substring(file.indexOf("Response:") + 9).trim();
        BacktestResult result = mapper.readValue(body, BacktestResult.class);

        List<ParsedTrade> trades = TradeAnalyzer.parseTrades(result);
        System.out.println("parsed trades: " + trades.size());
        System.out.println("first trade legs: " + trades.get(0).legs());

        // gross profit reconstruction vs server OverallProfit
        double gross = trades.stream().mapToDouble(t -> t.grossProfit(0)).sum();
        System.out.println("gross total (slip=0): " + gross
                + " vs server OverallProfit: " + result.getResults().getSummary().getOverallProfit());

        // our summary vs server summary (no costs)
        ResultSummary s = TradeAnalyzer.summarizeTrades(trades, 0, CostConfig.none(), Ticker.SENSEX, 10);
        ResultSummary server = result.getResults().getSummary();
        System.out.printf("%-32s %15s %15s%n", "metric", "ours", "server");
        cmp("OverallProfit", s.getOverallProfit(), server.getOverallProfit());
        cmp("NumberOfTrades", s.getNumberOfTrades(), server.getNumberOfTrades());
        cmp("WinningRatio", s.getWinningRatio(), server.getWinningRatio());
        cmp("AverageProfitPerTrade", s.getAverageProfitPerTrade(), server.getAverageProfitPerTrade());
        cmp("AvgWin", s.getAverageProfitPerWinningTrade(), server.getAverageProfitPerWinningTrade());
        cmp("AvgLoss", s.getAverageProfitPerLosingTrade(), server.getAverageProfitPerLosingTrade());
        cmp("MaxWin", s.getMaximumProfitInSingleTrade(), server.getMaximumProfitInSingleTrade());
        cmp("MaxLoss", s.getMinimumProfitInSingleTrade(), server.getMinimumProfitInSingleTrade());
        cmp("MaxDD", s.getMaximumDrawdown(), server.getMaximumDrawdown());
        cmp("WinStreak", s.getMaximumWinningStreak(), server.getMaximumWinningStreak());
        cmp("LoseStreak", s.getMaximumLosingStreak(), server.getMaximumLosingStreak());
        cmp("Expectancy", s.getExpectancy(), server.getExpectancy());
        cmp("R2R", s.getRewardToRiskRatio(), server.getRewardToRiskRatio());
        cmp("RDD", s.getReturnOverMaximumDrawdown(), server.getReturnOverMaximumDrawdown());

        // DTE distribution sanity
        Map<Integer, Integer> dayIndex = TradeAnalyzer.tradingDayIndex(result.getResults().getTradingDays());
        Map<Integer, Integer> dteCounts = new java.util.TreeMap<>();
        for (ParsedTrade t : trades) {
            Integer d = TradeAnalyzer.tradingDaysToExpiry(t, dayIndex);
            dteCounts.merge(d == null ? -1 : d, 1, Integer::sum);
        }
        System.out.println("DTE distribution (-1=none): " + dteCounts);

        // 0DTE summary with costs
        List<ParsedTrade> dte0 = TradeAnalyzer.filterByDte(trades, dayIndex, Set.of(0));
        ResultSummary s0 = TradeAnalyzer.summarizeTrades(dte0, 0.005,
                CostConfig.taxesPlusPerOrder(20), Ticker.SENSEX, 10);
        System.out.println("0DTE (slip .5%, taxes+Rs20/order): trades=" + s0.getNumberOfTrades()
                + " profit=" + s0.getOverallProfit() + " maxDD=" + s0.getMaximumDrawdown()
                + " sortino=" + s0.getSortinoRatio());

        // full analyze() path — all + per-DTE, with costs
        Map<String, ResultSummary> analyzed = TradeAnalyzer.analyze(result, Ticker.SENSEX, 0.005,
                CostConfig.taxesPlusPerOrder(20), 10,
                List.of(Set.of(), Set.of(0), Set.of(0, 1)), true);
        for (var e : analyzed.entrySet()) {
            ResultSummary m = e.getValue();
            System.out.printf("DTE %-4s trades=%-4s profit=%-12s sortino=%-6s RDD=%s%n",
                    e.getKey(), m.getNumberOfTrades(), m.getOverallProfit(),
                    m.getSortinoRatio(), m.getReturnOverMaximumDrawdown());
        }

        // margin positions from latest trade
        System.out.println("margin positions: "
                + TradeAnalyzer.marginPositions(result, "SENSEX"));
    }

    static void cmp(String name, Object ours, Object server) {
        System.out.printf("%-32s %15s %15s%n", name, ours, server);
    }
}
