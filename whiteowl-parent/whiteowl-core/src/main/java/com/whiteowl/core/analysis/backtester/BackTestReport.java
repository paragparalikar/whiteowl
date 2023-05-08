package com.whiteowl.core.analysis.backtester;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.trade.Trade;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class BackTestReport {
	
	public static double computeReturnsPct(Position position, TradeType tradeType) {
		if(!position.getStatus().isTerminal()) return 0;
		final double exitPrice = position.getExitTrades().stream().collect(Collectors.summingDouble(Trade::getAveragePrice));
		final double entryPrice = position.getEntryTrades().stream().collect(Collectors.summingDouble(Trade::getAveragePrice));
		final double returns = (exitPrice - entryPrice) * 100 / entryPrice;
		return TradeType.BUY.equals(tradeType) ? returns : -1 * returns;
	}
	
	public static double computeAnnualReturnsPct(TradingStrategyConfig config, Duration duration, List<Position> positions) {
		final double returnsPct = positions.stream()
				.collect(Collectors.summingDouble(position -> computeReturnsPct(position, config.getTradeType())));
		final double multiple = Double.valueOf(Duration.ofDays(365).toMinutes()) / Double.valueOf(duration.toMinutes());
		return multiple * returnsPct;
	}

	private final Duration duration;
	private final List<Position> positions;
	private final TradingStrategyConfig config;
	private final double annualReturnsPct;
	
	@Builder
	public BackTestReport(
			@NonNull Duration duration,
			@NonNull List<Position> positions, 
			@NonNull TradingStrategyConfig config) {
		this.positions = positions;
		this.duration = duration;
		this.config = config;
		
		annualReturnsPct = computeAnnualReturnsPct(config, duration, positions);
	}

}
