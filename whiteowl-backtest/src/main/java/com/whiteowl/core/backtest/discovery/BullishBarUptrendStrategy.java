package com.whiteowl.core.backtest.discovery;

import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Java port of {@code Bullish bar in uptrend.afl} — an IBS long-only momentum
 * strategy translated from Pine Script v6.
 *
 * <p>Entry (long only, filled at the signal bar's close):
 * <ul>
 *   <li>IBS = (close - low) / (high - low), or 0 on a zero-range bar;
 *       {@code ibs > ibsThreshold}</li>
 *   <li>{@code close >= high[i-1]} (bullish bar making a 1-bar high)</li>
 *   <li>{@code close > EMA(close, emaLength)} (uptrend filter)</li>
 * </ul>
 *
 * <p>Exit (filled at the bar's close): a trailing stop ratchets up each bar as
 * {@code max(prevStop, close - ATR(atrLength) * atrMultiplier)}; the position
 * closes when {@code close < stop}. The stop is initialised on the entry bar
 * and can never move down. A new entry on the exit bar is not allowed (same
 * as the AFL if/else structure). A position still open on the last bar is
 * closed at that bar's close, matching AmiBroker's end-of-backtest behaviour.
 */
public final class BullishBarUptrendStrategy {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final int emaLength;
    private final float ibsThreshold;
    private final int atrLength;
    private final float atrMultiplier;
    private final float costPointsPerSide;

    /** AFL defaults: EMA 130, IBS 0.66, ATR 50 x 1.0, ~120 Rs/side at 100 Rs/pt. */
    public BullishBarUptrendStrategy() {
        this(130, 0.66f, 50, 1.0f, 1.2f);
    }

    public BullishBarUptrendStrategy(int emaLength, float ibsThreshold,
                                     int atrLength, float atrMultiplier,
                                     float costPointsPerSide) {
        this.emaLength = emaLength;
        this.ibsThreshold = ibsThreshold;
        this.atrLength = atrLength;
        this.atrMultiplier = atrMultiplier;
        this.costPointsPerSide = costPointsPerSide;
    }

    public List<DiscoveryTrade> run(BarsArrays bars) {
        int n = bars.size();
        float[] h = bars.high();
        float[] l = bars.low();
        float[] c = bars.close();
        long[] ts = bars.timestamp();

        float[] ema = IndicatorFunctions.ema(c, n, emaLength);
        float[] atr = IndicatorFunctions.atr(h, l, c, n, atrLength);

        List<DiscoveryTrade> trades = new ArrayList<>();
        boolean inPosition = false;
        float trail = Float.NaN;
        int entryBar = -1;
        float entryPrice = 0f, mfe = 0f, mae = 0f;
        int entryMinute = 0, dow = 0;

        for (int i = 1; i < n; i++) {
            if (inPosition) {
                mfe = Math.max(mfe, h[i] - entryPrice);
                mae = Math.min(mae, l[i] - entryPrice);
                float possibleStop = c[i] - atr[i] * atrMultiplier;
                trail = Float.isNaN(trail) ? possibleStop : Math.max(possibleStop, trail);
                if (c[i] < trail) {
                    trades.add(toTrade(entryBar, i, entryPrice, c[i], ts,
                            atr[entryBar], entryMinute, dow, mfe, mae));
                    inPosition = false;
                    trail = Float.NaN;
                }
            } else {
                float range = h[i] - l[i];
                float ibs = range > 0 ? (c[i] - l[i]) / range : 0f;
                if (ibs > ibsThreshold && c[i] >= h[i - 1] && c[i] > ema[i]) {
                    inPosition = true;
                    entryBar = i;
                    entryPrice = c[i];
                    mfe = 0f;
                    mae = 0f;
                    ZonedDateTime z = Instant.ofEpochMilli(ts[i]).atZone(IST);
                    entryMinute = z.getHour() * 60 + z.getMinute();
                    dow = z.getDayOfWeek().getValue();
                    trail = c[i] - atr[i] * atrMultiplier;
                }
            }
        }
        if (inPosition) {
            int last = n - 1;
            trades.add(toTrade(entryBar, last, entryPrice, c[last], ts,
                    atr[entryBar], entryMinute, dow, mfe, mae));
        }
        return trades;
    }

    private DiscoveryTrade toTrade(int entryBar, int exitBar, float entryPrice,
                                   float exitPrice, long[] ts, float atrAtEntry,
                                   int entryMinute, int dow, float mfe, float mae) {
        float gross = exitPrice - entryPrice;
        float cost = 2 * costPointsPerSide;
        return new DiscoveryTrade(
                entryBar, entryBar, exitBar, +1,
                ts[entryBar], ts[exitBar],
                entryPrice, exitPrice,
                gross, cost, gross - cost,
                mfe, mae, atrAtEntry,
                exitBar - entryBar, entryMinute, dow);
    }

}
