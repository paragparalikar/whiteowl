package com.whiteowl.scripting.ranker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RankerSetting {

    private final String name;
    private final Class<?> type;
    private final Object defaultValue;

}
