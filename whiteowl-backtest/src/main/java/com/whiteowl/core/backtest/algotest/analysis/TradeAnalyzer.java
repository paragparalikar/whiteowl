package com.whiteowl.core.backtest.algotest.analysis;

import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.MarginEstimate;
import com.whiteowl.core.backtest.algotest.model.ResultSummary;
import com.whiteowl.core.backtest.algotest.model.enums.Ticker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replicates the AlgoTest front-end's post-processing of backtest results:
 * slippage, statutory charges, brokerage and the summary metrics — including
 * the "Trading Days to Expiry" (DTE) trade filter — so that 0DTE/1DTE
 * performance, net of costs, can be derived from a single backtest's
 * trade_wise_results without re-running it.
 */
public final class TradeAnalyzer {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TradeAnalyzer() {
    }

    public static List<ParsedTrade> parseTrades(BacktestResult result) {
        List<ParsedTrade> trades = new ArrayList<>();
        if (result.getResults() != null && result.getResults().getTradeWiseResults() != null) {
            for (List<Object> row : result.getResults().getTradeWiseResults()) {
                trades.add(ParsedTrade.parse(row));
            }
        }
        return trades;
    }

    /**
     * Trading days to expiry for a trade: index of the nearest leg expiry minus
     * index of the trade entry day in the {@code trading_days} list — exactly
     * the site's DTE filter (0 = entered on expiry day). Returns null when the
     * trade has no expiring legs or either date is absent from the list.
     */
    public static Integer tradingDaysToExpiry(ParsedTrade trade, Map<Integer, Integer> dayIndex) {
        Integer expiry = trade.nearestExpiry();
        if (expiry == null) {
            return null; // site keeps such trades
        }
        Integer expiryIdx = dayIndex.get(expiry);
        Integer entryIdx = dayIndex.get(trade.entryYmd());
        if (expiryIdx == null || entryIdx == null) {
            return null;
        }
        return expiryIdx - entryIdx;
    }

    public static Map<Integer, Integer> tradingDayIndex(List<Long> tradingDays) {
        Map<Integer, Integer> index = new HashMap<>(tradingDays.size() * 2);
        for (int i = 0; i < tradingDays.size(); i++) {
            index.put(tradingDays.get(i).intValue(), i);
        }
        return index;
    }

    /** Trades whose DTE is in {@code dteSet}; trades without an expiry pass through. */
    public static List<ParsedTrade> filterByDte(List<ParsedTrade> trades,
                                                Map<Integer, Integer> dayIndex,
                                                Set<Integer> dteSet) {
        List<ParsedTrade> filtered = new ArrayList<>(trades.size());
        for (ParsedTrade trade : trades) {
            Integer dte = tradingDaysToExpiry(trade, dayIndex);
            if (dte == null || dteSet.contains(dte)) {
                filtered.add(trade);
            }
        }
        return filtered;
    }

    /** Drops trades entered before the ticker's expiry-regime change date
     *  (SENSEX: 2024-11-20 etc.) — the site's includeFromPreviousRegime toggle. */
    public static List<ParsedTrade> filterByRegime(List<ParsedTrade> trades, Ticker ticker,
                                                   boolean includeFromPreviousRegime) {
        if (includeFromPreviousRegime) {
            return trades;
        }
        Integer regimeStart = IndianChargeTables.regimeChangeDate(ticker);
        if (regimeStart == null) {
            return trades;
        }
        List<ParsedTrade> filtered = new ArrayList<>(trades.size());
        for (ParsedTrade trade : trades) {
            if (trade.entryYmd() >= regimeStart) {
                filtered.add(trade);
            }
        }
        return filtered;
    }

    /**
     * Net profit of one trade after slippage, taxes and brokerage — mirrors the
     * site's per-trade calculation, floored to whole paise.
     */
    public static double netProfit(ParsedTrade trade, double slippageFraction,
                                   CostConfig cost, Ticker ticker, double lotSize) {
        double profit = round2(trade.grossProfit(slippageFraction));
        if (cost.taxesEnabled()) {
            var exchange = IndianChargeTables.exchangeOf(ticker);
            var segment = IndianChargeTables.segmentOf(ticker);
            for (ParsedTrade.Leg leg : trade.legs()) {
                double entryTurnover = ParsedTrade.adjustedEntry(leg, slippageFraction)
                        * leg.qty() * 1e-7;
                double exitTurnover = ParsedTrade.adjustedExit(leg, slippageFraction)
                        * leg.qty() * 1e-7;
                profit -= IndianChargeTables.charges(exchange, segment, leg.optionType(),
                        leg.side(), entryTurnover);
                profit -= IndianChargeTables.charges(exchange, segment, leg.optionType(),
                        -leg.side(), exitTurnover);
            }
        }
        if (cost.brokerageEnabled()) {
            double brokerage;
            if (cost.brokerageType() == CostConfig.BrokerageType.PER_ORDER) {
                brokerage = cost.brokerageValue() * trade.legs().size() * 2;
            } else {
                double lots = 0;
                for (ParsedTrade.Leg leg : trade.legs()) {
                    lots += "CASH".equals(leg.optionType()) ? leg.qty() : leg.qty() / lotSize;
                }
                brokerage = cost.brokerageValue() * lots * 2;
            }
            profit -= 1.18 * brokerage; // brokerage + 18% GST
        }
        return Math.floor(100 * profit) / 100;
    }

    /**
     * Recomputes the strategy summary from net per-trade profits — same math
     * the site runs for its metrics panel.
     */
    public static ResultSummary summarize(List<Double> netProfits, long firstEntryYmd,
                                          long lastExitYmd) {
        ResultSummary s = new ResultSummary();
        int n = netProfits.size();
        if (n == 0) {
            s.setNumberOfTrades(0);
            return s;
        }
        double total = 0, winSum = 0, lossSum = 0, max = Double.NEGATIVE_INFINITY,
                min = Double.POSITIVE_INFINITY;
        int wins = 0, losses = 0;
        for (double p : netProfits) {
            total += p;
            max = Math.max(max, p);
            min = Math.min(min, p);
            if (p > 0) {
                winSum += p;
                wins++;
            } else {
                lossSum += p;
                losses++;
            }
        }
        double winFrac = (double) wins / n;
        double avgWin = wins > 0 ? winSum / wins : 0;
        double avgLoss = losses > 0 ? lossSum / losses : 0;
        double rewardToRisk = avgLoss < 0 ? avgWin / -avgLoss : 0;

        double[] drawdown = maxDrawdown(netProfits);
        int maxWinStreak = maxStreak(netProfits, true);
        int maxLoseStreak = maxStreak(netProfits, false);
        long days = daysBetween(firstEntryYmd, lastExitYmd) + 1;

        s.setOverallProfit(round5(total));
        s.setNumberOfTrades(n);
        s.setAverageProfitPerTrade(round2(total / n));
        s.setWinningRatio(round2(100 * winFrac));
        s.setLosingRatio(round2(100 * (1 - winFrac)));
        s.setAverageProfitPerWinningTrade(round2(avgWin));
        s.setAverageProfitPerLosingTrade(round2(avgLoss));
        s.setMaximumProfitInSingleTrade(round5(max));
        s.setMinimumProfitInSingleTrade(round5(min));
        s.setMaximumDrawdown(round5(drawdown[0]));
        s.setMaximumWinningStreak(maxWinStreak);
        s.setMaximumLosingStreak(maxLoseStreak);
        s.setRewardToRiskRatio(round2(rewardToRisk));
        s.setExpectancy(round2(rewardToRisk * winFrac - (1 - winFrac)));
        s.setReturnOverMaximumDrawdown(drawdown[0] < 0
                ? round2(total / (days / 365.0) / -drawdown[0]) : 0.0);
        s.setSortinoRatio(sortinoRatio(netProfits, days));
        return s;
    }

    /**
     * Annualized Sortino ratio on daily net P&L (each trade is one day):
     * mean / downsideDeviation(0) * sqrt(trading-days-per-year).
     * Scale-free — identical whether computed on ₹ P&L or margin-normalized returns.
     */
    public static double sortinoRatio(List<Double> dailyNetPnl, long spanDays) {
        int n = dailyNetPnl.size();
        if (n < 2 || spanDays <= 0) {
            return 0;
        }
        double mean = dailyNetPnl.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSq = 0;
        int neg = 0;
        for (double p : dailyNetPnl) {
            if (p < 0) {
                sumSq += p * p;
                neg++;
            }
        }
        if (neg == 0) {
            return 0;
        }
        double downsideDev = Math.sqrt(sumSq / neg);
        double daysPerYear = n * 365.0 / spanDays;
        return round2(downsideDev > 0 ? mean / downsideDev * Math.sqrt(daysPerYear) : 0);
    }

    public static ResultSummary summarizeTrades(List<ParsedTrade> trades, double slippageFraction,
                                                CostConfig cost, Ticker ticker, double lotSize) {
        List<Double> profits = new ArrayList<>(trades.size());
        long firstEntry = Long.MAX_VALUE, lastExit = Long.MIN_VALUE;
        for (ParsedTrade trade : trades) {
            profits.add(netProfit(trade, slippageFraction, cost, ticker, lotSize));
            firstEntry = Math.min(firstEntry, trade.entryYmd());
            lastExit = Math.max(lastExit, trade.exitYmd());
        }
        return summarize(profits, firstEntry, lastExit);
    }

    /**
     * Runs the full local analysis for one backtest result: regime filter, then
     * per DTE set a net-of-costs summary (incl. Sortino). Key "all" = unfiltered.
     *
     * @param dteSets sets like {@code Set.of(0)} for 0DTE, {@code Set.of(0,1)} for 0+1 DTE;
     *                an empty set means "all days" (no DTE filter)
     * @return ordered map DTE-label -> summary; label "all" for the unfiltered set
     */
    public static Map<String, ResultSummary> analyze(BacktestResult result, Ticker ticker,
                                                     double slippageFraction, CostConfig cost,
                                                     double lotSize, List<Set<Integer>> dteSets,
                                                     boolean includePreviousRegime) {
        List<ParsedTrade> trades = filterByRegime(parseTrades(result), ticker, includePreviousRegime);
        Map<Integer, Integer> dayIndex = result.getResults() != null
                && result.getResults().getTradingDays() != null
                ? tradingDayIndex(result.getResults().getTradingDays()) : Map.of();

        Map<String, ResultSummary> out = new LinkedHashMap<>();
        for (Set<Integer> dteSet : dteSets) {
            String label = dteSet.isEmpty() ? "all"
                    : dteSet.stream().sorted().map(String::valueOf)
                            .reduce((a, b) -> a + "+" + b).orElse("all");
            List<ParsedTrade> subset = dteSet.isEmpty() ? trades
                    : filterByDte(trades, dayIndex, dteSet);
            out.put(label, summarizeTrades(subset, slippageFraction, cost, ticker, lotSize));
        }
        return out;
    }

    /**
     * Builds margin positions from the latest trade's legs, rolled forward to the
     * next weekly expiry — the same shape the site's margin call uses.
     * Returns empty list when the strategy has no expiring legs.
     */
    public static List<MarginEstimate.Position> marginPositions(BacktestResult result,
                                                              String ticker) {
        List<ParsedTrade> trades = parseTrades(result);
        if (trades.isEmpty()) {
            return List.of();
        }
        ParsedTrade last = trades.get(trades.size() - 1);
        Integer lastExpiry = last.nearestExpiry();
        if (lastExpiry == null) {
            return List.of();
        }
        LocalDate expiry = LocalDate.parse(String.valueOf(lastExpiry),
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        while (!expiry.isAfter(LocalDate.now())) {
            expiry = expiry.plusWeeks(1);
        }
        String expiryText = expiry.format(
                DateTimeFormatter.ofPattern("dd-MMM-yy", java.util.Locale.ENGLISH));
        List<MarginEstimate.Position> positions = new ArrayList<>(last.legs().size());
        for (ParsedTrade.Leg leg : last.legs()) {
            positions.add(new MarginEstimate.Position(ticker, expiryText, leg.strike(),
                    leg.optionType(), (int) (leg.side() * leg.qty())));
        }
        return positions;
    }

    /** Max drawdown of the cumulative profit curve; [0]=value, [1]=peak idx, [2]=trough idx. */
    private static double[] maxDrawdown(List<Double> profits) {
        double[] cumulative = new double[profits.size()];
        double run = 0;
        for (int i = 0; i < profits.size(); i++) {
            cumulative[i] = (run += profits.get(i));
        }
        double peak = cumulative[0] > 0 ? cumulative[0] : 0;
        double maxDd = Double.POSITIVE_INFINITY;
        int peakIdx = -1, troughIdx = -1;
        for (int i = 0; i < cumulative.length; i++) {
            double dd = cumulative[i] - peak;
            if (dd < maxDd) {
                maxDd = dd;
                troughIdx = i;
            }
            if (cumulative[i] > peak) {
                peak = cumulative[i];
                peakIdx = i;
            }
        }
        if (maxDd >= 0) {
            return new double[]{maxDd, 0, 0};
        }
        return new double[]{maxDd, peakIdx, troughIdx};
    }

    private static int maxStreak(List<Double> profits, boolean wins) {
        int streak = 0, best = 0;
        for (double p : profits) {
            // site counts strictly-positive wins / strictly-negative losses; 0 breaks both streaks
            if (wins ? p > 0 : p < 0) {
                best = Math.max(best, ++streak);
            } else {
                streak = 0;
            }
        }
        return Math.max(best, streak);
    }

    private static long daysBetween(long ymd1, long ymd2) {
        return ChronoUnit.DAYS.between(LocalDate.parse(String.valueOf(ymd1), YMD),
                LocalDate.parse(String.valueOf(ymd2), YMD));
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /** Site's 5-paise floor rounding: floor(20*x)/20. */
    private static double round5(double v) {
        return Math.floor(20 * v) / 20;
    }
}
