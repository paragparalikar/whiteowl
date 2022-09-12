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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.ml.feature.BarSeriesNormaliser;
import com.whiteowl.ml.feature.BuyTradeRule;
import com.whiteowl.ml.feature.extracter.FeatureExtracter;
import com.whiteowl.ml.feature.extracter.ValueSeriesFeatureExtracter;

import lombok.SneakyThrows;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.supervised.instance.ClassBalancer;

public class Test {
	private static final int folds = 10;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final Path directory = Paths.get("C:/Users/parag/Documents/finance/trading/ml/");

	public static void main(String[] args) throws Exception {
		final String scripCode = "SIEMENS";
		final BarSeries barSeries = getBars(scripCode);
		final BarSeriesNormaliser barSeriesNormalizer = new BarSeriesNormaliser();
		final BarSeries normalSeries = barSeriesNormalizer.normalise(barSeries);
		final BuyTradeRule buyTradeule = new BuyTradeRule(barSeries, 10, 10, 5);
		
		final TypicalPriceIndicator typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		final SMAIndicator smaIndicator = new SMAIndicator(typicalPriceIndicator, 21);
		final FeatureExtracter featureExtracter = new ValueSeriesFeatureExtracter("SMA21.129vals", smaIndicator, 129);
		final Instances instances = createInstances(featureExtracter.getAttributeNames());
		
		for (int index = featureExtracter.getMinBarCount(); index < barSeries.getBarCount(); index++) {
			final double[] features = featureExtracter.extract(index).stream().mapToDouble(Double::doubleValue)
					.toArray();
			final Instance instance = new DenseInstance(instances.numAttributes());
			instance.setDataset(instances);
			for (int f = 0; f < features.length; f++) {
				instance.setValue(f, features[f]);
			}
			instance.setClassValue(String.valueOf(buyTradeule.isSatisfied(index)));
			instances.add(instance);
		}

		save(scripCode + "-" + featureExtracter.getName() + ".arff", instances);
		final Instances balancedInstances = balance(instances);
		final Random random = new Random();
		balancedInstances.randomize(random);
		balancedInstances.stratify(folds);
		final Map<RandomForestConfig, Double> results = new HashMap<>(); 
		
		for(int maxDepth = 0; maxDepth <= 100; maxDepth+=10) {
			for(int numFeatures = 0; numFeatures < balancedInstances.numAttributes() - 1; numFeatures+= 10) {
				for(int numIterations = 25; numIterations <= 300; numIterations+=25) {
					final RandomForestConfig config = RandomForestConfig.builder()
							.maxDepth(maxDepth)
							.numFeatures(numFeatures)
							.numIterations(numIterations)
							.build();
					final RandomForest classifier = config.create();
					classifier.setNumExecutionSlots(50);
					System.out.println("Evaluating for config " + config.toString());
					final Evaluation evaluation = new Evaluation(balancedInstances);
					for (int index = 0; index < folds; index++) {
						final Instances train = balancedInstances.trainCV(folds, index, random);
						final Instances test = balancedInstances.testCV(folds, index);
						final Classifier copy = AbstractClassifier.makeCopy(classifier);
						copy.buildClassifier(train);
						evaluation.evaluateModel(copy, test);
					}
					results.put(config, evaluation.pctCorrect());
					System.out.println(evaluation.pctCorrect() + " - " + config);
				}
			}
		}
		
		final RandomForestConfig bestConfig = results.entrySet().stream().max(Comparator.comparing(Entry::getValue))
				.map(Entry::getKey).orElse(null);
		System.out.println("Best Config : " + bestConfig);
	}

	private static void save(String fileName, Instances instances) throws IOException {
		final ArffSaver arffSaver = new ArffSaver();
		arffSaver.setInstances(instances);
		arffSaver.setFile(directory.resolve(fileName).toFile());
		arffSaver.writeBatch();
	}

	private static Instances createInstances(List<String> attributeNames) {
		final Attribute classAttribute = new Attribute("success",
				Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()));
		final ArrayList<Attribute> attributes = attributeNames.stream().map(Attribute::new)
				.collect(Collectors.toCollection(ArrayList::new));
		attributes.add(classAttribute);
		final Instances instances = new Instances("test", attributes, 100);
		instances.setClass(classAttribute);
		return instances;
	}

	private static Instances balance(Instances instances) throws Exception {
		final ClassBalancer classBalancer = new ClassBalancer();
		classBalancer.setInputFormat(instances);
		classBalancer.input(instances);
		classBalancer.batchFinished();
		final Instances balancedInstances = new Instances(instances);
		balancedInstances.clear();
		Stream.generate(classBalancer::output).takeWhile(Objects::nonNull).forEach(balancedInstances::add);
		return balancedInstances;
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
