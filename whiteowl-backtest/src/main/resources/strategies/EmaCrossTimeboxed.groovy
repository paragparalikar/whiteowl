import groovy.transform.Field
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * EMA crossover with session time gates (LONG only).
 *
 * Entry : bar time >= 09:30 IST AND shortEma crosses above longEma
 * Exit  : shortEma crosses below longEma OR bar time >= 15:15 IST
 *
 * Entry signals fill next-bar open (engine default).
 */

@Field def fast
@Field def slow
@Field int fastN
@Field int slowN
@Field int qty
@Field ZoneId ist
@Field LocalTime entryFrom
@Field LocalTime exitFrom

void setup() {
    fastN = input("shortEma", 9, 3, 21, 3) as int
    slowN = input("longEma", 21, 21, 84, 7) as int
    qty   = input("quantity", 50) as int
    ist = ZoneId.of("Asia/Kolkata")
    entryFrom = LocalTime.of(9, 30)
    exitFrom = LocalTime.of(15, 15)
    fast = ema(close, fastN)
    slow = ema(close, slowN)
}

void onBar(int bar, String scripId) {
    if (bar < slowN + 2) return

    LocalTime tod = Instant.ofEpochMilli(timestamp[0]).atZone(ist).toLocalTime()
    boolean entryGate = !tod.isBefore(entryFrom)
    boolean timeExit  = !tod.isBefore(exitFrom)

    if (hasOpenPositions() && (timeExit || crossunder(fast, slow))) {
        longExit()
        return
    }
    if (!hasOpenPositions() && entryGate && !timeExit && crossover(fast, slow)) {
        longEntry(qty)
    }
}
