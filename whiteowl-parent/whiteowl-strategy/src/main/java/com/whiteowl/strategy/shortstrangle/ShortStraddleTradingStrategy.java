package com.whiteowl.strategy.shortstrangle;

import java.time.LocalTime;
import java.util.Optional;

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
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ShortStraddleTradingStrategy {

	private Position createNewPosition(Scrip scrip, TradingStrategyConfig config) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setStatus(PositionStatus.NEW);
		position.setTradingStrategyConfigId(config.getId());
		return position;
	}
	
	private final OptionChain optionChain;
	private final ShortStraddleConfig config;
	
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
			log.debug("We are not allowed to take any trades just yet. Current time {}, min open time {}",
					now, config.getMinPositionOpenTime());
			return false;
		}
		
		if(now.isAfter(config.getMaxPositionOpenTime())) {
			log.debug("It is too late to take any more trades in market. Current time {}, max open time {}", 
					now, config.getMaxPositionOpenTime());
			return false;
		}
		
		open(position);
		
		log.info("Opening a new position for {} with config {} and template {}", 
				optionChain.getUnderlying().getName(), config.getId(), config.getTemplate());
		return true;
	}
	
	private boolean handleOpenPosition(Position position) {
		final LocalTime now = LocalTime.now();
		if(position.getEntryTrades().size() == position.getExitTrades().size()) {
			// All entry trades have their corresponding exit trades, nothing to do here.
			return false;
		}
		if(now.isAfter(config.getMaxPositionCloseTime())) {
			boolean result = false;
			// It is post max time to keep the trade open. 
			//All open trades must be closed at this point.
			for(Trade entryTrade : position.getEntryTrades()) {
				if(position.hasExitTrade(entryTrade.getScrip())) {
					continue;	
				} else if(TradeStatus.NEW.equals(entryTrade.getStatus())) {
					entryTrade.setStatus(TradeStatus.CANCELLED);
				} else if(TradeStatus.COMPLETE.equals(entryTrade.getStatus())) {
					position.getExitTrades().add(entryTrade.complement());
					result = true;
				} else if(!entryTrade.getStatus().isTerminal()) {
					entryTrade.setStatus(TradeStatus.CANCELLABLE);
					result = true;
				} 
			}
			return result;
		} else {
			boolean result = false;
			for(Trade entryTrade : position.getEntryTrades()) {
				if(position.hasExitTrade(entryTrade.getScrip())) {
					continue;	
				} else if(!TradeStatus.COMPLETE.equals(entryTrade.getStatus())) {
					// Current entry is not completed yet, it could be CANCELLED, REJECTED or 
					// one of the actionable states like NEW, CANCELLABLE, UPDATABLE.
					// We can only act on completed trades.
					continue;
				} else {
					final OptionChainItem item = optionChain.findByScrip(entryTrade.getScrip()).orElse(null);
					if(null == item) {
						// We could not procure the option chain, so we are in the dark now.
						// Do not take any action unless we have some data to base our decision.
						return false; 
					}
					final double entryPrice = entryTrade.getAveragePrice();
					final double targetPrice = entryPrice * (1 + config.getPercentageTarget() / 100);
					final double stopLossPercentage = position.getExitTrades().isEmpty() ? 
							config.getPercentageStopLoss() : 0;
					final double stopLossPrice = entryPrice * (1 - stopLossPercentage / 100);
					final double currentPrice = item.getOptionInfo(entryTrade.getScrip().getType()).getLastPrice();
					if(currentPrice >= targetPrice || currentPrice <= stopLossPrice) {
						position.getExitTrades().add(entryTrade.complement());
					}
				}
			}
			return result;
		}
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
