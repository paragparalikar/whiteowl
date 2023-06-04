package com.whiteowl.analysis.performance;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.whiteowl.analysis.backtester.listener.BacktestListener;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;
import com.whiteowl.core.trade.Trade;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class TradingStrategyConfigPerformance implements BacktestListener, Serializable {
	private static final long serialVersionUID = 7714959761833354015L;

	private final double initialMargin;
	private final String id = UUID.randomUUID().toString();
	private final List<Double> equity = new ArrayList<>();
	private final List<Double> drawdown = new ArrayList<>();
	private final List<Position> positions = new ArrayList<>();
	private int totalTradeCount, winningTradeCount, losingTradeCount, openTradeCount;
	private double cagr, exposure, maxDrawdownPct, avgDrawdownPct, avgWinPct, avgLossPct, avgReturnPctPerTrade;
	@Getter(AccessLevel.PROTECTED) private double maxEquity, maxDrawdown;
	@Getter(AccessLevel.PROTECTED) private Duration exposureDuration = Duration.ofSeconds(0);
	
	public TradingStrategyConfigPerformance(double initialMargin) {
		this.initialMargin = initialMargin;
	}
	
	public TradingStrategyConfigPerformance(double initialMargin, List<Position> positions) {
		this(initialMargin);
		positions.sort(Comparator.comparing(Position::getCreatedDate));
		positions.forEach(this::onExit);
	}
	
	@Override
	public synchronized void onExit(Position position) {
		positions.add(position);
		totalTradeCount++;
		if(Positions.isOpen(position)) {
			openTradeCount++;
			return;
		}
		
		final double entryAmount = Positions.entryAmount(position);
		final double exitAmount = Positions.exitAmount(position);
		final double returns = entryAmount + exitAmount;
		final double returnsPct = returns * 100 / Math.abs(entryAmount);
		
		final double previousEquity = equity.isEmpty() ? maxEquity = initialMargin : equity.get(equity.size() - 1);
		final double currentEquity = previousEquity + returns;
		equity.add(currentEquity);
		maxEquity = Math.max(maxEquity, currentEquity);
		
		final double currentDrawdown = Math.max(0, maxEquity - currentEquity);
		drawdown.add(currentDrawdown);
		final double currentAvgDrawdownPct = currentDrawdown * 100 / maxEquity;
		avgDrawdownPct = (avgDrawdownPct * (drawdown.size() - 1) + currentAvgDrawdownPct) / drawdown.size();
		if(currentDrawdown > maxDrawdown) {
			maxDrawdown = currentDrawdown;
			maxDrawdownPct = maxDrawdown * 100 / maxEquity;
		}
		if(0 < returns) {
			winningTradeCount++;
			avgWinPct = (avgWinPct * (winningTradeCount - 1) + returnsPct) / winningTradeCount;
		} else {
			losingTradeCount++;
			avgLossPct = (avgLossPct * (losingTradeCount - 1) + returnsPct) / losingTradeCount;
		}
		avgReturnPctPerTrade = (avgReturnPctPerTrade * (totalTradeCount - 1) + returnsPct) / totalTradeCount;
	
		final LocalDateTime firstStartTime = positions.get(0).getCreatedDate();
		final LocalDateTime currentStartTime = resolveStartTime(position);
		final LocalDateTime currentEndTime = resolveEndTime(position);
		final Duration duration = Duration.between(firstStartTime, currentEndTime);
		final Duration currentDuration = Duration.between(currentStartTime, currentEndTime);
		final double years = ((double)(duration.toMillis())) / ((double)(Duration.ofDays(365).toMillis()));
		cagr = 0 == years ? 0 : Math.pow(currentEquity / initialMargin, 1 / years) - 1;
		exposureDuration = exposureDuration.plus(currentDuration);
		exposure = ((double)exposureDuration.toMillis()) / ((double)duration.toMillis());
	}
	
	private LocalDateTime resolveStartTime(Position position) {
		return position.getEntryTrades().stream()
				.map(Trade::getTimestamp)
				.min(Comparator.naturalOrder())
				.orElse(LocalDateTime.now());
	}
	
	private LocalDateTime resolveEndTime(Position position) {
		return position.getExitTrades().stream()
				.map(Trade::getTimestamp)
				.max(Comparator.nullsFirst(Comparator.naturalOrder()))
				.orElseGet(() -> position.getEntryTrades().stream()
						.map(Trade::getTimestamp)
						.max(Comparator.nullsFirst(Comparator.naturalOrder()))
						.orElse(LocalDateTime.now()));
	}
	
	public double getCagrOverAvgDrawdown() {
		return cagr / Math.max(0.001, avgDrawdownPct);
	}
	
	public double getCagrOverAvgDrawdownAndExposure() {
		return cagr / (Math.max(0.001, avgDrawdownPct) * exposure);
	}
	
	public double getCagrOverMaxDrawdown() {
		return cagr / Math.max(0.001, maxDrawdownPct);
	}
	
	public double getProfitFactor() {
		return (avgWinPct * winningTradeCount) / ( -1 * avgLossPct * losingTradeCount);
	}
	
	public double getExpectancy() {
		return (avgWinPct * winningTradeCount + avgLossPct * losingTradeCount) / totalTradeCount;
	}
	
	public double getProfitablePct() {
		return winningTradeCount * 100 / totalTradeCount;
	}
	
	@Override
	public String toString() {
		final String newLine = System.lineSeparator();
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%-16s : %s", "ID", id)).append(newLine);
		builder.append(String.format("%-16s : %d", "Total Trades", totalTradeCount)).append(newLine);
		builder.append(String.format("%-16s : %d", "Winning Trades", winningTradeCount)).append(newLine);
		builder.append(String.format("%-16s : %d", "Losing Trades", losingTradeCount)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Initial Margin", initialMargin)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "End Equity", equity.get(equity.size() - 1))).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Profitable %", getProfitablePct())).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Max Drawdown %", maxDrawdownPct)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Avg Drawdown %", avgDrawdownPct)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Avg Win %", avgWinPct)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Avg Loss %", avgLossPct)).append(newLine);
		builder.append(String.format("%-16s : %.2f", "Avg Return %", avgReturnPctPerTrade)).append(newLine);
		builder.append(String.format("%-16s : %.3f", "Exposure", exposure)).append(newLine);
		builder.append(String.format("%-16s : %.3f", "CAGR", cagr)).append(newLine);
		builder.append(String.format("%-16s : %.3f", "CAGR/MaxDD", getCagrOverMaxDrawdown())).append(newLine);
		builder.append(String.format("%-16s : %.3f", "CAGR/AvgDD", getCagrOverAvgDrawdown())).append(newLine);
		builder.append(String.format("%-16s : %.3f", "Profit Factor", getProfitFactor())).append(newLine);
		builder.append(String.format("%-16s : %.3f", "Expectancy", getExpectancy())).append(newLine);
		return builder.toString();
	}
	
}
