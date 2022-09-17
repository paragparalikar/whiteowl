package com.whiteowl.ml.feature.extracter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.ChopIndicator;
import org.ta4j.core.indicators.ROCIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IndicatorFeatureExtracter extends AbstractFeatureExtracter {

	private final int[] barCounts;
	
	@Override
	public List<String> getAttributeNames() {
		final List<String> names = new ArrayList<>();
		for(int barCount : barCounts) {
			names.add(String.format("atr(%d)", barCount));
			names.add(String.format("rsi(%d)", barCount));
			names.add(String.format("roc(%d)", barCount));
			names.add(String.format("sto(%d)", barCount));
			names.add(String.format("stdev(%d)", barCount));
			names.add(String.format("chop(%d)", barCount));
			names.add(String.format("sma(tp, %d)", barCount));
			names.add(String.format("lowerBBDist(%d)", barCount));
		}
		return names;
	}
	
	public List<Indicator<Num>> buildIndicators(BarSeries normalSeries) {
		final List<Indicator<Num>> indicators = new ArrayList<>();
		final Indicator<Num> typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		for(int barCount : barCounts) {
			final Indicator<Num> atrIndicator = new ATRIndicator(normalSeries, barCount);
			final Indicator<Num> rsiIndicator = new RSIIndicator(typicalPriceIndicator, barCount);
			final Indicator<Num> rocIndicator = new ROCIndicator(typicalPriceIndicator, barCount);
			final Indicator<Num> stoIndicator = new StochasticOscillatorKIndicator(normalSeries, barCount);
			final Indicator<Num> stdDevIndicator = new StandardDeviationIndicator(typicalPriceIndicator, barCount);
			final Indicator<Num> chopIndicator = new ChopIndicator(normalSeries, barCount, 100);
			final Indicator<Num> smaIndicator = new SMAIndicator(typicalPriceIndicator, barCount);
			final Indicator<Num> stdDev2Indicator = new TransformIndicator(stdDevIndicator, num -> num.multipliedBy(DoubleNum.valueOf(2)));
			final Indicator<Num> lowerBBIndicator = new DifferenceIndicator(smaIndicator, stdDev2Indicator);
			final Indicator<Num> lowerBBDistanceIndicator = new DifferenceIndicator(typicalPriceIndicator, lowerBBIndicator);
			
			indicators.add(atrIndicator);
			indicators.add(rsiIndicator);
			indicators.add(rocIndicator);
			indicators.add(stoIndicator);
			indicators.add(stdDevIndicator);
			indicators.add(chopIndicator);
			indicators.add(smaIndicator);
			indicators.add(lowerBBDistanceIndicator);
		}
		return indicators;
	}
	
	@Override
	public String getName() {
		return "indicators";
	}
	
	@Override
	public int getMinBarCount() {
		return IntStream.of(barCounts).max().orElse(1);
	}

}
