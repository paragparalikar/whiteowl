package com.whiteowl.core.backtest.algotest;

/**
 * Names of every parameter that can be swept by {@link AlgoTestOptimizer},
 * for use as {@link ParameterSweep} labels (they show up in the per-run log
 * lines and in no CSV column — CSV input columns are flattened from the
 * request itself by {@link ResultFlattener}).
 *
 * <p>A sweep mutates the {@link com.whiteowl.core.backtest.algotest.model.AlgoStrategy}
 * template before each run. Strategy-level parameters set fields directly;
 * leg-level parameters are applied via {@code s.forEachLeg(l -> ...)} for all
 * legs or {@code s.getListOfLegConfigs().get(i)} for one leg.
 *
 * <p>Example:
 * <pre>{@code
 * List<ParameterSweep<?>> sweeps = List.of(
 *         ParameterSweep.numericRange(SweepParameters.CLOSEST_PREMIUM, 100, 400, 25,
 *                 (s, v) -> s.forEachLeg(l -> l.setClosestPremium(v))),
 *         ParameterSweep.strikes(SweepParameters.STRIKE_TYPE,
 *                 StrikeType.ATM, StrikeType.OTM10,
 *                 (s, v) -> s.forEachLeg(l -> l.setStrike(v))),
 *         ParameterSweep.intRange(SweepParameters.MAX_POSITION_IN_A_DAY, 1, 3, 1,
 *                 AlgoStrategy::setMaxPositionInADay));
 * }</pre>
 */
public final class SweepParameters {

    private SweepParameters() {
    }

    // ==================== strategy-level (AlgoStrategy setters) ====================

    /** Index/instrument — {@link com.whiteowl.core.backtest.algotest.model.enums.Ticker}. */
    public static final String TICKER = "Ticker";

    /** Leg-entry time — sweep {@link java.time.LocalTime} values via
     *  {@code (s, v) -> s.setEntryTime(v.getHour(), v.getMinute())}. */
    public static final String ENTRY_TIME = "EntryTime";

    /** Square-off time — sweep {@link java.time.LocalTime} values via
     *  {@code (s, v) -> s.setExitTime(v.getHour(), v.getMinute())}. */
    public static final String EXIT_TIME = "ExitTime";

    /** {@link com.whiteowl.core.backtest.algotest.model.enums.StrategyType}
     *  (INTRADAY_SAME_DAY / INTRADAY_BTST). */
    public static final String STRATEGY_TYPE = "StrategyType";

    /** Max positions the strategy may hold in a day — int. */
    public static final String MAX_POSITION_IN_A_DAY = "MaxPositionInADay";

    /** Minutes after entry time during which re-entry is allowed, or "None"
     *  — sweep a heterogeneous list, e.g. {@code List.of("None", 60, 120)}. */
    public static final String REENTRY_TIME_RESTRICTION = "ReentryTimeRestriction";

    /** Number of initial candles to skip before taking trades — int. */
    public static final String SKIP_INITIAL_CANDLES = "SkipInitialCandles";

    /** Time after which open trades are no longer monitored, or "None". */
    public static final String STOP_MONITORING_TIME = "StopMonitoringTime";

    /** "True"/"False" string — take spot price from cash market. */
    public static final String TAKE_UNDERLYING_FROM_CASH = "TakeUnderlyingFromCashOrNot";

    /** "True"/"False" string — move SL to breakeven once in profit. */
    public static final String TRAIL_SL_TO_BREAKEVEN = "TrailSLtoBreakeven";

    /** "True"/"False" string — square off every leg when one exits. */
    public static final String SQUARE_OFF_ALL_LEGS = "SquareOffAllLegs";

    /** Use the pre-2024 weekly-expiry regime — boolean. */
    public static final String WEEKLY_OLD_REGIME = "WeeklyOldRegime";

    /** Combined MTM stop loss — double; apply via
     *  {@code s.setOverallSL(TypedValue.of(OverallTgtSLType.MTM, v))}. */
    public static final String OVERALL_SL = "OverallSL";

    /** Combined MTM target — double; apply via
     *  {@code s.setOverallTgt(TypedValue.of(OverallTgtSLType.MTM, v))}. */
    public static final String OVERALL_TGT = "OverallTgt";

    /** Trails the combined MTM SL — sweep {@link com.whiteowl.core.backtest.algotest.model.TrailValue}
     *  pairs via {@code ParameterSweep.of}; apply via
     *  {@code s.setOverallTrailSL(TypedValue.of(TrailStopLossType.POINTS, v))}. */
    public static final String OVERALL_TRAIL_SL = "OverallTrailSL";

    /** "If profit reaches X, lock Y" — sweep {@link com.whiteowl.core.backtest.algotest.model.TrailValue}
     *  pairs; apply via {@code s.setLockAndTrail(TypedValue.of(TrailStopLossType.POINTS, v))}. */
    public static final String LOCK_AND_TRAIL = "LockAndTrail";

    /** Re-enter all legs after overall SL — int count (api limit 5); apply via
     *  {@code s.setOverallReentrySL(TypedValue.of(ReentryType.IMMEDIATE, new ReentryValue(v, null)))}. */
    public static final String OVERALL_REENTRY_SL = "OverallReentrySL";

    /** Re-enter all legs after overall target — int count (api limit 5); apply via
     *  {@code s.setOverallReentryTgt(TypedValue.of(ReentryType.IMMEDIATE, new ReentryValue(v, null)))}. */
    public static final String OVERALL_REENTRY_TGT = "OverallReentryTgt";

    /** Combined-position momentum trigger — double; apply via
     *  {@code s.setOverallMomentum(TypedValue.of(OverallMomentumType.POINTS_UP, v))}. */
    public static final String OVERALL_MOMENTUM = "OverallMomentum";

    // ==================== leg-level (LegConfig setters) ====================

    /** Buy or sell the leg — {@link com.whiteowl.core.backtest.algotest.model.enums.PositionType}. */
    public static final String LEG_POSITION_TYPE = "LegPositionType";

    /** Option/future/cash kind — {@link com.whiteowl.core.backtest.algotest.model.enums.LegType}
     *  (CE, PE, FUT, FUT_P, CASH). */
    public static final String LEG_INSTRUMENT_KIND = "LegInstrumentKind";

    /** Which expiry to trade — {@link com.whiteowl.core.backtest.algotest.model.enums.ExpiryType}
     *  (WEEKLY, NEXT_WEEKLY, MONTHLY, NEXT_MONTHLY, TODAY, TOMORROW). */
    public static final String LEG_EXPIRY_KIND = "LegExpiryKind";

    /** Lot/capital size — double; apply via
     *  {@code l.setLots(LotType.QUANTITY, v)} or {@code LotType.CAPITAL}. */
    public static final String LEG_LOTS = "LegLots";

    /** ITM/ATM/OTM strike walk — {@link com.whiteowl.core.backtest.algotest.model.enums.StrikeType};
     *  use {@link ParameterSweep#strikes} for ATM..OTMn ranges. */
    public static final String STRIKE_TYPE = "StrikeType";

    /** Numeric strike parameter for delta / exact-strike / premium entries —
     *  double; apply via {@code l.strikeParameter(v)} (entry type must match). */
    public static final String STRIKE_PARAMETER = "StrikeParameter";

    /** "Closest Premium" entry — double target premium; apply via
     *  {@code l.setClosestPremium(v)}. */
    public static final String CLOSEST_PREMIUM = "ClosestPremium";

    /** Leg stop loss in % of entry premium — double; apply via
     *  {@code l.setStopLoss(LegTgtSLType.PERCENTAGE, v)}. */
    public static final String LEG_STOP_LOSS_PCT = "LegStopLossPct";

    /** Leg stop loss in points — double; apply via
     *  {@code l.setStopLoss(LegTgtSLType.POINTS, v)}. Other
     *  {@link com.whiteowl.core.backtest.algotest.model.enums.LegTgtSLType}
     *  values (underlying points/%, delta, breadth) also work. */
    public static final String LEG_STOP_LOSS_POINTS = "LegStopLossPoints";

    /** Leg target in % of entry premium — double; apply via
     *  {@code l.setTarget(LegTgtSLType.PERCENTAGE, v)}. */
    public static final String LEG_TARGET_PCT = "LegTargetPct";

    /** Leg target in points — double; apply via
     *  {@code l.setTarget(LegTgtSLType.POINTS, v)}. */
    public static final String LEG_TARGET_POINTS = "LegTargetPoints";

    /** Leg trailing SL — sweep {@link com.whiteowl.core.backtest.algotest.model.TrailValue}
     *  (instrumentMove, stopLossMove) pairs via {@code ParameterSweep.of}; apply
     *  via {@code l.setTrailSL(TrailStopLossType.POINTS, v.getInstrumentMove(), v.getStopLossMove())}. */
    public static final String LEG_TRAIL_SL = "LegTrailSL";

    /** Leg momentum trigger — double; apply via
     *  {@code l.setMomentum(MomentumType.PERCENTAGE_UP, v)}. */
    public static final String LEG_MOMENTUM = "LegMomentum";

    /** Re-entry count after the leg's SL hits (api limit 20) — int; apply via
     *  {@code l.setReentryOnSL(ReentryType.IMMEDIATE, v)}. */
    public static final String LEG_REENTRY_ON_SL = "LegReentryOnSL";

    /** Re-entry count after the leg's target hits (api limit 20) — int; apply via
     *  {@code l.setReentryOnTP(ReentryType.IMMEDIATE, v)}. */
    public static final String LEG_REENTRY_ON_TP = "LegReentryOnTP";
}
