package com.whiteowl.developer.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.whiteowl.developer.bar.Bar;
import com.whiteowl.developer.indicator.Indicator;
import com.whiteowl.developer.indicator.Indicators;
import com.whiteowl.developer.strategy.StochasticsMeanReversionStrategy.Config;
import com.whiteowl.developer.trade.Trade;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StochasticsMeanReversionStrategy implements Strategy<Config> {
	
	@EqualsAndHashCode
	@RequiredArgsConstructor
	public static class Config {
		public final float maxStochastics;
		public final int stochasticsBarCount, smaBarCount;
	}

	@Override
	public Set<Config> getConfigs() {
		final Set<Config> configs = new HashSet<>();
		for(float maxStochastics = 5; maxStochastics <= 50; maxStochastics += 5) {
			for(int stochsticsBarCount = 5; stochsticsBarCount <= 50; stochsticsBarCount += 5) {
				for(int smaBarCount = 5; smaBarCount <= 50; smaBarCount += 5) {
					configs.add(new Config(maxStochastics, stochsticsBarCount, smaBarCount));
				}
			}
		}
		return configs;
	}

	@Override
	public List<Trade> execute(Config config) {
		final List<Trade> trades = new ArrayList<>();
		long entryDate = 0;
		boolean tradeOpen = false;
		float entryPrice = 0, target = 0, stopLoss = 0;
		final Indicator sma = Indicators.sma(Indicators.close(), config.smaBarCount);
		final Indicator stochastics = Indicators.stochastics(config.stochasticsBarCount);
		final int start = Math.max(config.stochasticsBarCount, config.smaBarCount);
		for(int index = start; index < Indicators.bars.size(); index++) {
			final Bar bar = Indicators.bars.get(index);
			if(tradeOpen) {
				if(bar.low <= stopLoss) {
					trades.add(new Trade(entryDate, bar.date, entryPrice, stopLoss * 0.995f));
					tradeOpen = false;
				} else if(bar.high >= target) {
					trades.add(new Trade(entryDate, bar.date, entryPrice, target * 0.995f));
					tradeOpen = false;
				}
			} else {
				if(bar.close < sma.getValue(index)) continue;
				if(config.maxStochastics < stochastics.getValue(index)) continue;
				if(bar.close < (bar.high + bar.low) / 2) continue;
				final Bar previousBar = Indicators.bars.get(index - 1);
				if(bar.high < previousBar.high) continue;
				if(bar.low < previousBar.low) continue;
				entryDate = bar.date;
				entryPrice = bar.close * 1.005f; 	// Slippage 0.5 %
				target = entryPrice * 1.16f;  		// target 16%
				stopLoss = entryPrice * 0.92f;		// stop loss 8 %
				tradeOpen = true;
			}
		}
		return trades;
	}
	
	
}
