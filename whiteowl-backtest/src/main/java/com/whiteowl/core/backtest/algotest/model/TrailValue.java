package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value payload for trailing stop loss and lock-and-trail settings
 * (LegTrailSL, OverallTrailSL, LockAndTrail).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class TrailValue {

    /** For leg trail SL: points/% move of the instrument that arms the trail.
     *  For LockAndTrail: "If Profit reaches". For OverallTrailSL: "Profit increases". */
    private double instrumentMove;

    /** For leg trail SL: SL move per instrument move.
     *  For LockAndTrail: "Lock profit". For OverallTrailSL: "Trail profit by". */
    private double stopLossMove;
}
