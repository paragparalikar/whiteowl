package com.whiteowl.core.backtest.discovery;

/**
 * One completed signal-to-EOD trade. Prices are raw fills; costs are tracked
 * separately so both gross and net point P&L are recorded, alongside
 * maximum favorable/adverse excursion in points and ATR units.
 */
public record DiscoveryTrade(
        int signalBarIndex,
        int entryBarIndex,
        int exitBarIndex,
        int side,                 // +1 long, -1 short
        long entryTimestamp,
        long exitTimestamp,
        float entryPrice,
        float exitPrice,
        float grossPoints,
        float costPoints,
        float netPoints,
        float mfePoints,          // max favorable excursion, points
        float maePoints,          // max adverse excursion, points (negative)
        float atrAtEntry,
        int holdingBars,
        int entryMinuteOfDay,
        int dayOfWeek) {

    public float mfeAtr() { return atrAtEntry > 0 ? mfePoints / atrAtEntry : Float.NaN; }
    public float maeAtr() { return atrAtEntry > 0 ? maePoints / atrAtEntry : Float.NaN; }
    public boolean win() { return netPoints > 0; }
}
