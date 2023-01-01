package com.whiteowl.strategy.performance;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyPerformanceService implements TradingStrategyPerformanceService {

	@Delegate
	private final TradingStrategyPerformanceRepository repository;
	
	public TradingStrategyPerformance calculate(
			final double initialCapital,
			final double endCapital,
			@NonNull final List<Bar> bars,
			@NonNull final List<Position> positions,
			@NonNull final TradingStrategyConfig config) {
		int openPositionCount = 0;
		int closedPositionCount = 0;
		int losingPositionCount = 0;
		int winningPositionCount = 0;
		int breakEventPositionCount = 0;
		int totalPositionCount = positions.size();
		int netHoldingTimeInMinutes = 0;
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
		
		for(Position position : positions) {
			double entryAmount = position.getEntryAmount();
			netHoldingTimeInMinutes += position.getHoldingTimeInMinutes();
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
		
		final Bar firstBar = bars.get(0);
		final Bar lastBar = bars.get(bars.size() - 1);
		final int totalTimeInMinutes = (int) Duration.between(
				firstBar.getBeginTime(), 
				lastBar.getEndTime())
				.abs().toMinutes();
		final double buyAndHoldProfitLossAmount = lastBar.getClosePrice().minus(firstBar.getOpenPrice()).doubleValue();
		final double buyAndHoldProfitLossPercentage = buyAndHoldProfitLossAmount * 100 / firstBar.getOpenPrice().doubleValue();
		final int averageHoldingTimeInMinutes = netHoldingTimeInMinutes / totalPositionCount;
		final double holdingTimeInMinutesPercentage = netHoldingTimeInMinutes * 100 / totalTimeInMinutes;
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
		final double netHoldingTimeInYears = netHoldingTimeInMinutes / 60 * 24 * 365;
		final double cagr = Math.pow((endCapital / initialCapital), 1 / netHoldingTimeInYears) - 1;
		
		
		return TradingStrategyPerformance.builder()
				.totalPositionCount(totalPositionCount)
				.openPositionCount(openPositionCount)
				.closedPositionCount(closedPositionCount)
				.losingPositionCount(losingPositionCount)
				.winningPositionCount(winningPositionCount)
				.breakEventPositionCount(breakEventPositionCount)
				.netHoldingTimeInMinutes(netHoldingTimeInMinutes)
				.averageHoldingTimeInMinutes(averageHoldingTimeInMinutes)
				.holdingTimeInMinutesPercentage(holdingTimeInMinutesPercentage)
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
