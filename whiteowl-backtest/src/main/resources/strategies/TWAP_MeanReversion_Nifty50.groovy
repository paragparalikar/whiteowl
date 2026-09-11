// TWAP Mean Reversion with Supertrend Trend Filter
// Rules:
//   - When price deviates significantly from intraday TWAP, fade toward TWAP
//   - Supertrend filter ensures we only fade WITH the higher-TF trend:
//       * Supertrend bullish + price below TWAP by threshold → long (buy the dip)
//       * Supertrend bearish + price above TWAP by threshold → short (sell the rally)
//   - Reversal candle confirmation: bullish bar for longs, bearish bar for shorts
//   - Target: TWAP level
//   - Stop: beyond the extreme (entry ± ATR-based buffer)
//   - Active window: 11:00 AM to 2:00 PM IST (midday mean-reversion zone)

import groovy.transform.Field
import java.time.*

@Field def stDir
@Field def twapLine
@Field def atrLine
@Field int stPeriod
@Field float stMult
@Field int atrPeriod
@Field float deviationPct
@Field float stopAtrMult
@Field int qty

@Field long dayEpoch = Long.MIN_VALUE
@Field int tradeDir = 0
@Field float stopPrice = 0
@Field int tradesThisSession = 0
@Field int maxTradesPerDay

static final ZoneId IST = ZoneId.of("Asia/Kolkata")

void setup() {
    stPeriod = input("ST Period", 10, 3, 50, 1) as int
    stMult = input("ST Multiplier", 3.0, 1.0, 6.0, 0.5) as float
    atrPeriod = input("ATR Period", 14, 5, 30, 1) as int
    deviationPct = input("Deviation %", 0.3, 0.1, 1.0, 0.05) as float
    stopAtrMult = input("Stop ATR Mult", 1.5, 0.5, 3.0, 0.5) as float
    qty = input("Quantity", 50, 1, 500, 1) as int
    maxTradesPerDay = input("Max Trades/Day", 2, 1, 5, 1) as int

    def stResult = supertrend(stPeriod, stMult)
    stDir = stResult[1]
    twapLine = twap()
    atrLine = atr(atrPeriod)
}

void onBar(int bar, String scripId) {
    if (bar < Math.max(stPeriod, atrPeriod) + 2) return

    long ts = timestamp[0]
    def dt = Instant.ofEpochMilli(ts).atZone(IST)
    long dateEp = dt.toLocalDate().toEpochDay()
    int minuteOfDay = dt.hour * 60 + dt.minute

    // --- New day detection ---
    if (dateEp != dayEpoch) {
        if (hasOpenPositions()) {
            if (tradeDir > 0) longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            else if (tradeDir < 0) shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
        }
        dayEpoch = dateEp
        tradeDir = 0; stopPrice = 0
        tradesThisSession = 0
    }

    float c = close[0]
    float h = high[0]
    float l = low[0]
    float o = open[0]
    float twapVal = twapLine[0]
    float atrVal = atrLine[0]
    float stDirection = stDir[0]

    if (Float.isNaN(twapVal) || Float.isNaN(atrVal) || twapVal <= 0) return

    float deviation = (c - twapVal) / twapVal * 100f

    // --- Time exit at 14:30 ---
    if (minuteOfDay >= 14 * 60 + 30) {
        if (hasOpenPositions()) {
            if (tradeDir > 0) longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            else if (tradeDir < 0) shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            tradeDir = 0
        }
        return
    }

    // --- Manage open position ---
    if (hasOpenPositions() && tradeDir != 0) {
        if (tradeDir > 0) {
            // Target: price reverts to TWAP
            if (c >= twapVal) {
                longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
                return
            }
            // Stop loss
            if (l <= stopPrice) {
                longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
                return
            }
        } else {
            // Target: price reverts to TWAP
            if (c <= twapVal) {
                shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
                return
            }
            // Stop loss
            if (h >= stopPrice) {
                shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
                return
            }
        }
        return
    }

    // --- Entry logic: only during midday window 11:00 - 14:00 ---
    if (minuteOfDay < 11 * 60 || minuteOfDay >= 14 * 60) return
    if (tradesThisSession >= maxTradesPerDay) return

    boolean bullishBar = c > o
    boolean bearishBar = c < o

    // Long mean reversion: Supertrend bullish + price below TWAP by threshold + bullish reversal bar
    if (stDirection > 0 && deviation < -deviationPct && bullishBar) {
        stopPrice = c - stopAtrMult * atrVal
        tradeDir = 1
        tradesThisSession++
        longEntry(qty, FillTiming.CURRENT_BAR, FillPrice.CLOSE)
    }
    // Short mean reversion: Supertrend bearish + price above TWAP by threshold + bearish reversal bar
    else if (stDirection < 0 && deviation > deviationPct && bearishBar) {
        stopPrice = c + stopAtrMult * atrVal
        tradeDir = -1
        tradesThisSession++
        shortEntry(qty, FillTiming.CURRENT_BAR, FillPrice.CLOSE)
    }
}
