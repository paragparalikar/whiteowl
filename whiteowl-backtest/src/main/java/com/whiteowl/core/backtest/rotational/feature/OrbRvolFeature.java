package com.whiteowl.core.backtest.rotational.feature;

import com.whiteowl.core.bar.model.Bars;

import java.time.*;

/**
 * Trade feature: relative volume (RVOL) during the opening range period.
 *
 * <p>{@code OR_RVOL = OR_volume_today / avg(OR_volume, past N days)}</p>
 *
 * <p>RVOL &gt; 1 means higher-than-average participation during the OR,
 * which may indicate institutional interest and a higher-conviction breakout.
 * RVOL &lt; 1 suggests a quiet open.</p>
 *
 * <p>The lookback period is configurable (default 20 trading days). OR volume
 * is the sum of volume across the first {@code orBarCount} intraday bars of
 * each day.</p>
 */
public final class OrbRvolFeature implements TradeFeature {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final int lookbackDays;

    public OrbRvolFeature() {
        this(20);
    }

    /**
     * @param lookbackDays number of prior trading days to average OR volume over
     */
    public OrbRvolFeature(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    @Override
    public String name() {
        return "OR_RVOL";
    }

    @Override
    public double compute(TradeContext ctx) {
        Bars intradayBars = ctx.intradayBars();
        int dayStart = ctx.intradayDayStart();
        int dayCount = ctx.intradayDayCount();
        int orBarCount = ctx.orBarCount();

        if (intradayBars == null || dayStart < 0 || dayCount < orBarCount || orBarCount <= 0) {
            return Double.NaN;
        }

        // Compute today's OR volume
        long todayOrVol = 0;
        for (int i = dayStart; i < dayStart + orBarCount; i++) {
            todayOrVol += intradayBars.getVolume(i);
        }
        if (todayOrVol <= 0) return Double.NaN;

        // Walk backward through intraday bars to find prior days' OR volumes
        // We need to identify day boundaries by date changes
        long totalPriorVol = 0;
        int priorDaysFound = 0;

        int scanIdx = dayStart - 1;
        while (scanIdx >= 0 && priorDaysFound < lookbackDays) {
            // Find start of this prior day
            LocalDate barDate = Instant.ofEpochMilli(intradayBars.getTimestamp(scanIdx))
                    .atZone(IST).toLocalDate();

            // Find the first bar of this date
            int priorDayStart = scanIdx;
            while (priorDayStart > 0) {
                LocalDate prevDate = Instant.ofEpochMilli(intradayBars.getTimestamp(priorDayStart - 1))
                        .atZone(IST).toLocalDate();
                if (!prevDate.equals(barDate)) break;
                priorDayStart--;
            }

            // Count bars in this prior day
            int priorDayEnd = scanIdx + 1;
            int priorDayCount = priorDayEnd - priorDayStart;

            if (priorDayCount >= orBarCount) {
                long priorOrVol = 0;
                for (int i = priorDayStart; i < priorDayStart + orBarCount; i++) {
                    priorOrVol += intradayBars.getVolume(i);
                }
                if (priorOrVol > 0) {
                    totalPriorVol += priorOrVol;
                    priorDaysFound++;
                }
            }

            // Move to the day before
            scanIdx = priorDayStart - 1;
        }

        if (priorDaysFound == 0) return Double.NaN;

        double avgPriorOrVol = (double) totalPriorVol / priorDaysFound;
        return todayOrVol / avgPriorOrVol;
    }
}
