package com.whiteowl.core.backtest.algotest.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed row of {@code results.trade_wise_results}.
 *
 * Wire format per trade:
 * [entryYmd, entryTimeSecs, underlyingAtEntry, exitYmd, exitTimeSecs, underlyingAtExit, [legs]]
 *
 * Wire format per leg:
 * [entryPremium, exitPremium, qty, side(+1 buy / -1 sell), entryYmd, entryTimeSecs,
 *  exitYmd, exitTimeSecs, "CE"|"PE"|"FUT"|..., strike, expiryYmd]
 */
public record ParsedTrade(
        int entryYmd, int entryTimeSecs, double underlyingAtEntry,
        int exitYmd, int exitTimeSecs, double underlyingAtExit,
        List<Leg> legs) {

    public record Leg(
            double entryPremium, double exitPremium, double qty, int side,
            int entryYmd, int entryTimeSecs, int exitYmd, int exitTimeSecs,
            String optionType, double strike, Integer expiryYmd) {
    }

    @SuppressWarnings("unchecked")
    public static ParsedTrade parse(List<Object> row) {
        List<Object> rawLegs = (List<Object>) row.get(6);
        List<Leg> legs = new ArrayList<>(rawLegs.size());
        for (Object rawLeg : rawLegs) {
            List<Object> l = (List<Object>) rawLeg;
            legs.add(new Leg(
                    num(l.get(0)), num(l.get(1)), num(l.get(2)), (int) num(l.get(3)),
                    (int) num(l.get(4)), (int) num(l.get(5)), (int) num(l.get(6)), (int) num(l.get(7)),
                    String.valueOf(l.get(8)), num(l.get(9)),
                    l.size() > 10 && l.get(10) != null ? (int) num(l.get(10)) : null));
        }
        return new ParsedTrade(
                (int) num(row.get(0)), (int) num(row.get(1)), num(row.get(2)),
                (int) num(row.get(3)), (int) num(row.get(4)), num(row.get(5)),
                legs);
    }

    /** Earliest (nearest) leg expiry, ignoring legs without one (cash/futures). */
    public Integer nearestExpiry() {
        Integer nearest = null;
        for (Leg leg : legs) {
            if (leg.expiryYmd() != null && (nearest == null || leg.expiryYmd() < nearest)) {
                nearest = leg.expiryYmd();
            }
        }
        return nearest;
    }

    /** Gross trade profit with slippage applied on both sides of every leg. */
    public double grossProfit(double slippageFraction) {
        double profit = 0;
        for (Leg leg : legs) {
            profit += leg.qty() * ((leg.exitPremium() - leg.entryPremium()) * leg.side()
                    - slippageFraction * (leg.entryPremium() + leg.exitPremium()));
        }
        return profit;
    }

    /** Slippage-adjusted entry premium of a leg (worse fill). */
    public static double adjustedEntry(Leg leg, double slippageFraction) {
        return leg.entryPremium() * (1 + leg.side() * slippageFraction);
    }

    /** Slippage-adjusted exit premium of a leg (worse fill). */
    public static double adjustedExit(Leg leg, double slippageFraction) {
        return leg.exitPremium() * (1 - leg.side() * slippageFraction);
    }

    private static double num(Object o) {
        return ((Number) o).doubleValue();
    }
}
