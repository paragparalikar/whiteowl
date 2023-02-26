package com.whiteowl.core.bar.event;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.Value;

@Value
public class ScripBarDownloadedEvent {

	@NonNull private Scrip scrip;
	@NonNull private Timeframe timeframe;

}
