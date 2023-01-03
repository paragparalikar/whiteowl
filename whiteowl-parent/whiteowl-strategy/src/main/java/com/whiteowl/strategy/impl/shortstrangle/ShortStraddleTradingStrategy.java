package com.whiteowl.strategy.impl.shortstrangle;

import java.util.Optional;
import java.util.function.Predicate;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.derivative.option.OptionInfo;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;
import com.whiteowl.strategy.TradingStrategy;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ShortStraddleTradingStrategy implements TradingStrategy {
	
	@NonNull private final OptionChain optionChain;
	@NonNull private final ShortStraddleConfig config;
	
	@Override
	public Optional<Position> enter() {
		final OptionInfo optionInfo = optionChain.findByDeltaAndScripType(0.5, ScripType.CE).orElseThrow();
		final OptionChainItem item = optionChain.findByStrikePrice(optionInfo.getStrikePrice()).orElseThrow();
		final OptionInfo callInfo = item.getCallOptionInfo();
		final OptionInfo putInfo = item.getPutOptionInfo(); 
		final Position position = createNewPosition(optionChain.getUnderlying(), callInfo.getScrip(), 
				putInfo.getScrip(), config.getId(), config.getQuantity());
		return Optional.of(position);
	}
	
	@Override
	public boolean quantify(@NonNull Position position, double amount) {
		return false;
	}
	
	@Override
	public boolean manage(@NonNull Position position) {
		position.getEntryTrades().stream()
			.filter(entryTrade -> !position.getExitTrades().stream()
				.map(Trade::getScrip)
				.anyMatch(Predicate.isEqual(entryTrade.getScrip())))
			.map(Trade::complement)
			.forEach(position.getExitTrades()::add);
		return true;
	}
	
	@Override
	public boolean manage(@NonNull Position position, @NonNull Quote quote) {
		final double stopLossMultiplier = 1 + (position.getExitTrades().isEmpty() ? config.getPercentageStopLoss() / 100 : 0);
		return 0 < position.getEntryTrades().stream()
				.filter(trade -> position.getExitTrades().stream()
						.map(Trade::getScrip)
						.anyMatch(Predicate.isEqual(trade.getScrip())))
				.filter(trade -> quote.getCode().equalsIgnoreCase(trade.getScrip().getCode()))
				.filter(trade -> quote.getLastPrice() >= trade.getAveragePrice() * stopLossMultiplier)
				.map(Trade::complement)
				.map(position.getExitTrades()::add)
				.count();
	}

	private Position createNewPosition(Scrip underlying, Scrip call, Scrip put, String configId, int quantity) {
		final Position position = new Position();
		position.setScrip(underlying);
		position.setTradingStrategyConfigId(configId);
		position.getEntryTrades().add(createEntryTrade(put, quantity));
		position.getEntryTrades().add(createEntryTrade(call, quantity));
		return position;
	}
	
	private Trade createEntryTrade(Scrip scrip, int quantity) {
		return Trade.builder()
				.limitType(TradeLimitType.MARKET)
				.product(TradeProduct.MIS)
				.quantity(quantity)
				.scrip(scrip)
				.status(TradeStatus.NEW)
				.type(TradeType.SELL)
				.validity(TradeValidity.DAY)
				.variety(TradeVariety.REGULAR)
				.build();
	}
	
}
