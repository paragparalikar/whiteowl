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
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IndicatorFeatureExtracter extends AbstractFeatureExtracter {

	private final int[] lengths;
	
	@Override
	public List<String> getAttributeNames() {
		final List<String> names = new ArrayList<>();
		final int availableBarCount = getMinBarCount();
		for(int barCount1 : lengths) {
			names.add(String.format("atr - length %d", barCount1));
			names.add(String.format("rsi - length %d", barCount1));
			names.add(String.format("roc - length %d", barCount1));
			names.add(String.format("sto - length %d", barCount1));
			names.add(String.format("stdev - length %d", barCount1));
			names.add(String.format("chop - length %d", barCount1));
			names.add(String.format("sma(tp) - length %d", barCount1));
			names.add(String.format("lowerBBDist - length %d", barCount1));
			for(int offset : lengths) {
				if(availableBarCount > barCount1 + offset) {
					names.add(String.format("atr - length %d - offset %d", barCount1, offset));
					names.add(String.format("rsi - length %d - offset %d", barCount1, offset));
					names.add(String.format("roc - length %d - offset %d", barCount1, offset));
					names.add(String.format("sto - length %d - offset %d", barCount1, offset));
					names.add(String.format("stdev - length %d - offset %d", barCount1, offset));
					names.add(String.format("chop - length %d - offset %d", barCount1, offset));
					names.add(String.format("sma(tp) - length %d - offset %d", barCount1, offset));
					names.add(String.format("lowerBBDist - length %d - offset %d", barCount1, offset));
				}
			}
		}
		return names;
	}
	
	public List<Indicator<Num>> buildIndicators(BarSeries normalSeries) {
		final List<Indicator<Num>> indicators = new ArrayList<>();
		final int availableBarCount = getMinBarCount();
		final Indicator<Num> typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		for(int barCount1 : lengths) {
			final Indicator<Num> atrIndicator = new ATRIndicator(normalSeries, barCount1);
			final Indicator<Num> rsiIndicator = new RSIIndicator(typicalPriceIndicator, barCount1);
			final Indicator<Num> rocIndicator = new ROCIndicator(typicalPriceIndicator, barCount1);
			final Indicator<Num> stoIndicator = new StochasticOscillatorKIndicator(normalSeries, barCount1);
			final Indicator<Num> stdDevIndicator = new StandardDeviationIndicator(typicalPriceIndicator, barCount1);
			final Indicator<Num> chopIndicator = new ChopIndicator(normalSeries, barCount1, 100);
			final Indicator<Num> smaIndicator = new SMAIndicator(typicalPriceIndicator, barCount1);
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
			
			for(int offset : lengths) {
				if(availableBarCount > barCount1 + offset) {
					indicators.add(new PreviousValueIndicator(atrIndicator, offset));
					indicators.add(new PreviousValueIndicator(rsiIndicator, offset));
					indicators.add(new PreviousValueIndicator(rocIndicator, offset));
					indicators.add(new PreviousValueIndicator(stoIndicator, offset));
					indicators.add(new PreviousValueIndicator(stdDevIndicator, offset));
					indicators.add(new PreviousValueIndicator(chopIndicator, offset));
					indicators.add(new PreviousValueIndicator(smaIndicator, offset));
					indicators.add(new PreviousValueIndicator(lowerBBDistanceIndicator, offset));
				}
			}
		}
		return indicators;
	}
	
	@Override
	public String getName() {
		return "indicators";
	}
	
	@Override
	public int getMinBarCount() {
		return IntStream.of(lengths).max().orElse(1) * 2;
	}

}
