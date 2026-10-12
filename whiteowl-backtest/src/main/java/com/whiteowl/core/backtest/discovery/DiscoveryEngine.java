package com.whiteowl.core.backtest.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a raw signal array into trades.
 *
 * <p>Rules: a signal at bar i fills at bar i+1's open (same day only);
 * signals arriving while a position is open are ignored; no entries after
 * the entry cutoff; the position always closes by the per-day exit bar
 * (last bar starting at or before the configured exit time), or earlier if
 * the configured ATR stop or target is hit.</p>
 *
 * <p>Intrabar exit policy is conservative: when a bar's range contains both
 * the stop and the target, the stop is assumed to have been hit first. Bar
 * opens that gap through a level fill at the open.</p>
 */
public final class DiscoveryEngine {

    private final SignalContext ctx;
    private final int entryCutoffMinute;
    private final int entryStartMinute;
    private final double costPointsPerSide;
    private final Double initialStopAtr;
    private final Double targetAtr;

    public DiscoveryEngine(SignalContext ctx, DiscoveryConfig config) {
        this.ctx = ctx;
        this.entryCutoffMinute = config.entryCutoff().getHour() * 60 + config.entryCutoff().getMinute();
        this.entryStartMinute = config.entryStart().getHour() * 60 + config.entryStart().getMinute();
        this.costPointsPerSide = config.costPointsPerSide();
        this.initialStopAtr = config.initialStopAtr();
        this.targetAtr = config.targetAtr();
    }

    public List<DiscoveryTrade> run(byte[] signals) {
        List<DiscoveryTrade> trades = new ArrayList<>();
        SessionIndex s = ctx.session;
        int n = ctx.size;
        boolean inPosition = false;
        int side = 0, signalBar = 0, entryBar = 0, entryMinute = 0, dow = 0;
        float entryPrice = 0, mfe = 0, mae = 0, stopLevel = 0, targetLevel = 0;

        for (int i = 1; i < n; i++) {
            if (inPosition) {
                float h = ctx.high[i], l = ctx.low[i], o = ctx.open[i];
                if (side > 0) {
                    mfe = Math.max(mfe, h - entryPrice);
                    mae = Math.min(mae, l - entryPrice);
                } else {
                    mfe = Math.max(mfe, entryPrice - l);
                    mae = Math.min(mae, entryPrice - h);
                }
                Float exitPrice = null;
                if (initialStopAtr != null) {
                    boolean hit = side > 0 ? l <= stopLevel : h >= stopLevel;
                    if (hit) {
                        exitPrice = side > 0 ? Math.min(o, stopLevel) : Math.max(o, stopLevel);
                    }
                }
                if (exitPrice == null && targetAtr != null) {
                    boolean hit = side > 0 ? h >= targetLevel : l <= targetLevel;
                    if (hit) {
                        exitPrice = side > 0 ? Math.max(o, targetLevel) : Math.min(o, targetLevel);
                    }
                }
                if (exitPrice == null && i == s.exitBar(i)) {
                    exitPrice = ctx.close[i];
                }
                if (exitPrice != null) {
                    float gross = side * (exitPrice - entryPrice);
                    float cost = (float) (2 * costPointsPerSide);
                    trades.add(new DiscoveryTrade(
                            signalBar, entryBar, i, side,
                            ctx.timestamp[entryBar], ctx.timestamp[i],
                            entryPrice, exitPrice,
                            gross, cost, gross - cost,
                            mfe, mae,
                            ctx.atr(14)[signalBar],
                            i - entryBar, entryMinute, dow));
                    inPosition = false;
                }
            }
            if (!inPosition && signals[i] != 0 && i + 1 < n
                    && s.sameDay(i, i + 1)
                    && s.minuteOfDay(i + 1) >= entryStartMinute
                    && s.minuteOfDay(i + 1) <= entryCutoffMinute
                    && s.minuteOfDay(i + 1) <= s.minuteOfDay(s.exitBar(i + 1))) {
                inPosition = true;
                side = signals[i];
                signalBar = i;
                entryBar = i + 1;
                entryPrice = ctx.open[i + 1];
                entryMinute = s.minuteOfDay(i + 1);
                dow = s.dayOfWeek(i + 1);
                mfe = 0;
                mae = 0;
                float atr = ctx.atr(14)[i];
                if (Float.isNaN(atr)) atr = 0;
                stopLevel = initialStopAtr != null
                        ? (side > 0 ? entryPrice - (float) (initialStopAtr * atr)
                                    : entryPrice + (float) (initialStopAtr * atr))
                        : 0;
                targetLevel = targetAtr != null
                        ? (side > 0 ? entryPrice + (float) (targetAtr * atr)
                                    : entryPrice - (float) (targetAtr * atr))
                        : 0;
            }
        }
        return trades;
    }
}
