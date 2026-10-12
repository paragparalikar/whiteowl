package com.whiteowl.core.backtest.discovery;

/**
 * A raw entry signal: evaluated on closed bars, it marks bar i with +1 (long
 * entry intent), -1 (short) or 0. The discovery engine fills at the next bar's
 * open and exits at the session EOD time, so a signal is a pure timing event —
 * all regime information is carried by the feature snapshot, not the signal.
 */
public interface EntrySignal {

    String name();

    /** +1 long / -1 short / 0 flat, one value per bar, causal. */
    byte[] evaluate(SignalContext ctx);

    static EntrySignal of(String name, java.util.function.Function<SignalContext, byte[]> fn) {
        return new EntrySignal() {
            public String name() { return name; }
            public byte[] evaluate(SignalContext ctx) { return fn.apply(ctx); }
        };
    }
}
