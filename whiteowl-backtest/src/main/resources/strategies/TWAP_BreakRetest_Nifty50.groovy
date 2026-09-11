// TWAP Break & Retest with Supertrend Trend Filter
// Rules:
//   - Detect when price crosses above/below intraday TWAP decisively
//   - Wait for a pullback (retest) toward TWAP
//   - Enter continuation when TWAP holds as support/resistance
//   - Supertrend filter: only take trades aligned with the prevailing trend
//       * Bullish break & retest: price crosses above TWAP, retests, holds → long (Supertrend bullish)
//       * Bearish break & retest: price crosses below TWAP, retests, holds → short (Supertrend bearish)
//   - Stop: ATR-based below/above TWAP
//   - Target: day's high/low extension (2:1 R:R)
//   - Time exit: 2:30 PM IST

import groovy.transform.Field
import java.time.*

@Field def stDir
@Field def twapLine
@Field def atrLine
@Field int stPeriod
@Field float stMult
@Field int atrPeriod
@Field float retestPct
@Field float rrTarget
@Field float stopAtrMult
@Field int qty

@Field long dayEpoch = Long.MIN_VALUE
@Field int tradeDir = 0
@Field float stopPrice = 0
@Field float targetPrice = 0
@Field int tradesThisSession = 0
@Field int maxTradesPerDay

// State machine for break & retest pattern
// Phase 0: watching for a TWAP cross
// Phase 1: cross detected, waiting for retest (pullback toward TWAP)
// Phase 2: retest occurred, waiting for confirmation (hold and bounce)
@Field int phase = 0
@Field int crossDir = 0       // 1=bullish cross, -1=bearish cross
@Field float dayHigh = Float.MIN_VALUE
@Field float dayLow = Float.MAX_VALUE

static final ZoneId IST = ZoneId.of("Asia/Kolkata")

void setup() {
    stPeriod = input("ST Period", 10, 3, 50, 1) as int
    stMult = input("ST Multiplier", 3.0, 1.0, 6.0, 0.5) as float
    atrPeriod = input("ATR Period", 14, 5, 30, 1) as int
    retestPct = input("Retest Tolerance %", 0.1, 0.05, 0.3, 0.05) as float
    rrTarget = input("RR Target", 2.0, 1.0, 4.0, 0.5) as float
    stopAtrMult = input("Stop ATR Mult", 1.0, 0.5, 3.0, 0.5) as float
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
        tradeDir = 0; stopPrice = 0; targetPrice = 0
        tradesThisSession = 0
        phase = 0; crossDir = 0
        dayHigh = Float.MIN_VALUE; dayLow = Float.MAX_VALUE
    }

    float c = close[0]
    float h = high[0]
    float l = low[0]
    float twapVal = twapLine[0]
    float atrVal = atrLine[0]
    float stDirection = stDir[0]

    if (Float.isNaN(twapVal) || Float.isNaN(atrVal) || twapVal <= 0) return

    // Track intraday high/low
    if (h > dayHigh) dayHigh = h
    if (l < dayLow) dayLow = l

    // Skip first 30 minutes — let TWAP stabilize
    if (minuteOfDay < 9 * 60 + 45) return

    // --- Time exit at 14:30 ---
    if (minuteOfDay >= 14 * 60 + 30) {
        if (hasOpenPositions()) {
            if (tradeDir > 0) longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            else if (tradeDir < 0) shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            tradeDir = 0
        }
        phase = 0
        return
    }

    // --- Manage open position ---
    if (hasOpenPositions() && tradeDir != 0) {
        if (tradeDir > 0) {
            if (l <= stopPrice || h >= targetPrice) {
                longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
            }
        } else {
            if (h >= stopPrice || l <= targetPrice) {
                shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0
            }
        }
        return
    }

    if (tradesThisSession >= maxTradesPerDay) return

    float prevClose = close[-1]
    float prevTwap = twapLine[-1]
    if (Float.isNaN(prevTwap)) return

    float distPct = Math.abs(c - twapVal) / twapVal * 100f
    boolean nearTwap = distPct <= retestPct

    // === State machine ===

    if (phase == 0) {
        // Look for a decisive TWAP cross
        if (prevClose <= prevTwap && c > twapVal) {
            crossDir = 1
            phase = 1
        } else if (prevClose >= prevTwap && c < twapVal) {
            crossDir = -1
            phase = 1
        }
    }
    else if (phase == 1) {
        // Waiting for pullback toward TWAP after the cross
        if (crossDir == 1) {
            if (nearTwap || c <= twapVal) {
                // Price pulled back to TWAP after bullish cross
                phase = 2
            }
            // Invalidate if price goes back through TWAP decisively the wrong way
            if (c < twapVal - atrVal * 0.5f) {
                phase = 0; crossDir = 0
            }
        } else if (crossDir == -1) {
            if (nearTwap || c >= twapVal) {
                // Price pulled back to TWAP after bearish cross
                phase = 2
            }
            if (c > twapVal + atrVal * 0.5f) {
                phase = 0; crossDir = 0
            }
        }
    }
    else if (phase == 2) {
        // Waiting for confirmation: price bounces away from TWAP in original direction
        if (crossDir == 1 && c > twapVal && stDirection > 0) {
            // Bullish break & retest confirmed
            float risk = stopAtrMult * atrVal
            stopPrice = twapVal - risk
            targetPrice = c + rrTarget * risk
            tradeDir = 1
            tradesThisSession++
            phase = 0; crossDir = 0
            longEntry(qty, FillTiming.CURRENT_BAR, FillPrice.CLOSE)
        }
        else if (crossDir == -1 && c < twapVal && stDirection < 0) {
            // Bearish break & retest confirmed
            float risk = stopAtrMult * atrVal
            stopPrice = twapVal + risk
            targetPrice = c - rrTarget * risk
            tradeDir = -1
            tradesThisSession++
            phase = 0; crossDir = 0
            shortEntry(qty, FillTiming.CURRENT_BAR, FillPrice.CLOSE)
        }
        // Invalidate if pattern takes too long or breaks
        else if (crossDir == 1 && c < twapVal - atrVal) {
            phase = 0; crossDir = 0
        }
        else if (crossDir == -1 && c > twapVal + atrVal) {
            phase = 0; crossDir = 0
        }
    }
}
