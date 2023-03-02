package com.whiteowl.core.bar.event;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.Value;

@Value
public class ScripBarDownloadedEvent {

	@NonNull private Scrip scrip;
	@NonNull private List<Bar> bars;
	@NonNull private Timeframe timeframe;

}
