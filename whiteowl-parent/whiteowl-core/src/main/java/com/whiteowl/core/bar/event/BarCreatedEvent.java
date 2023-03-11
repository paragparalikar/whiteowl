package com.whiteowl.core.bar.event;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;
import lombok.Value;

@Value
public class BarCreatedEvent {
	@NonNull private Bar bar;
	@NonNull private Scrip scrip;
	@NonNull private Timeframe timeframe;
}