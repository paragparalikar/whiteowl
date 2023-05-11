package com.whiteowl.core.analysis.performance;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.Positions;

import lombok.Data;

@Data
@Entity
public class TradingStrategyConfigPerformance {

	@Id
	private String id;
	private int totalTradeCount, winningTradeCount, losingTradeCount, openTradeCount;
	private double cagr, maxDrawdownPct, avgDrawdownPct, avgWinPct, avgLossPct, avgReturnPctPerTrade;
	
	public TradingStrategyConfigPerformance(List<Position> positions) {
		double winSum = 0, lossSum = 0;
		totalTradeCount = positions.size();
		for(int index = 0; index < totalTradeCount; index++) {
			final Position position = positions.get(index);
			if(Positions.isOpen(position)) {
				openTradeCount++;
			} else {
				final double returnPct = Positions.getReturn(position) * 100;
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
