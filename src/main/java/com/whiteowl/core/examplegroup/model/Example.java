package com.whiteowl.core.examplegroup.model;

import com.whiteowl.core.bar.model.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class Example {

    private final String scripId;
    private final Timeframe timeframe;
    private final long startTimestamp;
    private final long endTimestamp;

}
