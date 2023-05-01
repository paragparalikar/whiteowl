package com.whiteowl.core.strategy.impl.trendfollowing;

import java.util.Collection;
import java.util.Collections;

import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.strategy.AbstractTradingStrategy;
import com.whiteowl.core.strategy.context.TradingStrategyContext;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;

public class MACDLongTradingStrategy extends AbstractTradingStrategy<MACDLongTradingStrategyConfig> {

	private final Rule entryRule, exitRule;
	
	public MACDLongTradingStrategy(MACDLongTradingStrategyConfig config, TradingStrategyContext context) {
		super(config, context);
		final Indicator<Num> closePriceIndicator = new ClosePriceIndicator(getBarSeries());
		final Indicator<Num> macdIndicator = new MACDIndicator(closePriceIndicator, config.getShortBarCount(), config.getLongBarCount());
		final Indicator<Num> signalIndicator = new EMAIndicator(macdIndicator, config.getSignalBarCount());
		this.entryRule = new CrossedUpIndicatorRule(macdIndicator, signalIndicator);
		this.exitRule = new CrossedDownIndicatorRule(macdIndicator, signalIndicator)
				.or(new UnderIndicatorRule(macdIndicator, signalIndicator));
	}

	@Override
	protected boolean shouldEnter() {
		return entryRule.isSatisfied(getBarSeries().getEndIndex());
	}
	
	@Override
	protected boolean shouldExit(Quote quote, Position position) {
		return exitRule.isSatisfied(getBarSeries().getEndIndex());
	}

	@Override
	protected Collection<Trade> createEntryTrades() {
		final Trade entryTrade = new Trade();
		entryTrade.setScrip(getScrip());
		entryTrade.setQuantity(1); 
		entryTrade.setType(TradeType.BUY);
		entryTrade.setStatus(TradeStatus.NEW);
		entryTrade.setLimitType(TradeLimitType.MARKET);
		entryTrade.setValidity(TradeValidity.DAY);
		entryTrade.setVariety(TradeVariety.REGULAR);
		entryTrade.setPrice(getBarSeries().getLastBar().getHighPrice().doubleValue());
		entryTrade.setProduct(getScrip().isDerivative() ? TradeProduct.NRML : TradeProduct.CNC);
		return Collections.singleton(entryTrade);
	}

}
