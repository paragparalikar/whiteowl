package com.whiteowl.developer.strategy;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import com.whiteowl.developer.Indicators;
import com.whiteowl.developer.bar.Bar;
import com.whiteowl.developer.bar.BarRepository;
import com.whiteowl.developer.bar.BarSeries;
import com.whiteowl.developer.bar.Timeframe;
import com.whiteowl.developer.trade.Trade;
import com.whiteowl.developer.trade.TradeSeries;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class StockMeanReversionStrategy {

	private List<Bar> bars;
	private CountDownLatch latch;
	private float[][] stochastics, sma;
	private Collection<TradeSeries<StockMeanReversionConfig>> tradeSerieses;
	private final int[] smaBarCounts = IntStream.rangeClosed(1, 10).map(value -> value * 5).toArray();
	private final int[] stochasticsBarCounts = IntStream.rangeClosed(1, 10).map(value -> value * 5).toArray();
	private final double[] maxStochastics = IntStream.rangeClosed(1, 10).map(value -> value * 5).asDoubleStream().toArray();
	private final CyclicBarrier barrier = new CyclicBarrier(smaBarCounts.length * stochasticsBarCounts.length);
	private final ExecutorService executorService = Executors.newWorkStealingPool(barrier.getParties());
	
	public static void main(String[] args) {
		final StockMeanReversionStrategy strategy = new StockMeanReversionStrategy();
		final List<String> codes = BarRepository.getInstance().findAllCodes();
		final LocalTime start = LocalTime.now();
		codes.stream().forEach(code -> {
			final Collection<TradeSeries<StockMeanReversionConfig>> serieses = strategy.apply(new BarSeries(code, Timeframe.D));
		});
		System.out.printf("Time taken for processing %d codes is %d seconds", codes.size(), 
				Duration.between(start, LocalTime.now()).getSeconds());
	}
	
	@SneakyThrows
	public Collection<TradeSeries<StockMeanReversionConfig>> apply(BarSeries series) {
		int globalId = 0;
		this.barrier.reset();
		this.tradeSerieses = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.bars = BarRepository.getInstance().load(series, 1000 * series.timeframe.getDayMultiple(), 0);
		this.latch = new CountDownLatch(smaBarCounts.length * stochasticsBarCounts.length * maxStochastics.length);
		sma = new float[smaBarCounts.length][bars.size()];
		stochastics = new float[stochasticsBarCounts.length][bars.size()];
		for(int smaBarCountIndex = 0; smaBarCountIndex < smaBarCounts.length; smaBarCountIndex++) {
			for(int stochasticsBarCountIndex = 0; stochasticsBarCountIndex < stochasticsBarCounts.length; stochasticsBarCountIndex++) {
				for(int maxStochasticsIndex = 0; maxStochasticsIndex < maxStochastics.length; maxStochasticsIndex++) {
					final StockMeanReversionConfig config = new StockMeanReversionConfig(
							globalId++, smaBarCountIndex, stochasticsBarCountIndex, maxStochasticsIndex);
					final TradeSeries<StockMeanReversionConfig> tradeSeries = new TradeSeries<StockMeanReversionConfig>(
							config, series.code, series.timeframe);
					tradeSerieses.add(tradeSeries);
					executorService.submit(() -> execute(config, tradeSeries));
				}
			}
		}
		latch.await();
		return tradeSerieses;
	}
	
	@SneakyThrows
	private void execute(StockMeanReversionConfig config, TradeSeries<StockMeanReversionConfig> tradeSeries) {
		
		try {
			if(config.globalId < smaBarCounts.length) {
				sma[config.globalId] = Indicators.sma(bars, smaBarCounts[config.globalId]);
			} else if(config.globalId < stochasticsBarCounts.length) {
				stochastics[config.globalId] = Indicators.stochastics(bars, 
						stochasticsBarCounts[config.globalId - smaBarCounts.length]);
			}
			
			barrier.await();
			
			float entryPrice = 0, target = 0, stopLoss = 0;
			long entryDate = 0;
			boolean tradeOpen = false;
			final int smaBarCount = smaBarCounts[config.smaBarCountIndex];
			final int stochasticsBarCount = stochasticsBarCounts[config.stochasticsBarCountIndex];
			for(int index = bars.size() - Math.max(smaBarCount, stochasticsBarCount); index >= 0; index--) {
				final Bar bar = bars.get(index);
				if(tradeOpen) {
					if(bar.low <= stopLoss) {
						tradeSeries.add(new Trade(entryDate, bar.date, entryPrice, stopLoss * 0.995f));
						tradeOpen = false;
					} else if(bar.high >= target) {
						tradeSeries.add(new Trade(entryDate, bar.date, entryPrice, target * 0.995f));
						tradeOpen = false;
					}
				} else {
					if(bar.close < sma[config.smaBarCountIndex][index]) continue;
					if(maxStochastics[config.maxStochasticsIndex] < stochastics[config.stochasticsBarCountIndex][index]) continue;
					if(bar.close < (bar.high + bar.low) / 2) continue;
					final Bar previousBar = bars.get(index + 1);
					if(bar.high < previousBar.high) continue;
					if(bar.low < previousBar.low) continue;
					entryDate = bar.date;
					entryPrice = bar.close * 1.005f; 	// Slippage 0.5 %
					target = entryPrice * 1.16f;  		// target 16%
					stopLoss = entryPrice * 0.92f;		// stop loss 8 %
					tradeOpen = true;
				}
			}
			//System.out.printf("Processed %s - %s, found %d trades\n", tradeSeries.code, tradeSeries.timeframe.name(), tradeSeries.size());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
		
		
	}
	
}

@RequiredArgsConstructor
class StockMeanReversionConfig {
	final int globalId, smaBarCountIndex, stochasticsBarCountIndex, maxStochasticsIndex;
}
