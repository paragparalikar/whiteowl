package com.whiteowl.core.rule;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.IsHighestRule;
import org.ta4j.core.rules.NotRule;

public class NotOverboughtByPriceChannelRule extends AbstractRule {
	
	private final Rule delegate;
	
	public NotOverboughtByPriceChannelRule(final int overboughtFilterLookbackPeriod, final BarSeries barSeries) {
		final HighPriceIndicator highPriceIndicator = new HighPriceIndicator(barSeries);
		final IsHighestRule isHighestRule = new IsHighestRule(highPriceIndicator, overboughtFilterLookbackPeriod);
		this.delegate = new NotRule(isHighestRule);
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		return delegate.isSatisfied(index);
	}

}
