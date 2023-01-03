package com.whiteowl.strategy.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.num.DoubleNum;

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
		double openPositionCount = 0;
		double closedPositionCount = 0;
		double losingPositionCount = 0;
		double winningPositionCount = 0;
		double breakEventPositionCount = 0;
		double netHoldingBarCount = 0;
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
		final double totalPositionCount = positions.size();
		final double initialCapital = equityCurve.get(0);
		final double endCapital = equityCurve.get(equityCurve.size() - 1);
		
		double maxAmount = Double.MIN_VALUE;
		double minAmount = Double.MAX_VALUE;
		double maxDrawDown = 0;
		final Bar firstBar = bars.get(0);
		final Bar lastBar = bars.get(bars.size() - 1);
		final Map<LocalDateTime, Integer> timestampIndices = new HashMap<>();
		for(int index = 0; index < bars.size(); index++) {
			timestampIndices.put(bars.get(index).getEndTime().toLocalDateTime(), index);
			double amount = equityCurve.get(index);
			if(amount > maxAmount) {
				maxAmount = amount;
				minAmount = amount;
			}
			minAmount = Math.min(minAmount, amount);
			maxDrawDown = Math.max(maxDrawDown, maxAmount - minAmount);
		}
		
		for(Position position : positions) {
			final LocalDateTime entryTime = position.getEntryTrades().stream()
					.map(Trade::getTimestamp)
					.filter(Objects::nonNull)
					.min(Comparator.naturalOrder())
					.orElseThrow();
			final LocalDateTime exitTime = position.getExitTrades().stream()
					.map(Trade::getTimestamp)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder())
					.orElse(entryTime);
			final Integer entryIndex = timestampIndices.get(entryTime);
			final Integer exitIndex = timestampIndices.get(exitTime);
			netHoldingBarCount += exitIndex - entryIndex;
			
			double entryAmount = position.getEntryAmount();
			final double profitLossAmount = PositionStatus.CLOSED.equals(position.getStatus()) ? position.getProfitLossAmount() : 
				position.getOnBalanceQuantity(position.getScrip()) * lastBar.getClosePrice().doubleValue() - entryAmount;
			netProfitLossAmount += profitLossAmount;
			final double profitLossPercentage = 0 == entryAmount ? 0 : profitLossAmount * 100 / entryAmount;
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
				maxLossAmount = Math.min(maxLossAmount, profitLossAmount);
				maxLossPercentage = Math.min(maxLossPercentage, profitLossPercentage);
				if(-100 == maxLossPercentage) {
					System.out.println("maxLossPercentage " + maxLossPercentage);
				}
			} else {
				breakEventPositionCount++;
			}
			if(PositionStatus.CLOSED.equals(position.getStatus())) {
				closedPositionCount++;
			} else {
				openPositionCount++;
			}
		}
		
		final double buyAndHoldProfitLossAmount = lastBar.getClosePrice().multipliedBy(DoubleNum.valueOf(initialCapital)).dividedBy(firstBar.getOpenPrice()).doubleValue();
		final double buyAndHoldProfitLossPercentage = buyAndHoldProfitLossAmount * 100 / initialCapital;
		final double averageHoldingBarCount = netHoldingBarCount / totalPositionCount;
		final double holdingBarCountPercentage = netHoldingBarCount * 100 / bars.size();
		final double netLossPercentage = netLossAmount * 100 / initialCapital;
		final double netProfitPercentage = netProfitAmount * 100 / initialCapital;
		final double netProfitLossPercentage = netProfitLossAmount * 100 / initialCapital;
		final double averageLossAmount = netLossAmount / losingPositionCount;
		final double averageProfitAmount = netProfitAmount / winningPositionCount;
		final double averageLossPercentage = lossPercentageSum / losingPositionCount;
		final double averageProfitPercentage = profitPercentageSum / winningPositionCount;
		final double averageProfitLossPercentage = profitLossPercentageSum / totalPositionCount;
		final double maxDrawDownPercentage = maxDrawDown * 100 / maxAmount;
		final double winRatio = winningPositionCount / totalPositionCount;
		final double lossRatio = losingPositionCount / totalPositionCount;
		final double expectancy = ((1 + (averageProfitAmount / averageLossAmount)) * winRatio) - 1;
		final double profitFactor = netProfitAmount / netLossAmount;
		final double years = Duration.between(lastBar.getEndTime(), firstBar.getBeginTime()).abs().toMinutes() / (60 * 24 * 365);
		final double cagr = Math.pow((endCapital / initialCapital), 1 / years) - 1;
		final double romad = (endCapital - initialCapital) / maxDrawDown;
		final double riskFreeReturn = initialCapital * Math.pow((1d + 6d/100d), years) - initialCapital;
		final double riskFreeReturnPercentage = riskFreeReturn * 100 / initialCapital;
		final double calmarRatio = (netProfitLossPercentage - riskFreeReturnPercentage) / (maxDrawDownPercentage * years);
		
		return TradingStrategyPerformance.builder()
				.initialCapital(initialCapital)
				.endCapital(endCapital)
				.totalPositionCount((int) totalPositionCount)
				.openPositionCount((int) openPositionCount)
				.closedPositionCount((int) closedPositionCount)
				.losingPositionCount((int) losingPositionCount)
				.winningPositionCount((int) winningPositionCount)
				.breakEventPositionCount((int) breakEventPositionCount)
				.averageHoldingBarCount((int) averageHoldingBarCount)
				.holdingBarCountPercentage(holdingBarCountPercentage)
				.netLossPercentage(netLossPercentage)
				.netProfitPercentage(netProfitPercentage)
				.netProfitLossPercentage(netProfitLossPercentage)
				.buyAndHoldProfitLossPercentage(buyAndHoldProfitLossPercentage)
				.averageLossPercentage(averageLossPercentage)
				.averageProfitPercentage(averageProfitPercentage)
				.averageProfitLossPercentage(averageProfitLossPercentage)
				.maxLossPercentage(maxLossPercentage)
				.maxProfitPercentage(maxProfitPercentage)
				.winRatio(winRatio)
				.lossRatio(lossRatio)
				.expectancy(expectancy)
				.profitFactor(profitFactor)
				.cagr(cagr)
				.maxDrawdownPercentage(maxDrawDownPercentage)
				.romad(romad)
				.riskFreeReturnPercentage(riskFreeReturnPercentage)
				.calmarRatio(calmarRatio)
				.build();
	}
	
	
}
