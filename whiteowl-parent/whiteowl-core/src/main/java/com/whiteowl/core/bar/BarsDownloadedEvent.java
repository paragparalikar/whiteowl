package com.whiteowl.core.bar;

import java.time.ZonedDateTime;

import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class BarsDownloadedEvent {

	@NonNull private Scrip scrip;
	@NonNull private Timeframe timeframe;
	@NonNull private ZonedDateTime timestamp;

}
