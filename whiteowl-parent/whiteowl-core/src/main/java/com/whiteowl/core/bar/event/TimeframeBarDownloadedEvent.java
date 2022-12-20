package com.whiteowl.core.bar.event;

import com.whiteowl.core.bar.Timeframe;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class TimeframeBarDownloadedEvent {

	@NonNull private final Timeframe timeframe;

}
