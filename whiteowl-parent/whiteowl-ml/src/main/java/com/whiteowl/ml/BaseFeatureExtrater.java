package com.whiteowl.ml;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.CloseLocationValueIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.indicators.helpers.TRIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import com.whiteowl.core.indicator.GapIndicator;
import com.whiteowl.core.indicator.LowerWickIndicator;
import com.whiteowl.core.indicator.RatioIndicator;
import com.whiteowl.core.indicator.UpperWickIndicator;

public class BaseFeatureExtrater implements FeatureExtracter {

	private final int baseLength = 8;
	private final List<Indicator<Num>> indicators = new LinkedList<>();
	
	public BaseFeatureExtrater(BarSeries series) {
		final BarSeriesNormaliser seriesNormaliser = new BarSeriesNormaliser();
		final BarSeries normalSeries = seriesNormaliser.normalise(series);
		
		final Indicator<Num> openIndicator = new OpenPriceIndicator(normalSeries);
		final Indicator<Num> gapIndicator = new GapIndicator(normalSeries);
		final Indicator<Num> volumeIndicator = new VolumeIndicator(normalSeries);
		final Indicator<Num> lowIndicator = new LowPriceIndicator(normalSeries);
		final Indicator<Num> highIndicator = new HighPriceIndicator(normalSeries);
		final Indicator<Num> closeIndicator = new ClosePriceIndicator(normalSeries);
		final Indicator<Num> upperWickIndicator = new UpperWickIndicator(normalSeries);
		final Indicator<Num> lowerWickIndicator = new LowerWickIndicator(normalSeries);
		final Indicator<Num> spreadIndicator = new DifferenceIndicator(highIndicator, lowIndicator);
		final Indicator<Num> bodyIndicator = new DifferenceIndicator(openIndicator, closeIndicator);
		final Indicator<Num> absBodyIndicator = new TransformIndicator(bodyIndicator, Num::abs);
		final Indicator<Num> trIndicator = new TRIndicator(normalSeries);
		final Indicator<Num> closeLocIndicator = new CloseLocationValueIndicator(normalSeries);
		final Indicator<Num> bodyLWickRatioIndicator = new RatioIndicator(absBodyIndicator, lowerWickIndicator);
		final Indicator<Num> spreadLWickRatioIndicator = new RatioIndicator(spreadIndicator, lowerWickIndicator);
		final Indicator<Num> prevATRIndicator = new PreviousValueIndicator(new ATRIndicator(normalSeries, baseLength));
		final Indicator<Num> typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		final Indicator<Num> trDiffIndicator = new DifferenceIndicator(trIndicator, prevATRIndicator);
		final Indicator<Num> typicalPriceDiffIndicator = differenceIndicator(typicalPriceIndicator, baseLength);
		final Indicator<Num> spreadDiffIndicator = differenceIndicator(spreadIndicator, baseLength);
		final Indicator<Num> volumeDiffIndicator = differenceIndicator(volumeIndicator, baseLength);
		
		indicators.add(bodyIndicator);
		indicators.add(absBodyIndicator);
		indicators.add(gapIndicator);
		indicators.add(highIndicator);
		indicators.add(lowIndicator);
		indicators.add(closeIndicator);
		indicators.add(volumeIndicator);
		indicators.add(closeLocIndicator);
		indicators.add(bodyLWickRatioIndicator);
		indicators.add(spreadLWickRatioIndicator);
		indicators.add(upperWickIndicator);
		indicators.add(lowerWickIndicator);
		indicators.add(trDiffIndicator);
		indicators.add(typicalPriceDiffIndicator);
		indicators.add(spreadDiffIndicator);
		indicators.add(volumeDiffIndicator);
		
		for(int index = 1; index < 3; index++) {
			indicators.add(new PreviousValueIndicator(bodyIndicator, index));
			indicators.add(new PreviousValueIndicator(absBodyIndicator, index));
			indicators.add(new PreviousValueIndicator(gapIndicator, index));
			indicators.add(new PreviousValueIndicator(highIndicator, index));
			indicators.add(new PreviousValueIndicator(lowIndicator, index));
			indicators.add(new PreviousValueIndicator(closeIndicator, index));
			indicators.add(new PreviousValueIndicator(volumeIndicator, index));
			indicators.add(new PreviousValueIndicator(closeLocIndicator, index));
			indicators.add(new PreviousValueIndicator(bodyLWickRatioIndicator, index));
			indicators.add(new PreviousValueIndicator(spreadLWickRatioIndicator, index));
			indicators.add(new PreviousValueIndicator(upperWickIndicator, index));
			indicators.add(new PreviousValueIndicator(lowerWickIndicator, index));
			indicators.add(new PreviousValueIndicator(trDiffIndicator, index));
			indicators.add(new PreviousValueIndicator(typicalPriceDiffIndicator, index));
			indicators.add(new PreviousValueIndicator(spreadDiffIndicator, index));
			indicators.add(new PreviousValueIndicator(volumeDiffIndicator, index));
		}
		
		
	}
	
	private Indicator<Num> differenceIndicator(Indicator<Num> indicator, int smaBarCount){
		return new DifferenceIndicator(indicator, new PreviousValueIndicator(new SMAIndicator(indicator, smaBarCount)));
	}
	
	@Override
	public List<Double> extract(int index){
		return indicators.stream()
				.map(indicator -> indicator.getValue(index))
				.map(Num::doubleValue)
				.collect(Collectors.toList());
	}

}
