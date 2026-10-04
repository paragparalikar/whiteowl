package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.whiteowl.core.backtest.algotest.model.enums.AlgoTestEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic "{Type, Value}" wrapper used throughout the AlgoTest payload for
 * lot size, leg/overall SL/target/trail/momentum and re-entry settings.
 * {@code Type} is either an enum api value (e.g. "LegTgtSLType.Points") or the
 * literal "None". Numeric-typed settings serialize {@code Value} as a number;
 * composite settings (trail SL, lock-and-trail, re-entries) serialize it as an
 * object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TypedValue {

    /** Api type string, e.g. "LegTgtSLType.Points", or literal "None". */
    private String type;

    /** Number, {@link TrailValue}, {@link ReentryValue}, or empty object when disabled. */
    private Object value;

    public static TypedValue of(AlgoTestEnum type, Object value) {
        return new TypedValue(type.getApiValue(), normalize(value));
    }

    /** Serializes integral doubles as ints so the wire shows 25, not 25.0. */
    private static Object normalize(Object value) {
        if (value instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) {
            return d.longValue() == (int) d.longValue() ? (int) d.longValue() : d.longValue();
        }
        return value;
    }

    /** Disabled setting whose api expects {@code "Value": 0}. */
    public static TypedValue none() {
        return new TypedValue("None", 0);
    }

    /** Disabled setting whose api expects {@code "Value": {}}. */
    public static TypedValue noneObject() {
        return new TypedValue("None", Map.of());
    }

    @JsonIgnore
    public boolean isNone() {
        return "None".equals(type);
    }

    /** Compact representation for CSV columns, e.g. "LegTgtSLType.Points:25". */
    public String describe() {
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return type;
        }
        return type + ":" + value;
    }
}
