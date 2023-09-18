package com.whiteowl.developer;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(of = {"config", "code", "timeframe"}, callSuper = false)
public class TradeSeries extends ArrayList<Trade> {
	private static final long serialVersionUID = 1L;

	public final int[] config;
	public final String code;
	public final Timeframe timeframe;
	
}
