package com.whiteowl.analysis.gpu;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Bar {

	private final float open, high, low, close;
	private final long volume, date;

}
