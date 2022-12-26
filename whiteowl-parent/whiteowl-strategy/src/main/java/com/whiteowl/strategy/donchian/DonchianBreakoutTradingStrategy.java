package com.whiteowl.strategy.donchian;

import java.util.Optional;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;

import lombok.NonNull;

public class DonchianBreakoutTradingStrategy {

	Optional<Position> openLongPosition(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final HighPriceIndicator highPriceIndicator = new HighPriceIndicator(barSeries);
		final HighestValueIndicator highestHighIndicator = new HighestValueIndicator(highPriceIndicator, config.getUpperBandLength());
		final PreviousValueIndicator previousHighestHighIndicator = new PreviousValueIndicator(highestHighIndicator);
		final LowPriceIndicator lowPriceIndicator = new LowPriceIndicator(barSeries);
		final LowestValueIndicator lowestLowIndicator = new LowestValueIndicator(lowPriceIndicator, config.getLowerBandLength());
		final PreviousValueIndicator previousLowestLowIndicator = new PreviousValueIndicator(lowestLowIndicator);
		final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
		final CrossedUpIndicatorRule longRule = new CrossedUpIndicatorRule(closePriceIndicator, previousHighestHighIndicator);
		final CrossedDownIndicatorRule shortRule = new CrossedDownIndicatorRule(closePriceIndicator, previousLowestLowIndicator);
		return null;
	}
	
	private Position createNewPosition(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull final TradeType tradeType,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setStatus(PositionStatus.NEW);
		position.setTradingStrategyConfigId(config.getId());
		final Trade trade = createNewTrade(scrip, barSeries, tradeType, config);
		position.getEntryTrades().add(trade);
		return position;
	}
	
	private Trade createNewTrade(
			@NonNull final Scrip scrip,
			@NonNull final BarSeries barSeries,
			@NonNull final TradeType tradeType,
			@NonNull final DonchianBreakoutTradingStrategyConfig config) {
		final Trade trade = new Trade();
		trade.setScrip(scrip);
		trade.setQuantity(1); // TODO calculate quantity
		trade.setType(tradeType);
		trade.setPrice(barSeries.getLastBar().getClosePrice().doubleValue());
		trade.setProduct(TradeProduct.NRML);
		trade.setStatus(TradeStatus.NEW);
		trade.setLimitType(TradeLimitType.LIMIT);
		trade.setValidity(TradeValidity.DAY);
		trade.setVariety(TradeVariety.REGULAR);
		return trade;
	}

}
