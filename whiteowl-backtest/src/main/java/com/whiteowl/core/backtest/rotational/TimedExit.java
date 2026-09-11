package com.whiteowl.core.backtest.rotational;

import com.whiteowl.core.bar.model.Bars;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Exit rule: force-close the position at a specific time of day.
 *
 * <p>If the configured exit time is reached and the position is still open
 * (no stop or target hit), the position is closed at the bar's close price.</p>
 *
 * <p>This is distinct from {@link MarketCloseExit} — it allows exiting
 * before market close (e.g. at 14:30 IST to avoid late-day volatility).</p>
 */
public final class TimedExit implements ExitRule {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final LocalTime exitTime;

    /**
     * @param exitTime time of day (IST) at which to close the position
     */
    public TimedExit(LocalTime exitTime) {
        this.exitTime = exitTime;
    }

    @Override
    public ExitSignal check(Bars bars, int barIdx, ExitContext ctx) {
        LocalTime barTime = Instant.ofEpochMilli(bars.getTimestamp(barIdx))
                .atZone(IST).toLocalTime();
        if (!barTime.isBefore(exitTime)) {
            return new ExitSignal(bars.getClose(barIdx), ExitReason.TIME_EXIT);
        }
        return null;
    }

    @Override
    public String toString() {
        return "TimedExit(" + exitTime + ")";
    }
}
