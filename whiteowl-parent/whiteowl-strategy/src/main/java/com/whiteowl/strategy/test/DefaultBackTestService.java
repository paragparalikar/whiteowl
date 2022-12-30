package com.whiteowl.strategy.test;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.config.TradingStrategyConfig;
import com.whiteowl.strategy.test.mock.MockContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultBackTestService implements BackTestService {

	private final BarService barService;
	private final ScripService scripService;
	
	public BackTest testByIndex(@NonNull final TradingStrategyConfig config) {
		final Timeframe timeframe = config.getTimeframe();
		final Scrip scrip = scripService.findByCode(config.getScripCode());
		final List<Bar> bars = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe);
		final MockContext context = new MockContext(scrip, bars, timeframe, config);
		
		while(context.next()) {
			final List<Position> positions = context.getPositionService()
					.findByTradingStrategyConfigIdAndStatusNot(config.getId(), PositionStatus.CLOSED);
		}
		
		
		return null;
	}
	
	

	
}
