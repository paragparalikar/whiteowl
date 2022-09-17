package com.whiteowl.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

import com.whiteowl.core.indicator.PercentageTransformIndicator;
import com.whiteowl.ml.feature.BarSeriesNormaliser;
import com.whiteowl.ml.feature.PercentageBarSeriesNormaliser;
import com.whiteowl.ml.feature.extracter.ValueSeriesFeatureExtracter;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class Test {

	private static Indicator<Num> createIndicator(BarSeries series, int barCount){
		final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
		final Indicator<Num> previousCloseIndicator = new PreviousValueIndicator(closePriceIndicator, barCount);
		final Indicator<Num> closeChangeIndicator = new DifferenceIndicator(closePriceIndicator, previousCloseIndicator);
		return new PercentageTransformIndicator(closeChangeIndicator);
	}
	
	public static void main(String[] args) throws Exception {
		final ValueSeriesFeatureExtracter featureExtracter = new ValueSeriesFeatureExtracter(
				"levels", 129, 1, Test::createIndicator);
		final Instances instances = createInstances(featureExtracter.getAttributeNames());
		final ExecutorService executor = Executors.newWorkStealingPool();
		final int minBarCount = 133;
		final BarSeriesNormaliser barSeriesNormalizer = new PercentageBarSeriesNormaliser();
		final TestDataService testDataService = new TestDataService();
		testDataService.getAllFiles().forEach(path -> {
			try {
				final Rule entryRule = (a, b) -> true;
				final BarSeries barSeries = testDataService.getBarSeries(path);
				System.out.println("Creating data for " + path.getFileName().toString());
				for(int index = minBarCount; index < barSeries.getBarCount() - 1; index++) {
					if(entryRule.isSatisfied(index)) {
						final int index_ = index;
						executor.submit(() -> {
							try {
								final BarSeries subSeries = barSeries.getSubSeries(index_ - minBarCount, index_ + 1);
								final BarSeries normalSeries = barSeriesNormalizer.normalise(subSeries);
								final double[] features = featureExtracter.extract(normalSeries.getEndIndex(), normalSeries)
										.stream().mapToDouble(Double::doubleValue)
										.toArray();
								final Instance instance = new DenseInstance(features.length + 1);
								instance.setDataset(instances);
								for (int f = 0; f < features.length; f++) {
									instance.setValue(f, features[f]);
								}
								instance.setClassValue(getResult(index_ + 1, 1, barSeries));
								instances.add(instance);
							}catch(Exception e) {
								e.printStackTrace();
							}
						});
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
		
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.HOURS);
		
		//final AttributeSelectionService attributeSelectionService = new AttributeSelectionService();
		//final Instances reducedInstances = attributeSelectionService.selectAttributes(instances, 10);
		testDataService.splitAndSave(5, String.join("-", "small", featureExtracter.getName()), instances);
	}
	
	private static String getResult(int index, int offset, BarSeries series) {
		if(index > series.getEndIndex() || index - offset < series.getBeginIndex()) {
			return "L";
		} else {
			final double currentValue = series.getBar(index).getClosePrice().doubleValue();
			final double pastValue = series.getBar(index - offset).getClosePrice().doubleValue();
			return currentValue > pastValue ? "H" : "L";
		}
	}
	
	private static Instances createInstances(List<String> attributeNames) {
		final Attribute classAttribute = new Attribute("prediction", Arrays.asList("H", "L"));
		final ArrayList<Attribute> attributes = attributeNames.stream().map(Attribute::new)
				.collect(Collectors.toCollection(ArrayList::new));
		attributes.add(classAttribute);
		final Instances instances = new Instances("test", attributes, 100);
		instances.setClass(classAttribute);
		return instances;
	}
	
}
