package com.whiteowl.strategy.derivative.option.shortstrangle;

import java.time.LocalTime;
import java.util.Optional;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.trade.TradeVariety;
import com.whiteowl.strategy.TradingStrategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShortStrangleTradingStrategy implements TradingStrategy {

	private final Scrip scrip;
	private final BarSeries series;
	private final OptionChain optionChain;
	private final ShortStrangleConfig config;
	
	public void run(Position position) {
		final LocalTime now = LocalTime.now();
		if(now.isBefore(config.getMinPositionOpenTime())) {
			
		} else if(now.isBefore(config.getMaxPositionOpenTime())) {
			
		} else if(now.isAfter(config.getMaxPositionCloseTime())) {
			
		}
	}
	
	private void close(Position position) {
		position.getEntryTrades().stream()
			.map(Trade::complement)
			.forEach(position.getExitTrades()::add);
	}
	
	private void open(Position position) {
		createCallEntryTrade().ifPresent(callEntryTrade -> {
			createPutEntryTrade().ifPresent(putEntryTrade -> {
				position.setScrip(scrip);
				position.setStatus(PositionStatus.NEW);
				position.setTradingStrategyConfigId(config.getId());
				position.getEntryTrades().add(callEntryTrade);
				position.getEntryTrades().add(putEntryTrade);
			});
		});
	}
	
	private Optional<Trade> createCallEntryTrade() {
		return optionChain.findByDelta(config.getCallDelta())
			.map(optionChainItem -> Trade.builder()
				.limitType(TradeLimitType.MARKET)
				.product(TradeProduct.MIS)
				.quantity(config.getCallQuantity())
				.scrip(optionChainItem.getScrip())
				.status(TradeStatus.NEW)
				.type(TradeType.SELL)
				.validity(TradeValidity.DAY)
				.variety(TradeVariety.REGULAR)
				.build());
	}
	
	private Optional<Trade> createPutEntryTrade() {
		return optionChain.findByDelta(config.getPutDelta())
			.map(optionChainItem -> Trade.builder()
				.limitType(TradeLimitType.MARKET)
				.product(TradeProduct.MIS)
				.quantity(config.getPutQuantity())
				.scrip(optionChainItem.getScrip())
				.status(TradeStatus.NEW)
				.type(TradeType.SELL)
				.validity(TradeValidity.DAY)
				.variety(TradeVariety.REGULAR)
				.build());
	}
	
}
