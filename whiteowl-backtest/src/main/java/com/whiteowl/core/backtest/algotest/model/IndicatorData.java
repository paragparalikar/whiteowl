package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.whiteowl.core.backtest.algotest.model.enums.AlgoTestEnum;
import com.whiteowl.core.backtest.algotest.model.enums.IndicatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Leaf node payload of an indicator tree (e.g. a TimeIndicator with Hour/Minute). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class IndicatorData {

    /** Api value of an {@link IndicatorType}, e.g. "IndicatorType.TimeIndicator". */
    private String indicatorName;

    private Map<String, Object> parameters;

    public static IndicatorData time(int hour, int minute) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("Hour", hour);
        params.put("Minute", minute);
        return new IndicatorData(IndicatorType.TIME_INDICATOR.getApiValue(), params);
    }

    public static IndicatorData of(AlgoTestEnum indicator, Map<String, Object> parameters) {
        return new IndicatorData(indicator.getApiValue(), parameters);
    }
}
