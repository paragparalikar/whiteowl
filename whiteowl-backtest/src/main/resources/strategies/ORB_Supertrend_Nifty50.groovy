// Opening Range Breakout (ORB) Strategy
// Optimizable parameters:
//   1. ORB end time      — minutes after 9:15 to define the opening range
//   2. Entry type        — 1 = breakout (intrabar breach), 2 = candle close
//   3. Entry candle TF   — aggregation period in minutes (applies to close-type)
//   4. Exit time         — minutes after 9:15 to force-close any open position
//   5. Stop multiplier   — stop distance = stopMult × ORB range width
//   6. Target multiplier — target distance = targetMult × stop distance (= R:R)
// One trade per session. Fill at next bar's open.

import groovy.transform.Field
import java.time.*

@Field int orbEndMins
@Field int entryType
@Field int entryCandleMins
@Field int exitMins
@Field float stopMult
@Field float targetMult
@Field int qty

@Field long dayEpoch = Long.MIN_VALUE
@Field float orbHigh
@Field float orbLow
@Field float rangeWidth = 0
@Field boolean orbReady = false
@Field boolean tradedToday = false
@Field float stopPrice = 0
@Field float targetPrice = 0
@Field int tradeDir = 0
@Field boolean pendingFill = false
@Field int pendingDir = 0

// Candle aggregation state (for close-type entries)
@Field int aggSlot = -1
@Field float aggHigh
@Field float aggLow
@Field float aggClose

@Field final ZoneId IST = ZoneId.of("Asia/Kolkata")
@Field final int MKT_OPEN = 9 * 60 + 15

void setup() {
    orbEndMins = input("ORB End (mins)", 30, 5, 120, 5) as int
    entryType = input("Entry Type (1=BO 2=Close)", 2, 1, 2, 1) as int
    entryCandleMins = input("Entry Candle TF (mins)", 5, 1, 30, 1) as int
    exitMins = input("Exit Time (mins)", 315, 240, 375, 15) as int
    stopMult = input("Stop (x range)", 1.0, 0.25, 3.0, 0.25) as float
    targetMult = input("Target (x stop)", 2.0, 0.5, 5.0, 0.5) as float
    qty = input("Quantity", 50, 1, 500, 1) as int
}

void onBar(int bar, String scripId) {
    if (bar < 1) return

    long ts = timestamp[0]
    def dt = Instant.ofEpochMilli(ts).atZone(IST)
    long dateEp = dt.toLocalDate().toEpochDay()
    int minsAfterOpen = dt.hour * 60 + dt.minute - MKT_OPEN

    float h = high[0]
    float l = low[0]
    float c = close[0]
    float o = open[0]

    // --- New day ---
    if (dateEp != dayEpoch) {
        if (hasOpenPositions()) {
            if (tradeDir > 0) longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            else if (tradeDir < 0) shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
        }
        dayEpoch = dateEp
        orbHigh = Float.MIN_VALUE; orbLow = Float.MAX_VALUE
        rangeWidth = 0; orbReady = false; tradedToday = false
        stopPrice = 0; targetPrice = 0; tradeDir = 0
        pendingFill = false; pendingDir = 0
        aggSlot = -1
    }

    // --- Pending fill: position just opened at this bar's open ---
    if (pendingFill) {
        if (hasOpenPositions()) {
            float fillPrice = o
            float stopDist = stopMult * rangeWidth
            tradeDir = pendingDir
            if (tradeDir > 0) {
                stopPrice = fillPrice - stopDist
                targetPrice = fillPrice + targetMult * stopDist
            } else {
                stopPrice = fillPrice + stopDist
                targetPrice = fillPrice - targetMult * stopDist
            }
        }
        pendingFill = false; pendingDir = 0
        // fall through to manage the position on this bar
    }

    // --- Build opening range ---
    if (minsAfterOpen < orbEndMins) {
        if (h > orbHigh) orbHigh = h
        if (l < orbLow) orbLow = l
        return
    }
    if (!orbReady) {
        orbReady = true
        rangeWidth = orbHigh - orbLow
        if (orbHigh == Float.MIN_VALUE || orbLow == Float.MAX_VALUE || rangeWidth <= 0) return
    }

    // --- Time exit ---
    if (minsAfterOpen >= exitMins) {
        if (hasOpenPositions()) {
            if (tradeDir > 0) longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            else if (tradeDir < 0) shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
            tradeDir = 0
        }
        return
    }

    // --- Manage open position: stop / target ---
    if (hasOpenPositions() && tradeDir != 0) {
        if (tradeDir > 0) {
            if (l <= stopPrice || h >= targetPrice) {
                longExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0; tradedToday = true
            }
        } else {
            if (h >= stopPrice || l <= targetPrice) {
                shortExit(FillTiming.CURRENT_BAR, FillPrice.CLOSE)
                tradeDir = 0; tradedToday = true
            }
        }
        return
    }

    // --- Entry logic: one trade per day ---
    if (tradedToday || tradeDir != 0) return

    // === Type 1: Breakout (intrabar price breach) ===
    if (entryType == 1) {
        if (h > orbHigh) {
            pendingFill = true; pendingDir = 1
            longEntry(qty)
        } else if (l < orbLow) {
            pendingFill = true; pendingDir = -1
            shortEntry(qty)
        }
        return
    }

    // === Type 2: Candle close (aggregated candle closes outside ORB) ===
    int slot = (int)(minsAfterOpen / entryCandleMins)
    if (slot != aggSlot) {
        // Previous aggregated candle just completed — check for breakout
        if (aggSlot >= 0 && aggSlot * entryCandleMins >= orbEndMins) {
            if (aggClose > orbHigh) {
                pendingFill = true; pendingDir = 1
                longEntry(qty)
            } else if (aggClose < orbLow) {
                pendingFill = true; pendingDir = -1
                shortEntry(qty)
            }
        }
        // Start new aggregated candle
        aggSlot = slot
        aggHigh = h; aggLow = l; aggClose = c
    } else {
        // Continue building current candle
        if (h > aggHigh) aggHigh = h
        if (l < aggLow) aggLow = l
        aggClose = c
    }
}
