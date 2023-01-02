package com.whiteowl.strategy.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyPerformanceService implements TradingStrategyPerformanceService {

	@Delegate
	private final TradingStrategyPerformanceRepository repository;
	
	@Override
	public TradingStrategyPerformance calculate(
			@NonNull final List<Bar> bars,
			@NonNull final List<Position> positions,
			@NonNull final List<Double> equityCurve,
			@NonNull final TradingStrategyConfig config) {
		int openPositionCount = 0;
		int closedPositionCount = 0;
		int losingPositionCount = 0;
		int winningPositionCount = 0;
		int breakEventPositionCount = 0;
		int totalPositionCount = positions.size();
		int netHoldingBarCount = 0;
		double netLossAmount = 0;
		double netProfitAmount = 0;
		double netProfitLossAmount = 0;
		double lossPercentageSum = 0;
		double profitPercentageSum = 0;
		double profitLossPercentageSum = 0;
		double maxLossAmount = 0;
		double maxProfitAmount = 0;
		double maxLossPercentage = 0;
		double maxProfitPercentage = 0;
		final double initialCapital = equityCurve.get(0);
		final double endCapital = equityCurve.get(equityCurve.size() - 1);
		
		final Bar firstBar = bars.get(0);
		final Bar lastBar = bars.get(bars.size() - 1);
		final Map<LocalDateTime, Integer> timestampIndices = new HashMap<>();
		for(int index = 0; index < bars.size(); index++) {
			timestampIndices.put(bars.get(index).getEndTime().toLocalDateTime(), index);
		}
		
		for(Position position : positions) {
			final LocalDateTime entryTime = position.getEntryTrades().stream()
					.map(Trade::getTimestamp)
					.min(Comparator.naturalOrder())
					.orElseThrow();
			final LocalDateTime exitTime = position.getExitTrades().stream()
					.map(Trade::getTimestamp)
					.max(Comparator.naturalOrder())
					.orElseGet(lastBar.getEndTime()::toLocalDateTime);
			final Integer entryIndex = timestampIndices.get(entryTime);
			final Integer exitIndex = timestampIndices.get(exitTime);
			netHoldingBarCount += exitIndex - entryIndex;
			
			double entryAmount = position.getEntryAmount();
			final double profitLossAmount = position.getProfitLossAmount();
			netProfitLossAmount += profitLossAmount;
			final double profitLossPercentage = profitLossAmount * 100 / entryAmount;
			profitLossPercentageSum += profitLossPercentage;
			if(0 < profitLossAmount) {
				winningPositionCount++;
				netProfitAmount += profitLossAmount;
				profitPercentageSum += profitLossPercentage;
				maxProfitAmount = Math.max(maxProfitAmount, profitLossAmount);
				maxProfitPercentage = Math.max(maxProfitPercentage, profitLossPercentage);
			} else if(0 > profitLossAmount) {
				losingPositionCount++;
				netLossAmount += profitLossAmount;
				lossPercentageSum += profitLossPercentage;
				maxLossAmount = Math.max(maxLossAmount, profitLossAmount);
				maxLossPercentage = Math.max(maxLossPercentage, profitLossPercentage);
			} else {
				breakEventPositionCount++;
			}
			if(PositionStatus.CLOSED.equals(position.getStatus())) {
				closedPositionCount++;
			} else {
				openPositionCount++;
			}
		}
		
		final double buyAndHoldProfitLossAmount = lastBar.getClosePrice().minus(firstBar.getOpenPrice()).doubleValue();
		final double buyAndHoldProfitLossPercentage = buyAndHoldProfitLossAmount * 100 / firstBar.getOpenPrice().doubleValue();
		final int averageHoldingBarCount = netHoldingBarCount / totalPositionCount;
		final double holdingBarCountPercentage = netHoldingBarCount * 100 / bars.size();
		final double netLossPercentage = netLossAmount * 100 / initialCapital;
		final double netProfitPercentage = netProfitAmount * 100 / initialCapital;
		final double netProfitLossPercentage = netProfitLossAmount * 100 / initialCapital;
		final double averageLossAmount = netLossAmount / losingPositionCount;
		final double averageProfitAmount = netProfitAmount / winningPositionCount;
		final double averageProfitLossAmount = netProfitLossAmount / totalPositionCount;
		final double averageLossPercentage = lossPercentageSum / losingPositionCount;
		final double averageProfitPercentage = profitPercentageSum / winningPositionCount;
		final double averageProfitLossPercentage = profitLossPercentageSum / totalPositionCount;
		final double winRatio = winningPositionCount / totalPositionCount;
		final double lossRatio = losingPositionCount / totalPositionCount;
		final double expectancy = ((1 + (averageProfitAmount / averageLossAmount)) * winRatio) - 1;
		final double profitFactor = netProfitAmount / netLossAmount;
		final double years = Duration.between(lastBar.getEndTime(), firstBar.getBeginTime()).abs().toMinutes() / 60 * 24 * 365;
		final double cagr = Math.pow((endCapital / initialCapital), 1 / years) - 1;
		
		return TradingStrategyPerformance.builder()
				.initialCapital(initialCapital)
				.endCapital(endCapital)
				.totalPositionCount(totalPositionCount)
				.openPositionCount(openPositionCount)
				.closedPositionCount(closedPositionCount)
				.losingPositionCount(losingPositionCount)
				.winningPositionCount(winningPositionCount)
				.breakEventPositionCount(breakEventPositionCount)
				.netHoldingBarCount(netHoldingBarCount)
				.averageHoldingBarCount(averageHoldingBarCount)
				.holdingBarCountPercentage(holdingBarCountPercentage)
				.netLossAmount(netLossAmount)
				.netProfitAmount(netProfitAmount)
				.netProfitLossAmount(netProfitLossAmount)
				.buyAndHoldProfitLossAmount(buyAndHoldProfitLossAmount)
				.netLossPercentage(netLossPercentage)
				.netProfitPercentage(netProfitPercentage)
				.netProfitLossPercentage(netProfitLossPercentage)
				.buyAndHoldProfitLossPercentage(buyAndHoldProfitLossPercentage)
				.averageLossAmount(averageLossAmount)
				.averageProfitAmount(averageProfitAmount)
				.averageProfitLossAmount(averageProfitLossAmount)
				.averageLossPercentage(averageLossPercentage)
				.averageProfitPercentage(averageProfitPercentage)
				.averageProfitLossPercentage(averageProfitLossPercentage)
				.maxLossAmount(maxLossAmount)
				.maxProfitAmount(maxProfitAmount)
				.maxLossPercentage(maxLossPercentage)
				.maxProfitPercentage(maxProfitPercentage)
				.winRatio(winRatio)
				.lossRatio(lossRatio)
				.expectancy(expectancy)
				.profitFactor(profitFactor)
				.cagr(cagr)
				.build();
	}
	
	
}
