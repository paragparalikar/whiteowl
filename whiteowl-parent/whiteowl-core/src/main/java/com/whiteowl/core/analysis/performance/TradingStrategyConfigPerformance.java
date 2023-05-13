package com.whiteowl.core.analysis.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;
import com.whiteowl.core.trade.Trade;

import lombok.Data;

@Data
public class TradingStrategyConfigPerformance {

	private String id;
	private int totalTradeCount, winningTradeCount, losingTradeCount;
	private double cagr, maxDrawdownPct, avgDrawdownPct, avgWinPct, avgLossPct, avgReturnPctPerTrade;
	
	public TradingStrategyConfigPerformance(double initialAmount, List<Position> positions) {
		positions.sort(Comparator.comparing(Position::getCreatedDate));
		double winSum = 0, lossSum = 0, maxEquity = initialAmount, maxDrawdown = 0, drawdownSum = 0;
		totalTradeCount = positions.size();
		final double[] equity = new double[totalTradeCount];
		final double[] drawdown = new double[totalTradeCount];
		for(int index = 0; index < totalTradeCount; index++) {
			final Position position = positions.get(index);
			final double returns = Positions.getReturn(position);
			final double returnPct = returns * 100;
			equity[index] = 0 == index ? maxEquity : equity[index - 1] * returns;
			maxEquity = Math.max(maxEquity, equity[index]);
			drawdown[index] = maxEquity - equity[index];
			maxDrawdown = Math.max(maxDrawdown, drawdown[index]);
			drawdownSum += drawdown[index];
			if(0 < returnPct) {
				winSum += returnPct;
				winningTradeCount++;
			} else {
				lossSum += returnPct;
				losingTradeCount++;
			}
		}
		avgWinPct = winSum / winningTradeCount;
		avgLossPct = lossSum / losingTradeCount;
		avgReturnPctPerTrade = (winSum + lossSum) / totalTradeCount;
		maxDrawdownPct = maxDrawdown * 100 / maxEquity;
		avgDrawdownPct = (drawdownSum / totalTradeCount)  * 100 / maxEquity;
		
		final LocalDateTime startTime = positions.get(0).getCreatedDate();
		final LocalDateTime endTime = positions.get(positions.size() - 1).getExitTrades().stream()
				.map(Trade::getTimestamp).max(Comparator.naturalOrder()).orElse(LocalDateTime.now());
		final Duration duration = Duration.between(startTime, endTime);
		final double years = duration.dividedBy(Duration.ofDays(365));
		cagr = 0 == years ? 0 : Math.pow((equity[equity.length - 1] / equity[0]), 1 / years) - 1;
	}
	
	public double getCagrOverAvgDrawdown() {
		return cagr / Math.max(0.001, avgDrawdownPct);
	}
	
	public double getCagrOverMaxDrawdown() {
		return cagr / Math.max(0.001, maxDrawdownPct);
	}
	
	public double getProfitFactor() {
		return avgWinPct * winningTradeCount / avgLossPct * losingTradeCount;
	}
	
	public double getExpectancy() {
		return (avgWinPct * winningTradeCount - avgLossPct * losingTradeCount) / totalTradeCount;
	}
	
	@Override
	public String toString() {
		final String newLine = System.lineSeparator();
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%-25s : %s", "ID", id)).append(newLine);
		builder.append(String.format("%-25s : %d", "Total Trades", totalTradeCount)).append(newLine);
		builder.append(String.format("%-25s : %d", "Winning Trades", winningTradeCount)).append(newLine);
		builder.append(String.format("%-25s : %d", "Losing Trades", losingTradeCount)).append(newLine);
		builder.append(String.format("%-25s : %.2f", "Max Drawdown %", maxDrawdownPct)).append(newLine);
		builder.append(String.format("%-25s : %.2f", "Avg Drawdown %", avgDrawdownPct)).append(newLine);
		builder.append(String.format("%-25s : %.2f", "Avg Win %", avgWinPct)).append(newLine);
		builder.append(String.format("%-25s : %.2f", "Avg Loss %", avgLossPct)).append(newLine);
		builder.append(String.format("%-25s : %.2f", "Avg Return %", avgReturnPctPerTrade)).append(newLine);
		builder.append(String.format("%-25s : %.3f", "CAGR", cagr)).append(newLine);
		builder.append(String.format("%-25s : %.3f", "CAGR/MaxDD", getCagrOverMaxDrawdown())).append(newLine);
		builder.append(String.format("%-25s : %.3f", "CAGR/AvgDD", getCagrOverAvgDrawdown())).append(newLine);
		builder.append(String.format("%-25s : %.3f", "Profit Factor", getProfitFactor())).append(newLine);
		builder.append(String.format("%-25s : %.3f", "Expectancy", getExpectancy())).append(newLine);
		return builder.toString();
	}
	
}
