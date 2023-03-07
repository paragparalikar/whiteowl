package com.whiteowl.core.strategy.impl.trendfollowing;

import java.util.Collection;
import java.util.Collections;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import com.whiteowl.core.strategy.AbstractTradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;

public class SMATradingStrategy extends AbstractTradingStrategy<SMATradingStrategyConfig> {

	private final Rule entryRule;
	
	public SMATradingStrategy(SMATradingStrategyConfig config, TradingStrategyContext context) {
		super(config, context);
		final BarSeries barSeries = getBarSeries();
		final Indicator<Num> closePriceIndicator = new ClosePriceIndicator(barSeries);
		final Indicator<Num> smaIndicator = new SMAIndicator(closePriceIndicator, config.getBarCount());
		this.entryRule = new CrossedUpIndicatorRule(closePriceIndicator, smaIndicator);
	}

	@Override
	protected boolean shouldEnter() {
		return entryRule.isSatisfied(getBarSeries().getEndIndex());
	}

	@Override
	protected Collection<Trade> createEntryTrades() {
		final Trade entryTrade = new Trade();
		entryTrade.setScrip(getScrip());
		entryTrade.setQuantity(1); 
		entryTrade.setType(TradeType.BUY);
		entryTrade.setProduct(TradeProduct.MIS);
		entryTrade.setStatus(TradeStatus.NEW);
		entryTrade.setLimitType(TradeLimitType.MARKET);
		entryTrade.setValidity(TradeValidity.DAY);
		entryTrade.setVariety(TradeVariety.REGULAR);
		return Collections.singleton(entryTrade);
	}
	
}
