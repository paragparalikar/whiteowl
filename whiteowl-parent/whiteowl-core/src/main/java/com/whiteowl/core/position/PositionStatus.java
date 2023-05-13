package com.whiteowl.core.position;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionStatus {

	NEW(false), 
	OPEN(false), 
	CLOSED(true);
	
	private final boolean terminal;
	
	public static void update(Position position) {
		if(position.getStatus().isTerminal()) return;
		final Set<Trade> exitTrades = position.getExitTrades();
		final Set<Trade> entryTrades = position.getEntryTrades();
		if(entryTrades.isEmpty()) {
			position.setStatus(PositionStatus.NEW);
			return;
		} else if(entryTrades.stream().map(Trade::getStatus).allMatch(Predicate.isEqual(TradeStatus.NEW))) {
			position.setStatus(PositionStatus.NEW);
			return;
		} else if(entryTrades.stream().map(Trade::getStatus).allMatch(Predicate.isEqual(TradeStatus.CANCELLED))) {
			position.setStatus(PositionStatus.CLOSED);
			return;
		} else if(entryTrades.stream().map(Trade::getStatus).allMatch(Predicate.isEqual(TradeStatus.REJECTED))) {
			position.setStatus(PositionStatus.CLOSED);
			return;
		} else if(entryTrades.stream().map(Trade::getStatus).allMatch(TradeStatus::isTerminal)) {
			if(exitTrades.isEmpty()) {
				position.setStatus(PositionStatus.OPEN);
				return;
			} else if(exitTrades.stream().map(Trade::getStatus).allMatch(TradeStatus::isTerminal)) {
				final Map<Scrip, Integer> buyQuantities = Stream.concat(entryTrades.stream(), exitTrades.stream())
						.filter(trade -> TradeType.BUY.equals(trade.getType()))
						.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
						.collect(Collectors.groupingBy(Trade::getScrip, Collectors.summingInt(Trade::getFilledQuantity)));
				final Map<Scrip, Integer> sellQuantities = Stream.concat(entryTrades.stream(), exitTrades.stream())
						.filter(trade -> TradeType.SELL.equals(trade.getType()))
						.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
						.collect(Collectors.groupingBy(Trade::getScrip, Collectors.summingInt(Trade::getFilledQuantity)));
				if(buyQuantities.equals(sellQuantities)) {
					position.setStatus(PositionStatus.CLOSED);
					return;
				} else {
					position.setStatus(PositionStatus.OPEN);
					return;
				}
			} else {
				position.setStatus(PositionStatus.OPEN);
				return;
			}
		} else {
			position.setStatus(PositionStatus.OPEN);
			return;
		}
	}
	
}
