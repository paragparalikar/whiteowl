package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.whiteowl.core.backtest.algotest.model.enums.StrategyType;
import com.whiteowl.core.backtest.algotest.model.enums.Ticker;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The "strategy" object of the AlgoTest backtest request: global settings plus
 * the list of leg configs.
 *
 * Booleans like {@code takeUnderlyingFromCashOrNot} are the api's "True"/"False"
 * strings, not JSON booleans. {@code reentryTimeRestriction} and
 * {@code stopMonitoringTime} are the literal "None" or a value/object —
 * modelled as {@link Object}.
 */
@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AlgoStrategy {

    private Ticker ticker = Ticker.NIFTY;
    private String takeUnderlyingFromCashOrNot = "True";
    private String trailSLtoBreakeven = "False";
    private String squareOffAllLegs = "False";

    /** Entry condition tree — typically a single time indicator. */
    private IndicatorNode entryIndicators = IndicatorNode.timeOperand(9, 20);

    /** Exit condition tree — typically a single time indicator. */
    private IndicatorNode exitIndicators = IndicatorNode.timeOperand(15, 15);

    private StrategyType strategyType = StrategyType.INTRADAY_SAME_DAY;
    private int maxPositionInADay = 1;

    /** "None" or minutes after which re-entry is disallowed. */
    private Object reentryTimeRestriction = "None";

    private int skipInitialCandles = 0;

    /** "None" or a stop-monitoring time. */
    private Object stopMonitoringTime = "None";

    private List<LegConfig> listOfLegConfigs = new ArrayList<>();

    /** Idle (disabled) legs kept in the saved strategy — normally empty. */
    private Object idleLegConfigs = Map.of();

    /** OverallTgtSLType + value, or disabled. Combined MTM stop loss. */
    private TypedValue overallSL = TypedValue.none();

    /** OverallTgtSLType + value, or disabled. Combined MTM target. */
    private TypedValue overallTgt = TypedValue.none();

    /** TrailStopLossType + {@link TrailValue}, or disabled. Trails the combined MTM SL. */
    private TypedValue overallTrailSL = TypedValue.noneObject();

    /** TrailStopLossType + {@link TrailValue} ("If Profit reaches" / "Lock profit"), or disabled. */
    private TypedValue lockAndTrail = TypedValue.noneObject();

    /** ReentryType + {@link ReentryValue}, or disabled. Re-enter all legs after overall SL. */
    private TypedValue overallReentrySL = TypedValue.noneObject();

    /** ReentryType + {@link ReentryValue}, or disabled. Re-enter all legs after overall target. */
    private TypedValue overallReentryTgt = TypedValue.noneObject();

    /** OverallMomentumType + value, or disabled. */
    private TypedValue overallMomentum = TypedValue.none();

    private boolean weeklyOldRegime = true;

    public LegConfig addLeg(LegConfig leg) {
        listOfLegConfigs.add(leg);
        return leg;
    }

    /** Applies {@code mutator} to every leg — used by parameter sweeps. */
    public void forEachLeg(Consumer<LegConfig> mutator) {
        listOfLegConfigs.forEach(mutator);
    }

    public void setEntryTime(int hour, int minute) {
        entryIndicators = IndicatorNode.timeOperand(hour, minute);
    }

    public void setExitTime(int hour, int minute) {
        exitIndicators = IndicatorNode.timeOperand(hour, minute);
    }
}
