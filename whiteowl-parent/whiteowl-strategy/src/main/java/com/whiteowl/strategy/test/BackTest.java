package com.whiteowl.strategy.test;

import java.util.List;
import java.util.Set;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.scrip.Scrip;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackTest {

	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final Set<Position> positions;
	
	
}
