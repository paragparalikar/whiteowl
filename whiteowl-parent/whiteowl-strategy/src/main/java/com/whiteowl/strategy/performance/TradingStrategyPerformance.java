package com.whiteowl.strategy.performance;

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

	private int totalPositionCount;
	private int openPositionCount;
	private int closedPositionCount;
	private int winningPositionCount;
	private int losingPositionCount;
	private int breakEventPositionCount;
	private int netHoldingTimeInMinutes;
	private int averageHoldingTimeInMinutes;
	private int holdingTimeInMinutesPercentage;
	private double netLossAmount;
	private double netProfitAmount;
	private double netProfitLossAmount;
	private double buyAndHoldProfitLossAmount;
	private double netLossPercentage;
	private double netProfitPercentage;
	private double netProfitLossPercentage;
	private double buyAndHoldProfitLossPercentage;
	private double averageLossAmount;
	private double averageProfitAmount;
	private double averageProfitLossAmount;
	private double averageLossPercentage;
	private double averageProfitPercentage;
	private double averageProfitLossPercentage;
	private double maxLossAmount;
	private double maxProfitAmount;
	private double maxLossPercentage;
	private double maxProfitPercentage;
	
	private double winRatio; 
	private double expectancy;
	private double profitFactor; 
	private double cagr;
	private double alpha;
	private double sharpeRatio; 
	private double maxDrawdown;

}
