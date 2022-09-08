package com.whiteowl.strategy.shortstrangle;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ShortStraddleTradingStrategy implements TradingStrategy {

	private final OptionChain optionChain;
	private final ShortStraddleConfig config;
	
	@Override
	public boolean handle(@NonNull Position position) {
		if(PositionStatus.NEW.equals(position.getStatus())) {
			return handleNewPosition(position);
		} else if(PositionStatus.OPEN.equals(position.getStatus())) {
			return handleOpenPosition(position);
		} else {
			log.error("Position with status {} should not reach this point : {}", position.getStatus(), position);
			return false;
		}
	}
	
	private boolean handleNewPosition(Position position) {
		final LocalTime now = LocalTime.now();
		if(now.isAfter(config.getMinPositionOpenTime()) 
				&& now.isBefore(config.getMaxPositionOpenTime())) {
			open(position);
			return true;
		}
		return false;
	}
	
	private boolean handleOpenPosition(Position position) {
		final Set<Scrip> exitScrips = position.getExitTrades().stream()
				.map(Trade::getScrip).collect(Collectors.toSet());
		return position.getEntryTrades().stream()
			.filter(trade -> !exitScrips.contains(trade.getScrip()))
			.filter(this::shouldClose)
			.map(Trade::complement)
			.map(exitTrade -> position.getExitTrades().add(exitTrade))
			.anyMatch(Predicate.isEqual(Boolean.TRUE));
	}
	
	private boolean shouldClose(Trade trade) {
		if(LocalTime.now().isAfter(config.getMaxPositionCloseTime())) return true;
		final OptionChainItem item = optionChain.findByScrip(trade.getScrip()).orElse(null);
		if(null == item) return false; 
		final double entryPrice = trade.getAveragePrice();
		final double targetPrice = entryPrice * (1 + config.getPercentageTarget() / 100);
		final double stopLossPrice = entryPrice * (1 - config.getPercentageStopLoss() / 100);
		final double currentPrice = item.getLastTradedPrice();
		return currentPrice >= targetPrice || currentPrice <= stopLossPrice;
	}
	
	private void open(Position position) {
		createEntryTrade(ScripType.CE).ifPresent(callEntryTrade -> {
			createEntryTrade(ScripType.PE).ifPresent(putEntryTrade -> {
				position.setStatus(PositionStatus.NEW);
				position.getEntryTrades().add(putEntryTrade);
				position.getEntryTrades().add(callEntryTrade);
				position.setScrip(optionChain.getUnderlying());
				position.setTradingStrategyConfigId(config.getId());
			});
		});
	}
	
	private Optional<Trade> createEntryTrade(ScripType scripType) {
		return optionChain.findByDeltaAndScripType(0.5, scripType)
			.map(optionChainItem -> Trade.builder()
				.limitType(TradeLimitType.MARKET)
				.product(TradeProduct.MIS)
				.quantity(config.getQuantity())
				.scrip(optionChainItem.getScrip())
				.status(TradeStatus.NEW)
				.type(TradeType.SELL)
				.validity(TradeValidity.DAY)
				.variety(TradeVariety.REGULAR)
				.build());
	}
	
}
