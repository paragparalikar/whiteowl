package com.whiteowl.core.backtest.rotational;

import lombok.Getter;

/**
 * Defines how and when an ORB entry is triggered once price crosses the opening range.
 *
 * <p>Two modes:</p>
 * <ul>
 *   <li>{@link #BREAKOUT} — enter immediately as price crosses the opening range boundary
 *       (traditional ORB). The entry price is the opening range high/low itself.</li>
 *   <li>{@link #CANDLE_CLOSE} — wait for a confirmation candle of a specified timeframe
 *       to <em>close</em> outside the opening range before entering. For example, trading
 *       a 15-minute ORB but requiring a 5-minute candle to close above OR-high before
 *       going long. The entry price is the close of the confirmation candle.</li>
 * </ul>
 *
 * <p>When using {@code CANDLE_CLOSE}, set {@link #confirmationCandle} to the confirmation
 * candle duration in minutes. This must be {@code <= openingRangeMinutes} in the config.</p>
 */
@Getter
public final class EntryMode {

    /** Enter immediately on opening range breakout. */
    public static final EntryMode BREAKOUT = new EntryMode(Type.BREAKOUT, 0);

    private final Type type;

    /**
     * Confirmation candle duration in minutes (only meaningful for {@link Type#CANDLE_CLOSE}).
     * For example, 5 means "wait for a 5-minute candle to close outside the OR".
     */
    private final int confirmationCandleMinutes;

    private EntryMode(Type type, int confirmationCandleMinutes) {
        this.type = type;
        this.confirmationCandleMinutes = confirmationCandleMinutes;
    }

    /**
     * Create a candle-close confirmation entry mode.
     *
     * @param candleMinutes the candle timeframe in minutes (e.g. 5 for a 5-minute candle)
     * @return entry mode requiring a candle close outside the OR
     * @throws IllegalArgumentException if candleMinutes <= 0
     */
    public static EntryMode candleClose(int candleMinutes) {
        if (candleMinutes <= 0) {
            throw new IllegalArgumentException("Confirmation candle minutes must be > 0, got: " + candleMinutes);
        }
        return new EntryMode(Type.CANDLE_CLOSE, candleMinutes);
    }

    public boolean isBreakout() {
        return type == Type.BREAKOUT;
    }

    public boolean isCandleClose() {
        return type == Type.CANDLE_CLOSE;
    }

    @Override
    public String toString() {
        if (type == Type.BREAKOUT) return "BREAKOUT";
        return "CANDLE_CLOSE(" + confirmationCandleMinutes + "min)";
    }

    public enum Type {
        /** Enter immediately when price crosses the opening range. */
        BREAKOUT,
        /** Wait for a candle to close outside the opening range. */
        CANDLE_CLOSE
    }
}
