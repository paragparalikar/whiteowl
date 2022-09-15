package com.whiteowl.ml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.rule.BullishBarRule;
import com.whiteowl.core.rule.LongOpportunityRule;
import com.whiteowl.core.rule.NotOverboughtByPriceChannelRule;
import com.whiteowl.core.rule.NotOverboughtByRSIRule;
import com.whiteowl.ml.feature.BarSeriesNormaliser;
import com.whiteowl.ml.feature.LongSuccessClassificationRule;
import com.whiteowl.ml.feature.PercentageBarSeriesNormaliser;
import com.whiteowl.ml.feature.extracter.BarInfoFeatureExtracter;
import com.whiteowl.ml.feature.extracter.CompositeFeatureExtracter;
import com.whiteowl.ml.feature.extracter.FeatureExtracter;
import com.whiteowl.ml.feature.extracter.IndicatorFeatureExtracter;
import com.whiteowl.ml.feature.extracter.ValueSeriesFeatureExtracter;

import lombok.SneakyThrows;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.instance.ClassBalancer;
import weka.filters.unsupervised.attribute.Remove;

public class Test {
	private static final int folds = 10;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final Path directory = Paths.get("C:/Users/parag/Documents/finance/trading/ml/");

	public static void main(String[] args) throws Exception {
		final String scripCode = "SUNPHARMA";
		final BarSeries barSeries = getBars(scripCode);
		final BarSeriesNormaliser barSeriesNormalizer = new PercentageBarSeriesNormaliser();
		final Rule entryRule = new BullishBarRule(barSeries)
				.and(new LongOpportunityRule(barSeries))
				.and(new NotOverboughtByRSIRule(8, 70, barSeries))
				.and(new NotOverboughtByPriceChannelRule(21, barSeries));
		final Rule buyTradeule = new LongSuccessClassificationRule(barSeries, 5, 5, 2.5);
		
		final ValueSeriesFeatureExtracter valueSeriesFeatureExtracter = new ValueSeriesFeatureExtracter("SMA(TP,21)", 129, 21, 
				(series, barCount) -> new SMAIndicator(new TypicalPriceIndicator(series), barCount));
		final BarInfoFeatureExtracter barInfoFeatureExtracter = new BarInfoFeatureExtracter(8, 10);
		final IndicatorFeatureExtracter indicatorFeatureExtracter = new IndicatorFeatureExtracter(new int[] {2,3,5,8,13,21});
		final FeatureExtracter featureExtracter = new CompositeFeatureExtracter(indicatorFeatureExtracter);
		final Instances instances = createInstances(featureExtracter.getAttributeNames());
		final ExecutorService executor = Executors.newWorkStealingPool();
		final int minBarCount = featureExtracter.getMinBarCount();
		int index = minBarCount;
		for(index = minBarCount; index < barSeries.getBarCount() - 1; index++) {
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
						
						instance.setClassValue(String.valueOf(buyTradeule.isSatisfied(index_)));
						instances.add(instance);
						System.out.println("Created instance for index " + index_);
					}catch(Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
		
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.HOURS);
		
		
		splitAndSave(5, String.join("-", scripCode, featureExtracter.getName()), instances);
		//evaluate(instances);
	}
	
	private static void splitAndSave(int folds, String name, Instances instances) throws IOException {
		instances.stratify(folds);
		save(name + ".arff", instances);
		save(name + "-train.arff", instances.trainCV(folds, 1));
		save(name + "-test.arff", instances.testCV(folds, 1));
	}
	
	private static void save(String fileName, Instances instances) throws IOException {
		final ArffSaver arffSaver = new ArffSaver();
		arffSaver.setInstances(instances);
		arffSaver.setFile(directory.resolve(fileName).toFile());
		arffSaver.writeBatch();
	}

	private static Instances createInstances(List<String> attributeNames) {
		final Attribute classAttribute = new Attribute("prediction", Arrays.asList("true", "false"));
		final ArrayList<Attribute> attributes = attributeNames.stream().map(Attribute::new)
				.collect(Collectors.toCollection(ArrayList::new));
		attributes.add(classAttribute);
		final Instances instances = new Instances("test", attributes, 100);
		instances.setClass(classAttribute);
		return instances;
	}

	@SneakyThrows
	private static BarSeries getBars(String scripCode) {
		final Path path = Paths.get("C:\\Users\\parag\\Documents\\finance\\trading\\ml\\data-small",
				scripCode + ".csv");
		final List<Bar> bars = Files.lines(path).map(Test::parse).distinct()
				.sorted(Comparator.comparing(Bar::getEndTime)).collect(Collectors.toList());
		return new BaseBarSeries(bars);
	}

	private static Bar parse(String line) {
		final String[] tokens = line.split(",");
		return BaseBar.builder().timePeriod(Duration.ofDays(1))
				.endTime(LocalDate.parse(tokens[1], formatter).atStartOfDay(ZoneId.systemDefault()))
				.openPrice(DoubleNum.valueOf(tokens[2])).highPrice(DoubleNum.valueOf(tokens[3]))
				.lowPrice(DoubleNum.valueOf(tokens[4])).closePrice(DoubleNum.valueOf(tokens[5]))
				.volume(DoubleNum.valueOf(tokens[6])).build();
	}

}
