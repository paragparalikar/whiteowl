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
		}
		log.error("A position with status {} reached a point where it shouldn't : {}", 
				position.getStatus(), position);
		return false;
	}
	
	private boolean handleNewPosition(Position position) {
		if(!position.getEntryTrades().isEmpty()) {
			// This position has already been populated with trades. It should not have made till
			// this point. We will just log and return false to indicate no action is required.
			log.error("A new position with existing entry trades reache a point where it shouldn't : {}", position);
			return false;
		}
		
		final LocalTime now = LocalTime.now();
		
		if(now.isBefore(config.getMinPositionOpenTime())) {
			// We are not allowed to take any trades just yet. 
			// We need to wait till market settles down.
			return false;
		}
		
		if(now.isAfter(config.getMaxPositionOpenTime())) {
			// It is too late to take any more trades in market.
			return false;
		}
		
		open(position);
		return true;
	}
	
	private boolean handleOpenPosition(Position position) {
		if(2 > position.getExitTrades().size()) {
			final Set<Scrip> exitScrips = position.getExitTrades().stream()
					.map(Trade::getScrip).collect(Collectors.toSet());
			return position.getEntryTrades().stream()
				.filter(trade -> !exitScrips.contains(trade.getScrip()))
				.filter(trade -> shouldClose(trade, !exitScrips.isEmpty()))
				.map(Trade::complement)
				.map(exitTrade -> position.getExitTrades().add(exitTrade))
				.anyMatch(Predicate.isEqual(Boolean.TRUE));
		}
		return false;
	}
	
	private boolean shouldClose(Trade trade, boolean adjustStopLoss) {
		if(LocalTime.now().isAfter(config.getMaxPositionCloseTime())) return true;
		final OptionChainItem item = optionChain.findByScrip(trade.getScrip()).orElse(null);
		if(null == item) return false; 
		final double entryPrice = trade.getAveragePrice();
		final double targetPrice = entryPrice * (1 + config.getPercentageTarget() / 100);
		final double stopLossPercentage = adjustStopLoss ? 0 : config.getPercentageStopLoss();
		final double stopLossPrice = entryPrice * (1 - stopLossPercentage / 100);
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
