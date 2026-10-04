package com.whiteowl.core.backtest.algotest.analysis;

import com.whiteowl.core.backtest.algotest.model.enums.Ticker;

import java.util.Map;
import java.util.Set;

/**
 * Statutory charge rate tables (per crore of turnover) extracted from the
 * AlgoTest web bundle — the same math the site applies to backtest results
 * when "Taxes and Charges" is enabled.
 *
 * All rates are multiplied by turnover-in-crores per order side.
 */
public final class IndianChargeTables {

    public enum Segment { INDEX, STOCK }
    public enum Exchange { NSE, BSE }

    private IndianChargeTables() {
    }

    /** SEBI charges per crore: INDEX 10, STOCK 20. */
    private static final Map<Segment, Double> SEBI = Map.of(
            Segment.INDEX, 10.0, Segment.STOCK, 20.0);

    private static final double CLEARING = 0.0;
    private static final double GST_RATE = 0.18;   // on Exchange + SEBI

    /** IPFT per crore. */
    private static final Map<Segment, Map<String, Double>> IPFT = Map.of(
            Segment.INDEX, Map.of("FUT", 10.0, "CE", 50.0, "PE", 50.0),
            Segment.STOCK, Map.of("FUT", 20.0, "CE", 100.0, "PE", 100.0, "CASH", 10.0));

    /** Stamp duty per crore — buy side only (key 1); sell side is 0. */
    private static final Map<String, Double> STAMP_DUTY_BUY = Map.of(
            "FUT", 200.0, "CE", 300.0, "PE", 300.0, "CASH", 300.0);

    /** STT per crore — sell side only; buy side is 0. */
    private static final Map<String, Double> STT_SELL = Map.of(
            "FUT", 5000.0, "CE", 15000.0, "PE", 15000.0, "CASH", 2500.0);

    /** Exchange transaction charges per crore. */
    private static final Map<Segment, Map<Exchange, Map<String, Double>>> EXCHANGE = Map.of(
            Segment.INDEX, Map.of(
                    Exchange.NSE, Map.of("FUT", 173.0, "CE", 3503.0, "PE", 3503.0),
                    Exchange.BSE, Map.of("FUT", 0.0, "CE", 3250.0, "PE", 3250.0)),
            Segment.STOCK, Map.of(
                    Exchange.NSE, Map.of("FUT", 346.0, "CE", 7006.0, "PE", 7006.0, "CASH", 325.0),
                    Exchange.BSE, Map.of("FUT", 346.0, "CE", 7006.0, "PE", 7006.0, "CASH", 325.0)));

    private static final Set<Ticker> BSE_TICKERS = Set.of(Ticker.SENSEX, Ticker.BANKEX);

    /** Per-ticker expiry-regime change dates (BSE/NSE expiry weekday moves, lot changes). */
    private static final Map<Ticker, Integer> REGIME_CHANGE = Map.of(
            Ticker.NIFTY, 20241120,
            Ticker.SENSEX, 20241120,
            Ticker.MIDCPNIFTY, 20241118,
            Ticker.BANKEX, 20241118,
            Ticker.BANKNIFTY, 20241113,
            Ticker.FINNIFTY, 20241119);

    public static Exchange exchangeOf(Ticker ticker) {
        return BSE_TICKERS.contains(ticker) ? Exchange.BSE : Exchange.NSE;
    }

    public static Segment segmentOf(Ticker ticker) {
        return ticker.name().startsWith("NSE_") ? Segment.STOCK : Segment.INDEX;
    }

    /** First date of the current expiry regime for a ticker; null if unknown. */
    public static Integer regimeChangeDate(Ticker ticker) {
        return REGIME_CHANGE.getOrDefault(ticker, REGIME_CHANGE.get(Ticker.NIFTY));
    }

    /**
     * Total statutory charges for one order side, replicating the site's formula:
     * {@code Total = (IPFT + SEBI + GST_sebi + Clearing + Exchange + GST_exch + Stamp + STT) * turnoverCr}
     *
     * @param optionType    leg instrument string ("CE", "PE", "FUT", "CASH")
     * @param positionSide  +1 for a buy order, -1 for a sell order
     * @param turnoverCrore price * qty * 1e-7
     */
    public static double charges(Exchange exchange, Segment segment, String optionType,
                                 int positionSide, double turnoverCrore) {
        String instrument = normalizeInstrument(optionType);
        double exchangeRate = EXCHANGE.get(segment).get(exchange).getOrDefault(instrument, 0.0);
        double sebiRate = SEBI.get(segment);
        double ipftRate = IPFT.get(segment).getOrDefault(instrument, 0.0);
        double stampRate = positionSide == 1 ? STAMP_DUTY_BUY.getOrDefault(instrument, 0.0) : 0.0;
        double sttRate = positionSide == -1 ? STT_SELL.getOrDefault(instrument, 0.0) : 0.0;
        double gstRate = GST_RATE * (exchangeRate + sebiRate);
        return (ipftRate + sebiRate + gstRate + CLEARING + exchangeRate + stampRate + sttRate)
                * turnoverCrore;
    }

    /** Leg optionType strings already match table keys; FUT_P shares FUT rates. */
    private static String normalizeInstrument(String optionType) {
        return "FUT_P".equals(optionType) ? "FUT" : optionType;
    }
}
