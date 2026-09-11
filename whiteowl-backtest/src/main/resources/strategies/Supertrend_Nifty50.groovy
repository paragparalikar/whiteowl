import groovy.transform.Field

@Field def st
@Field def dir
@Field int period
@Field float mult
@Field int qty = 50

void setup() {
    period = input("Period", 10, 3, 50, 1) as int
    mult = input("Multiplier", 3.0, 1.0, 6.0, 0.5) as float
    def result = supertrend(period, mult)
    st = result[0]
    dir = result[1]
}

void onBar(int bar, String scripId) {
    if (bar < period + 2) return

    float currentDir = dir[0]
    float prevDir = dir[-1]

    // Supertrend turned green (bullish) — go long
    if (currentDir > 0 && prevDir <= 0) {
        if (hasOpenPositions()) shortExit()
        longEntry(qty)
    }
    // Supertrend turned red (bearish) — go short
    else if (currentDir < 0 && prevDir >= 0) {
        if (hasOpenPositions()) longExit()
        shortEntry(qty)
    }
}
