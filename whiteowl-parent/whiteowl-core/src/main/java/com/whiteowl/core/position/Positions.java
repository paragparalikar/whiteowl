package com.whiteowl.core.position;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Positions {

	public Stream<Trade> trades(Position position){
		return Stream.concat(position.getEntryTrades().stream(), position.getExitTrades().stream());
	}
	
	public boolean isLong(Position position) {
		return position.getEntryTrades().stream().map(Trade::getType).allMatch(Predicate.isEqual(TradeType.BUY));
	}
	
	public boolean isShort(Position position) {
		return position.getEntryTrades().stream().map(Trade::getType).allMatch(Predicate.isEqual(TradeType.SELL));
	}
	
	public int balanceQuantity(Scrip scrip, Position position) {
		return quantity(scrip, position, TradeType.BUY) - quantity(scrip, position, TradeType.SELL);
	}
	
	public int quantity(Scrip scrip, Position position, TradeType tradeType) {
		return trades(position)
				.filter(trade -> Objects.equals(trade.getScrip(), scrip))
				.filter(trade -> tradeType.equals(trade.getType()))
				.map(Trade::getQuantity)
				.collect(Collectors.summingInt(Integer::intValue));
	}
	
	public double entryAmount(Position position) {
		return position.getEntryTrades().stream()
				.filter(trade -> !TradeStatus.REJECTED.equals(trade.getStatus()))
				.filter(trade -> !TradeStatus.CANCELLED.equals(trade.getStatus()))
				.map(Trade::getAmount)
				.collect(Collectors.summingDouble(Double::doubleValue));
	}
	
	public int entryQuantity(Position position) {
		return position.getEntryTrades().stream()
				.map(Trade::getFilledQuantity)
				.collect(Collectors.summingInt(Integer::intValue));
	}
	
	public double averageEntryPrice(Position position) {
		return entryAmount(position) / entryQuantity(position);
	}

}
