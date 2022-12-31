package com.whiteowl.strategy.size;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;

import lombok.NonNull;

@Component
public class FixedPercentagePositionSizingStrategy implements PositionSizingStrategy {

	private final BarService barService;
	private final double fixedPositionSizePercentage;
	
	public FixedPercentagePositionSizingStrategy(
			@NonNull final BarService barService,
			@Value("${whiteowl.strategy.size.fixed.percentage:5}") 
			final double fixedPositionSizePercentage) {
		this.barService = barService;
		this.fixedPositionSizePercentage = fixedPositionSizePercentage;
	}
	
	@Override
	public void size(@NonNull Position position, @NonNull Portfolio portfolio) {
		final double maxTradableAmount = portfolio.getMaxTradableAmount();
		final double maxPositionSize = maxTradableAmount * fixedPositionSizePercentage / 100;
		final double avaialbleMargin = portfolio.getAvailableMargin();
		final double effectiveMargin = Math.min(avaialbleMargin, maxPositionSize);
		
		final Map<Trade, Double> pricesPerQuantity = position.getEntryTrades().stream()
				.collect(Collectors.toMap(Function.identity(), this::resolveExecutionPricePerQuantity));
		final double totalEntryPricePerQuantity = pricesPerQuantity.entrySet().stream()
				.map(entry -> entry.getKey().getQuantity() * entry.getValue())
				.collect(Collectors.summingDouble(Double::doubleValue));
		for(Trade trade : position.getEntryTrades()) {
			final int quantity = (int) (effectiveMargin * trade.getQuantity() / totalEntryPricePerQuantity);
			trade.setQuantity(quantity);
		}
	}
	
	// TODO Use of margin provided by broker in case of MIS trades
	private double resolveExecutionPricePerQuantity(Trade trade) {
		if(TradeLimitType.LIMIT.equals(trade.getLimitType()) ||
				TradeLimitType.SL.equals(trade.getLimitType())) {
			return trade.getPrice();
		} else if(TradeLimitType.MARKET.equals(trade.getLimitType()) ||
				TradeLimitType.SLM.equals(trade.getLimitType())) {
			final Bar bar = barService.findLatestBar(trade.getScrip().getCode(), Timeframe.M5).orElseThrow();
			if(TradeType.BUY.equals(trade.getType())) {
				return bar.getHighPrice().doubleValue();
			} else if(TradeType.SELL.equals(trade.getType())) {
				return bar.getLowPrice().doubleValue();
			}
		}
		throw new IllegalArgumentException("Could not determine execution price");
	}

}
