package com.whiteowl.core.backtest.rotational;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Configuration for the rotational ORB backtest.
 *
 * <p>All strategy parameters are expressed as plain data (numbers, enums, optionals)
 * so the config is serializable and suitable for parameter optimization. The engine
 * constructs the appropriate filters, exit rules, and ranker from these values.</p>
 *
 * <h3>Parameter groups</h3>
 * <ul>
 *   <li><b>Universe</b> — which stocks to trade</li>
 *   <li><b>Opening Range</b> — OR duration and bar timeframe</li>
 *   <li><b>Entry Filters</b> — min/max thresholds for gap/ATR, OR/ATR, RVOL, RS rank, IBS</li>
 *   <li><b>Entry Method</b> — breakout vs candle-close, re-entries</li>
 *   <li><b>Picks &amp; Ranking</b> — how many picks, which ranker</li>
 *   <li><b>Timing</b> — entry cutoff and exit time</li>
 *   <li><b>Stop Loss</b> — static stop with configurable basis and multiplier</li>
 *   <li><b>Trailing Stop</b> — trailing stop with configurable basis and multiplier</li>
 *   <li><b>Target</b> — profit target with configurable basis and multiplier</li>
 *   <li><b>Position Sizing</b> — capital, slippage, ATR scaling</li>
 * </ul>
 */
@Getter
@Builder
public final class RotationalBacktestConfig {

    // ── Universe ─────────────────────────────────────────────────────────

    /** Name of the group that defines the tradable universe. */
    @Builder.Default private final String universeGroupName = "ORB Universe";

    // ── Date Range (null = use all available data) ─────────────────────

    /** Backtest start date (inclusive). Null means use earliest available. */
    private final LocalDate startDate;

    /** Backtest end date (inclusive). Null means use latest available. */
    private final LocalDate endDate;

    // ── Opening Range ────────────────────────────────────────────────────

    /**
     * Opening range duration in minutes after market open (09:15 IST).
     * Common values: 1, 2, 3, 5, 10, 15, 20, 25, 30, 60.
     */
    @Builder.Default private final int openingRangeMinutes = 5;

    /** Bar timeframe in minutes. Must evenly divide openingRangeMinutes. */
    @Builder.Default private final int barMinutes = 5;

    // ── Entry Filters (null = disabled) ──────────────────────────────────

    /** Minimum gap as a multiple of ATR(14). Null = no minimum. */
    @Builder.Default private final Double minGapAtr = 0.50;

    /** Maximum gap as a multiple of ATR(14). Null = no maximum. */
    @Builder.Default private final Double maxGapAtr = null;

    /** Minimum OR range as a multiple of ATR(14). Null = no minimum. */
    @Builder.Default private final Double minOrAtr = null;

    /** Maximum OR range as a multiple of ATR(14). Null = no maximum. */
    @Builder.Default private final Double maxOrAtr = null;

    /** Minimum relative volume during the OR period. Null = no minimum. */
    @Builder.Default private final Double minOrbRvol = null;

    /** Maximum relative volume during the OR period. Null = no maximum. */
    @Builder.Default private final Double maxOrbRvol = null;

    /** Minimum RS rank percentile (0-100) during the OR. Null = no minimum. */
    @Builder.Default private final Double minRsRank = null;

    /** Maximum RS rank percentile (0-100) during the OR. Null = no maximum. */
    @Builder.Default private final Double maxRsRank = null;

    /**
     * Minimum IBS of the opening range. IBS = (close_of_last_OR_bar - OR_low) / (OR_high - OR_low).
     * Null = no minimum.
     */
    @Builder.Default private final Double minOrbIbs = null;

    /**
     * Maximum IBS of the opening range. Null = no maximum.
     */
    @Builder.Default private final Double maxOrbIbs = 0.90;

    // ── OR Body / Spread filters (0 = disabled) ───────────────────────

    /**
     * Minimum OR body %. Body = |OR_Close - OR_Open| / OR_Close * 100.
     * 0 = no minimum (disabled).
     */
    @Builder.Default private final double minOrBodyPct = 0.0;

    /**
     * Maximum OR body %. 0 = no maximum (disabled).
     */
    @Builder.Default private final double maxOrBodyPct = 0.0;

    /**
     * Minimum OR spread %. Spread = (OR_High - OR_Low) / OR_Close * 100.
     * 0 = no minimum (disabled).
     */
    @Builder.Default private final double minOrSpreadPct = 0.0;

    /**
     * Maximum OR spread %. 0 = no maximum (disabled).
     */
    @Builder.Default private final double maxOrSpreadPct = 0.0;

    /**
     * Controls whether breakout direction must align with gap direction.
     * <ul>
     *   <li>{@link GapDirectionMode#ANY} — no filtering (default)</li>
     *   <li>{@link GapDirectionMode#ALIGNED} — long only on gap-up, short only on gap-down</li>
     *   <li>{@link GapDirectionMode#OPPOSITE} — long only on gap-down, short only on gap-up</li>
     * </ul>
     */
    @Builder.Default private final GapDirectionMode gapDirectionMode = GapDirectionMode.ANY;

    /**
     * Which side this strategy trades. Each strategy trades exactly one side.
     * <ul>
     *   <li>{@link Side#LONG} — only generate long (OR-high) breakouts</li>
     *   <li>{@link Side#SHORT} — only generate short (OR-low) breakouts</li>
     * </ul>
     * <p>Enforced at the simulator level: the {@link OrbSimulator} will never
     * produce a breakout on the opposite side.</p>
     */
    @Builder.Default private final Side side = Side.LONG;

    // ── Entry Method ─────────────────────────────────────────────────────

    /**
     * How to trigger an entry after the OR is established.
     * <ul>
     *   <li>{@link EntryMethod#BREAKOUT} — enter immediately when price crosses OR boundary</li>
     *   <li>{@link EntryMethod#CANDLE_CLOSE} — wait for a candle to close outside the OR</li>
     * </ul>
     */
    @Builder.Default private final EntryMethod entryMethod = EntryMethod.BREAKOUT;

    /**
     * Maximum number of re-entries per symbol per day (same direction only).
     * 0 = no re-entry (one trade per symbol per day, current behavior).
     * After a trade exits (stop/target), the simulator continues scanning
     * for the next breakout in the same direction up to this limit.
     */
    @Builder.Default private final int maxReEntries = 0;

    // ── Picks & Ranking ──────────────────────────────────────────────────

    /** Number of picks per day. */
    @Builder.Default private final int picks = 5;

    /**
     * Which ranker to use for selecting picks.
     * <ul>
     *   <li>{@link RankerType#RS_RVOL} — composite ranker using RVOL * RS rank (best risk-adjusted returns)</li>
     *   <li>{@link RankerType#GAP_RVOL_RS} — composite ranker using |GapATR| * RVOL * RS rank</li>
     *   <li>{@link RankerType#RANDOM} — random selection (baseline comparison)</li>
     *   <li>{@link RankerType#NONE} — trade all triggered breakouts (no pick limit)</li>
     * </ul>
     */
    @Builder.Default private final RankerType rankerType = RankerType.RS_RVOL;

    // ── Timing ───────────────────────────────────────────────────────────

    /**
     * Latest time of day (IST) at which a new entry is allowed.
     * No new trades after this time; existing positions are managed until exit.
     */
    @Builder.Default private final LocalTime entryCutoffTime = LocalTime.of(11, 0);

    /**
     * Time of day (IST) at which to force-exit all open positions.
     * Null = market close (last bar of the day).
     * If set, positions not already closed by stop/target are exited at this time.
     */
    @Builder.Default private final LocalTime exitTime = LocalTime.of(15, 25);

    // ── Stop Loss ────────────────────────────────────────────────────────

    /**
     * What the stop loss distance is measured in.
     * <ul>
     *   <li>{@link StopBasis#OR_RANGE} — stop distance = multiplier * (OR_high - OR_low)</li>
     *   <li>{@link StopBasis#ATR} — stop distance = multiplier * ATR(14)</li>
     * </ul>
     */
    @Builder.Default private final StopBasis stopBasis = StopBasis.OR_RANGE;

    /**
     * Stop loss multiplier. The stop distance = multiplier * basis_value.
     * <ul>
     *   <li>For OR_RANGE with multiplier=1.0: stop at the opposite OR boundary (current behavior)</li>
     *   <li>For ATR with multiplier=1.5: stop at entry +/- 1.5 * ATR(14)</li>
     * </ul>
     */
    @Builder.Default private final double stopMultiplier = 0.8;

    // ── Trailing Stop ────────────────────────────────────────────────────

    /** Whether the trailing stop is enabled. */
    @Builder.Default private final boolean trailingStopEnabled = true;

    /** Basis for the trailing stop distance (OR_RANGE or ATR). */
    @Builder.Default private final StopBasis trailingStopBasis = StopBasis.ATR;

    /**
     * Trailing stop multiplier. The trailing stop level is:
     * <ul>
     *   <li>LONG: highest_high_since_entry - multiplier * basis_value</li>
     *   <li>SHORT: lowest_low_since_entry + multiplier * basis_value</li>
     * </ul>
     * The trailing stop ratchets in the favorable direction (never moves adversely).
     */
    @Builder.Default private final double trailingStopMultiplier = 1.25;

    // ── Target ───────────────────────────────────────────────────────────

    /** Whether the profit target is enabled. */
    @Builder.Default private final boolean targetEnabled = true;

    /**
     * What the target distance is measured in.
     * <ul>
     *   <li>{@link TargetBasis#STOP_DISTANCE} — target = multiplier * stop_distance (R:R ratio)</li>
     *   <li>{@link TargetBasis#OR_RANGE} — target = multiplier * (OR_high - OR_low)</li>
     *   <li>{@link TargetBasis#ATR} — target = multiplier * ATR(14)</li>
     * </ul>
     */
    @Builder.Default private final TargetBasis targetBasis = TargetBasis.STOP_DISTANCE;

    /** Target multiplier. */
    @Builder.Default private final double targetMultiplier = 1.5;

    // ── Position Sizing ──────────────────────────────────────────────────

    /** Starting capital. */
    @Builder.Default private final double initialCapital = 1_000_000;

    /** Round-trip slippage as a fraction (brokerage + STT + impact). */
    @Builder.Default private final double slippage = 0.003;

    /** Whether to use ATR(14) volatility scaling for position sizing. */
    @Builder.Default private final boolean atrScaling = false;

    // ── Enums ────────────────────────────────────────────────────────────

    /** Entry method: how to trigger a trade after OR is established. */
    public enum EntryMethod {
        /** Enter immediately when price crosses the OR boundary. */
        BREAKOUT,
        /** Wait for a candle to close outside the OR. */
        CANDLE_CLOSE
    }

    /** Basis for computing stop loss distance. */
    public enum StopBasis {
        /** Stop distance = multiplier * (OR_high - OR_low). */
        OR_RANGE,
        /** Stop distance = multiplier * ATR(14). */
        ATR
    }

    /** Basis for computing target distance. */
    public enum TargetBasis {
        /** Target distance = multiplier * stop_distance (R:R ratio). */
        STOP_DISTANCE,
        /** Target distance = multiplier * (OR_high - OR_low). */
        OR_RANGE,
        /** Target distance = multiplier * ATR(14). */
        ATR
    }

    /** Ranker type for selecting picks. */
    public enum RankerType {
        /** Single-factor ranker: RS rank only. */
        RS,
        /** Single-factor ranker: RVOL only. */
        RVOL,
        /** Single-factor ranker: |GapATR| only. */
        GAP,
        /** Composite ranker: RVOL * RS rank (no gap factor). Best risk-adjusted returns. */
        RS_RVOL,
        /** Composite ranker: RS rank * |GapATR|. */
        RS_GAP,
        /** Composite ranker: RVOL * |GapATR|. */
        RVOL_GAP,
        /** Composite ranker: |GapATR| * RVOL * RS rank. */
        GAP_RVOL_RS,
        /** Random selection (baseline). */
        RANDOM,
        /** No ranking — trade all triggered breakouts. */
        NONE,
        /** Deterministic alphabetical sorting by symbol name. */
        ALPHABETICAL
    }

    /**
     * Controls whether trade direction must align with gap direction.
     * Applied as a post-filter on ORB results in the engine, after breakout
     * direction is determined and before ranking.
     */
    public enum GapDirectionMode {
        /** No gap-direction filtering — trade any breakout regardless of gap direction. */
        ANY,
        /** Momentum: only long on gap-up, only short on gap-down. */
        ALIGNED,
        /** Contrarian: only long on gap-down, only short on gap-up. */
        OPPOSITE
    }

    /**
     * Which side the strategy trades. A single strategy trades exactly one side.
     */
    public enum Side {
        /** Long (OR-high) breakouts only. */
        LONG,
        /** Short (OR-low) breakouts only. */
        SHORT
    }

    // ── Factory ──────────────────────────────────────────────────────────

    public static RotationalBacktestConfig defaults() {
        return RotationalBacktestConfig.builder().build();
    }

    /** Whether any OR body filter is active. */
    public boolean hasOrBodyFilter() {
        return minOrBodyPct > 0 || maxOrBodyPct > 0;
    }

    /** Whether any OR spread filter is active. */
    public boolean hasOrSpreadFilter() {
        return minOrSpreadPct > 0 || maxOrSpreadPct > 0;
    }

    // ── Validation ───────────────────────────────────────────────────────

    /**
     * Validate this configuration. Throws {@link IllegalArgumentException} if invalid.
     */
    public void validate() {
        if (universeGroupName == null || universeGroupName.isBlank()) {
            throw new IllegalArgumentException("universeGroupName must not be blank");
        }
        if (openingRangeMinutes <= 0) {
            throw new IllegalArgumentException("openingRangeMinutes must be > 0, got: " + openingRangeMinutes);
        }
        if (barMinutes <= 0) {
            throw new IllegalArgumentException("barMinutes must be > 0, got: " + barMinutes);
        }
        if (entryCutoffTime == null) {
            throw new IllegalArgumentException("entryCutoffTime must not be null");
        }
        if (picks <= 0) {
            throw new IllegalArgumentException("picks must be > 0, got: " + picks);
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        if (slippage < 0) {
            throw new IllegalArgumentException("slippage must be >= 0");
        }
        if (initialCapital <= 0) {
            throw new IllegalArgumentException("initialCapital must be > 0");
        }
        if (stopMultiplier <= 0) {
            throw new IllegalArgumentException("stopMultiplier must be > 0, got: " + stopMultiplier);
        }
        if (trailingStopEnabled && trailingStopMultiplier <= 0) {
            throw new IllegalArgumentException("trailingStopMultiplier must be > 0 when trailing stop is enabled");
        }
        if (targetEnabled && targetMultiplier <= 0) {
            throw new IllegalArgumentException("targetMultiplier must be > 0 when target is enabled");
        }
        if (maxReEntries < 0) {
            throw new IllegalArgumentException("maxReEntries must be >= 0, got: " + maxReEntries);
        }
    }

    // ── Display ──────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RotationalBacktestConfig {\n");
        sb.append(String.format("  universe          = %s\n", universeGroupName));
        sb.append(String.format("  openingRange      = %d min (bar=%d min)\n", openingRangeMinutes, barMinutes));
        sb.append(String.format("  entryMethod       = %s\n", entryMethod));
        sb.append(String.format("  maxReEntries      = %d\n", maxReEntries));

        // Entry filters
        sb.append("  filters           =");
        boolean anyFilter = false;
        if (minGapAtr != null) { sb.append(String.format(" GapATR>=%.2f", minGapAtr)); anyFilter = true; }
        if (maxGapAtr != null) { sb.append(String.format(" GapATR<=%.2f", maxGapAtr)); anyFilter = true; }
        if (minOrAtr != null) { sb.append(String.format(" OR/ATR>=%.2f", minOrAtr)); anyFilter = true; }
        if (maxOrAtr != null) { sb.append(String.format(" OR/ATR<=%.2f", maxOrAtr)); anyFilter = true; }
        if (minOrbRvol != null) { sb.append(String.format(" RVOL>=%.2f", minOrbRvol)); anyFilter = true; }
        if (maxOrbRvol != null) { sb.append(String.format(" RVOL<=%.2f", maxOrbRvol)); anyFilter = true; }
        if (minRsRank != null) { sb.append(String.format(" RS>=%.0f", minRsRank)); anyFilter = true; }
        if (maxRsRank != null) { sb.append(String.format(" RS<=%.0f", maxRsRank)); anyFilter = true; }
        if (minOrbIbs != null) { sb.append(String.format(" IBS>=%.2f", minOrbIbs)); anyFilter = true; }
        if (maxOrbIbs != null) { sb.append(String.format(" IBS<=%.2f", maxOrbIbs)); anyFilter = true; }
        if (!anyFilter) sb.append(" None");
        sb.append("\n");

        // OR Body / Spread filters
        if (hasOrBodyFilter() || hasOrSpreadFilter()) {
            sb.append("  orCandleFilters   =");
            if (minOrBodyPct > 0) sb.append(String.format(" Body>=%.2f%%", minOrBodyPct));
            if (maxOrBodyPct > 0) sb.append(String.format(" Body<=%.2f%%", maxOrBodyPct));
            if (minOrSpreadPct > 0) sb.append(String.format(" Spread>=%.2f%%", minOrSpreadPct));
            if (maxOrSpreadPct > 0) sb.append(String.format(" Spread<=%.2f%%", maxOrSpreadPct));
            sb.append("\n");
        }

        sb.append(String.format("  side              = %s\n", side));
        if (gapDirectionMode != GapDirectionMode.ANY) {
            sb.append(String.format("  gapDirection      = %s\n", gapDirectionMode));
        }
        sb.append(String.format("  ranker            = %s\n", rankerType));
        sb.append(String.format("  picks             = %d\n", picks));
        sb.append(String.format("  entryCutoff       = %s IST\n", entryCutoffTime));
        sb.append(String.format("  exitTime          = %s\n", exitTime != null ? exitTime + " IST" : "Market Close"));

        // Stop
        sb.append(String.format("  stop              = %.1fx %s\n", stopMultiplier, stopBasis));

        // Trailing stop
        if (trailingStopEnabled) {
            sb.append(String.format("  trailingStop      = %.1fx %s\n", trailingStopMultiplier, trailingStopBasis));
        } else {
            sb.append("  trailingStop      = OFF\n");
        }

        // Target
        if (targetEnabled) {
            sb.append(String.format("  target            = %.1fx %s\n", targetMultiplier, targetBasis));
        } else {
            sb.append("  target            = OFF\n");
        }

        sb.append(String.format("  slippage          = %.3f%%\n", slippage * 100));
        sb.append(String.format("  initialCapital    = %.0f\n", initialCapital));
        sb.append(String.format("  atrScaling        = %s\n", atrScaling));
        sb.append("}");
        return sb.toString();
    }
}
