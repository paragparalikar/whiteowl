package com.whiteowl.ml.feature.extracter;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BarInfoFeatureExtracter extends AbstractFeatureExtracter {
	
	private final int barCount, barOffset;
	
	@Override
	public String getName() {
		return "bar-info-" + barCount + "-" + barOffset;
	}
	
	@Override
	public int getMinBarCount() {
		return barCount + barOffset;
	}
	
	@Override
	public List<String> getAttributeNames() {
		final List<String> names = new ArrayList<>();
		for(int index = 0; index < barOffset; index++) {
			names.add("body_" + index);
			names.add("absBody_" + index);
			names.add("gap_" + index);
			names.add("high_" + index);
			names.add("low_" + index);
			names.add("close_" + index);
			names.add("volume_" + index);
			names.add("closeloc_" + index);
			names.add("bodyLWickRatio_" + index);
			names.add("spreadLWickRatio_" + index);
			names.add("upperWick_" + index);
			names.add("lowerWick_" + index);
			names.add("trDiff_" + index);
			names.add("tpDiff_" + index);
			names.add("spredDiff_" + index);
			names.add("volumeDiff_" + index);
		}
		return names;
	}
	
	public List<Indicator<Num>> buildIndicators(BarSeries normalSeries) {
		final List<Indicator<Num>> indicators = new ArrayList<>();
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
		final Indicator<Num> prevATRIndicator = new PreviousValueIndicator(new ATRIndicator(normalSeries, barCount));
		final Indicator<Num> typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		final Indicator<Num> trDiffIndicator = new DifferenceIndicator(trIndicator, prevATRIndicator);
		final Indicator<Num> typicalPriceDiffIndicator = differenceIndicator(typicalPriceIndicator, barCount);
		final Indicator<Num> spreadDiffIndicator = differenceIndicator(spreadIndicator, barCount);
		final Indicator<Num> volumeDiffIndicator = differenceIndicator(volumeIndicator, barCount);
		
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
		
		for(int index = 1; index < barOffset; index++) {
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
		return indicators;
	}
}
