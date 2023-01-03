package com.whiteowl.strategy.performance;

import java.text.NumberFormat;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategyPerformance {
	
	@Id
	private String tradingStrategyConfigId;

	private double initialCapital;
	private double endCapital;
	private int totalPositionCount;
	private int openPositionCount;
	private int closedPositionCount;
	private int winningPositionCount;
	private int losingPositionCount;
	private int breakEventPositionCount;
	private int averageHoldingBarCount;
	private double holdingBarCountPercentage;
	private double netLossPercentage;
	private double netProfitPercentage;
	private double netProfitLossPercentage;
	private double buyAndHoldProfitLossPercentage;
	private double averageLossPercentage;
	private double averageProfitPercentage;
	private double averageProfitLossPercentage;
	private double maxLossPercentage;
	private double maxProfitPercentage;
	private double maxDrawdownPercentage;
	private double riskFreeReturnPercentage;
	
	private double winRatio; 
	private double lossRatio;
	private double expectancy;
	private double profitFactor; 
	private double cagr;
	private double romad;
	private double sharpeRatio;
	private double sortinoRatio;
	private double calmarRatio;
	
	@Override
	public String toString() {
		final NumberFormat numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setMinimumFractionDigits(2);
		final String lineSeparator = System.lineSeparator();
		final StringBuilder builder = new StringBuilder();
		builder.append("tradingStrategyConfigId           ").append(tradingStrategyConfigId).append(lineSeparator);
		builder.append("initialCapital                    ").append(numberFormat.format(initialCapital)).append(lineSeparator);
		builder.append("endCapital                        ").append(numberFormat.format(endCapital)).append(lineSeparator);
		builder.append("totalPositionCount                ").append(String.valueOf(totalPositionCount)).append(lineSeparator);
		builder.append("openPositionCount                 ").append(String.valueOf(openPositionCount)).append(lineSeparator);
		builder.append("closedPositionCount               ").append(String.valueOf(closedPositionCount)).append(lineSeparator);
		builder.append("winningPositionCount              ").append(String.valueOf(winningPositionCount)).append(lineSeparator);
		builder.append("losingPositionCount               ").append(String.valueOf(losingPositionCount)).append(lineSeparator);
		builder.append("breakEventPositionCount           ").append(String.valueOf(breakEventPositionCount)).append(lineSeparator);
		builder.append("averageHoldingBarCount            ").append(String.valueOf(averageHoldingBarCount)).append(lineSeparator);
		builder.append("holdingBarCountPercentage         ").append(numberFormat.format(holdingBarCountPercentage)).append(lineSeparator);
		builder.append("netLossPercentage                 ").append(numberFormat.format(netLossPercentage)).append(lineSeparator);
		builder.append("netProfitPercentage               ").append(numberFormat.format(netProfitPercentage)).append(lineSeparator);
		builder.append("netProfitLossPercentage           ").append(numberFormat.format(netProfitLossPercentage)).append(lineSeparator);
		builder.append("buyAndHoldProfitLossPercentage    ").append(numberFormat.format(buyAndHoldProfitLossPercentage)).append(lineSeparator);
		builder.append("averageLossPercentage             ").append(numberFormat.format(averageLossPercentage)).append(lineSeparator);
		builder.append("averageProfitPercentage           ").append(numberFormat.format(averageProfitPercentage)).append(lineSeparator);
		builder.append("averageProfitLossPercentage       ").append(numberFormat.format(averageProfitLossPercentage)).append(lineSeparator);
		builder.append("maxLossPercentage                 ").append(numberFormat.format(maxLossPercentage)).append(lineSeparator);
		builder.append("maxProfitPercentage               ").append(numberFormat.format(maxProfitPercentage)).append(lineSeparator);
		builder.append("maxDrawdownPercentage             ").append(numberFormat.format(maxDrawdownPercentage)).append(lineSeparator);
		builder.append("riskFreeReturnPercentage          ").append(numberFormat.format(riskFreeReturnPercentage)).append(lineSeparator);
		builder.append("winRatio                          ").append(numberFormat.format(winRatio)).append(lineSeparator);
		builder.append("lossRatio                         ").append(numberFormat.format(lossRatio)).append(lineSeparator);
		builder.append("expectancy                        ").append(numberFormat.format(expectancy)).append(lineSeparator);
		builder.append("profitFactor                      ").append(numberFormat.format(profitFactor)).append(lineSeparator);
		builder.append("cagr                              ").append(numberFormat.format(cagr)).append(lineSeparator);
		builder.append("romad                             ").append(numberFormat.format(romad)).append(lineSeparator);
		builder.append("sharpeRatio                       ").append(numberFormat.format(sharpeRatio)).append(lineSeparator);
		builder.append("sortinoRatio                      ").append(numberFormat.format(sortinoRatio)).append(lineSeparator);
		builder.append("calmarRatio                       ").append(numberFormat.format(calmarRatio)).append(lineSeparator);
		return builder.toString();
	}

}
