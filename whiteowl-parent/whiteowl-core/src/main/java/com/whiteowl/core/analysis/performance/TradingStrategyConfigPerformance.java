package com.whiteowl.core.analysis.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;
import com.whiteowl.core.trade.Trade;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TradingStrategyConfigPerformance implements Consumer<Position> {

	private final double initialMargin;
	private final String id = UUID.randomUUID().toString();
	private final List<Double> equity = new ArrayList<>();
	private final List<Double> drawdown = new ArrayList<>();
	private final List<Position> positions = new ArrayList<>();
	private int totalTradeCount, winningTradeCount, losingTradeCount;
	private double cagr, maxDrawdownPct, avgDrawdownPct, avgWinPct, avgLossPct, avgReturnPctPerTrade;
	@Getter(AccessLevel.PROTECTED) private double maxEquity, maxDrawdown, drawdownSum;
	
	@Override
	public synchronized void accept(Position position) {
		positions.add(position);
		totalTradeCount++;
		final double returns = 1 + Positions.getReturn(position);
		final double previousEquity = equity.isEmpty() ? initialMargin : equity.get(equity.size() - 1);
		final double currentEquity = previousEquity * returns;
		equity.add(currentEquity);
		maxEquity = Math.max(maxEquity, currentEquity);
		final double currentDrawdown = Math.max(0, maxEquity - currentEquity);
		drawdown.add(currentDrawdown);
		drawdownSum += currentDrawdown;
		maxDrawdown = Math.max(maxDrawdown, currentDrawdown);
		maxDrawdownPct = maxDrawdown * 100 / maxEquity;
		if(1 < returns) {
			winningTradeCount++;
			avgWinPct = (avgWinPct * (winningTradeCount - 1) + (returns - 1) * 100) / winningTradeCount;
		} else {
			losingTradeCount++;
			avgLossPct = (avgLossPct * (losingTradeCount - 1) + (1 - returns) * 100) / losingTradeCount;
		}
		avgReturnPctPerTrade = (avgReturnPctPerTrade * (totalTradeCount - 1) + (returns - 1) * 100) / totalTradeCount;
	
		final LocalDateTime startTime = positions.get(0).getCreatedDate();
		final LocalDateTime endTime = positions.get(positions.size() - 1).getExitTrades().stream()
				.map(Trade::getTimestamp).max(Comparator.naturalOrder()).orElse(LocalDateTime.now());
		final Duration duration = Duration.between(startTime, endTime);
		final double years = duration.dividedBy(Duration.ofDays(365));
		cagr = 0 == years ? 0 : Math.pow((equity.get(equity.size() - 1) / equity.get(0)), 1 / years) - 1;
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
