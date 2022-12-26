package com.whiteowl.strategy.donchian;

import java.util.List;

import javax.validation.Valid;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class DonchianBreakoutTradingStrategyExecutor
		implements TradingStrategyExecutor<DonchianBreakoutTradingStrategyConfig> {

	private final BarService barService;
	
	@Override
	public void onScripBarDataDownloaded(
			@Valid @NonNull final DonchianBreakoutTradingStrategyConfig config, 
			@NonNull final Scrip scrip,
			@NonNull final Timeframe timeframe) {
		final long count = Math.max(config.getLowerBandLength(), config.getUpperBandLength());
		final List<Bar> bars = barService.findLatestByCodeAndTimeframe(scrip.getCode(), timeframe, count);
		if(bars.size() >= count) {
			final BarSeries barSeries = new BaseBarSeries(bars);
			
		} else {
			log.warn("Insufficient bar data for {}, required {} bars for strategy {}, but found only {} bars", 
					scrip.getCode(), count, getClass().getSimpleName(), bars.size());
		}
	}

	@Override
	public TradingStrategyTemplate getTradingStrategyTemplate() {
		return TradingStrategyTemplate.DONCHIAN;
	}
	
	@Override
	public void close() throws Exception {

	}

}
