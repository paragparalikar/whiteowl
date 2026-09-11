package com.whiteowl.core.backtest.v2.feature;

import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class FeatureDefinition {

    private final String name;
    private final FloatSmartValue source;
    private final int offset;

    public FeatureDefinition(String name, FloatSmartValue source) {
        this(name, source, 0);
    }

    public float readValue() {
        return source.getAt(offset);
    }

}
