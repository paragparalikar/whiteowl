package com.whiteowl.core.analysis.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;
import com.whiteowl.core.trade.Trade;

import lombok.Data;

@Data
@Entity
public class TradingStrategyConfigPerformance {

	@Id
	private String id;
	private double[] equity, drawdown;
	private int totalTradeCount, winningTradeCount, losingTradeCount, openTradeCount;
	private double cagr, maxDrawdownPct, avgDrawdownPct, avgWinPct, avgLossPct, avgReturnPctPerTrade;
	
	public TradingStrategyConfigPerformance(double initialAmount, List<Position> positions) {
		double winSum = 0, lossSum = 0, maxEquity = initialAmount;
		totalTradeCount = positions.size();
		equity = new double[totalTradeCount];
		drawdown = new double[totalTradeCount];
		for(int index = 0; index < totalTradeCount; index++) {
			final Position position = positions.get(index);
			if(Positions.isOpen(position)) {
				openTradeCount++;
			} else {
				final double returns = Positions.getReturn(position);
				final double returnPct = returns * 100;
				equity[index] = 0 == index ? maxEquity : equity[index - 1] * returns;
				maxEquity = Math.max(maxEquity, equity[index]);
				drawdown[index] = maxEquity - equity[index];
				if(0 < returnPct) {
					winSum += returnPct;
					winningTradeCount++;
				} else {
					lossSum += returnPct;
					losingTradeCount++;
				}
			}
		}
		avgWinPct = winSum / winningTradeCount;
		avgLossPct = lossSum / losingTradeCount;
		avgReturnPctPerTrade = (winSum + lossSum) / totalTradeCount;
		maxDrawdownPct = Arrays.stream(drawdown).max().orElse(0) * 100 / maxEquity;
		
		final LocalDateTime startTime = positions.get(0).getEntryTrades().stream()
				.map(Trade::getTimestamp).min(Comparator.naturalOrder()).orElse(LocalDateTime.now());
		final LocalDateTime endTime = positions.get(positions.size() - 1).getExitTrades().stream()
				.map(Trade::getTimestamp).max(Comparator.naturalOrder()).orElse(LocalDateTime.now());
		final Duration duration = Duration.between(startTime, endTime);
		final double years = duration.dividedBy(Duration.ofDays(365));
		cagr = Math.pow((equity[equity.length - 1] / equity[0]), 1 / years) - 1;
	}
	
	public double getCagrOverAvgDrawdown() {
		return cagr / Math.max(0.001, avgDrawdownPct);
	}
	
	public double getProfitFactor() {
		return avgWinPct * winningTradeCount / avgLossPct * losingTradeCount;
	}
	
	public double getExpectancy() {
		return (avgWinPct * winningTradeCount - avgLossPct * losingTradeCount) / totalTradeCount;
	}
	
}
