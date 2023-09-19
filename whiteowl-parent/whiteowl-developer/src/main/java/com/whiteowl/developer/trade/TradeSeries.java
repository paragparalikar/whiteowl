package com.whiteowl.developer.trade;

import java.util.ArrayList;

import com.whiteowl.developer.bar.Timeframe;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(of = {"config", "code", "timeframe"}, callSuper = false)
public class TradeSeries extends ArrayList<Trade> {
	private static final long serialVersionUID = 1L;

	public final String code;
	public final int[] config;
	public final Timeframe timeframe;
	
}
