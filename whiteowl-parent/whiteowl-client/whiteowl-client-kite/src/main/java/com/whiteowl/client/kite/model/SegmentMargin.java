package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class SegmentMargin {

	private boolean enabled;
	private double net;
	private AvailableMargin available;
	private UtilizedMargin utilised;
	
}
