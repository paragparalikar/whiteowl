import groovy.transform.Field

/**
 * SMA rebound (LONG only).
 *
 * Entry : close above the SMA AND the SMA was touched — i.e. current bar's
 *         low or the previous bar's low dipped below the SMA while price
 *         closed back above it.
 * Exit  : none in-strategy — engine-side StandardExitPolicy
 *         (stop/target/trailing/time-stop, optimized in Phase 4).
 *
 * Entry signals fill next-bar open (engine default).
 */

@Field def ma
@Field def longSma
@Field int smaPeriod
@Field int filterSmaPeriod
@Field int qty

void setup() {
    smaPeriod = input("smaPeriod", 10, 5, 50, 5) as int
    filterSmaPeriod = input("filterSmaPeriod", 50, 50, 500, 50) as int
    qty       = input("quantity", 100) as int
    ma = sma(close, smaPeriod)
    longSma = sma(close, filterSmaPeriod)
}

void onBar(int bar, String scripId) {
    if (bar < filterSmaPeriod + 2) return

    boolean closeAbove = close[0] > ma[0]
    boolean dipped = low[0] < ma[0] || low[1] < ma[1]
    boolean isUpTrend = ma[0] > longSma[0]

    if (!hasOpenPositions() && closeAbove && dipped && isUpTrend) {
        longEntry(qty)
    }
}
