package com.whiteowl.core.backtest.algotest;

import com.whiteowl.core.backtest.algotest.analysis.ParsedTrade;
import com.whiteowl.core.backtest.algotest.analysis.TradeAnalyzer;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.MarginEstimate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MarginCheck {
    public static void main(String[] args) {
        AlgoTestClient client = new AlgoTestClient(AlgoTestOptimizationDriver.COOKIE.strip());

        // fetch one earlier result and reuse its latest strikes; expiry = next weekly expiry
        BacktestResult result = client.getResult("6ac08ebc9a4f41b6f0b47efb");
        List<ParsedTrade> trades = TradeAnalyzer.parseTrades(result);
        ParsedTrade last = trades.get(trades.size() - 1);
        LocalDate lastExpiry = LocalDate.parse(String.valueOf(last.nearestExpiry()),
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate expiryDate = lastExpiry.plusWeeks(1);           // next weekly expiry
        String expiry = expiryDate.format(
                DateTimeFormatter.ofPattern("dd-MMM-yy", java.util.Locale.ENGLISH));
        System.out.println("using expiry " + expiry);

        List<MarginEstimate.Position> positions = new ArrayList<>();
        for (ParsedTrade.Leg leg : last.legs()) {
            positions.add(new MarginEstimate.Position(
                    "SENSEX", expiry, leg.strike(), leg.optionType(),
                    (int) (leg.side() * leg.qty())));
            System.out.println(leg.optionType() + " strike=" + leg.strike() + " expiry=" + expiry
                    + " netQty=" + (leg.side() * leg.qty()));
        }

        var normal = client.estimateMargin(positions, false);
        System.out.println("Normal margin:      span=" + normal.getFinalSpan()
                + " exposure=" + normal.getFinalExposure() + " total=" + normal.margin());
        var expiryDay = client.estimateMargin(positions, true);
        System.out.println("Expiry-day margin:  span=" + expiryDay.getFinalSpan()
                + " exposure=" + expiryDay.getFinalExposure() + " total=" + expiryDay.margin());
    }
}
