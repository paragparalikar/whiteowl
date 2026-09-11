package com.whiteowl.workbench.charting.indicator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class IndicatorSetting {

    private final String name;
    private final Class<?> type;
    private final Object defaultValue;

}
