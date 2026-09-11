package com.whiteowl.scripting.screener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ScreenSetting {

    private final String name;
    private final Class<?> type;
    private final Object defaultValue;

}
