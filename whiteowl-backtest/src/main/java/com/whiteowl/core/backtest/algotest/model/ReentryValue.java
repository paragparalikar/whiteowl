package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value payload for leg and overall re-entry settings
 * (LegReentrySL, LegReentryTP, OverallReentrySL, OverallReentryTgt).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReentryValue {

    /** Number of re-entries allowed (api limit: 20 per leg, 5 overall). */
    private int reentryCount;

    /** For ReentryType.NextLeg ("lazy leg"): id of the leg to wait for, else null. */
    private String nextLegRef;
}
