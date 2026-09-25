package com.whiteowl.core.backtest.v2.engine;

/**
 * Engine-side standardized exits, expressed in ATR multiples measured from the
 * trade's entry bar. When attached to {@link BacktestEngine}, open positions
 * are checked every bar and closed deterministically:
 *
 * <ul>
 *   <li><b>Initial stop</b> — LONG exits when low ≤ entry − stop·ATR, SHORT
 *       mirrored. Fill at the stop price, or the bar open if the market gapped
 *       through it.</li>
 *   <li><b>Target</b> — LONG exits when high ≥ entry + target·ATR, mirrored.</li>
 *   <li><b>Trailing stop</b> — from the highest high (LONG) / lowest low
 *       (SHORT) since entry minus/plus trail·ATR. Combined with the initial
 *       stop, the tighter level wins.</li>
 *   <li><b>Time stop</b> — exit at the bar close once the position has been
 *       open {@code timeStopBars} bars (entry bar = 0).</li>
 * </ul>
 *
 * <p>Check order within a bar is conservative: stop → target → time stop.
 * Any component left {@code null} is disabled. A strategy's own exit signals
 * still work — the policy is an addition, not a replacement.</p>
 */
public record StandardExitPolicy(
        Double initialStopAtr,
        Double targetAtr,
        Double trailingStopAtr,
        Integer timeStopBars,
        int atrPeriod) {

    public static final int DEFAULT_ATR_PERIOD = 14;

    /** Raw-trade-generation policy: only a (long) time stop bounds trades. */
    public static StandardExitPolicy timeStopOnly(int maxBars) {
        return new StandardExitPolicy(null, null, null, maxBars, DEFAULT_ATR_PERIOD);
    }

    public static StandardExitPolicy of(Double stopAtr, Double targetAtr,
                                         Integer timeStopBars) {
        return new StandardExitPolicy(stopAtr, targetAtr, null, timeStopBars,
                DEFAULT_ATR_PERIOD);
    }

}
