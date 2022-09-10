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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import lombok.SneakyThrows;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.supervised.instance.ClassBalancer;

public class Test {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	public static void main(String[] args) throws IOException {
		final String scripCode = "RELIANCE";
		final BarSeries barSeries = getBars(scripCode);
		final BuyTradeRule buyTradeule = new BuyTradeRule(barSeries);
		final FeatureExtracter featureExtrater = new BaseFeatureExtracter(barSeries);
		
		final Instances instances = createInstances();
		final Path path = Paths.get("C:/Users/parag/Documents/finance/trading/ml/" + scripCode + ".arff");
		for(int index = BaseFeatureExtracter.MIN_BAR_COUNT; index < barSeries.getBarCount(); index++) {
			final double[] features = featureExtrater.extract(index).stream()
					.mapToDouble(Double::doubleValue).toArray();
			final Instance instance = new DenseInstance(instances.numAttributes());
			instance.setDataset(instances);
			for(int f = 0; f < features.length; f++) {
				instance.setValue(f, features[f]);
			}
			instance.setClassValue(String.valueOf(buyTradeule.isSatisfied(index)));
			instances.add(instance);
		}
		final ArffSaver arffSaver = new ArffSaver();
		arffSaver.setDestination(path.toFile());
		arffSaver.setInstances(instances);
		arffSaver.writeBatch();
	}
	
	private static Instances createInstances() {
		final Attribute classAttribute = new Attribute("success", 
				Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()));
		final ArrayList<Attribute> attributes = BaseFeatureExtracter.getAttributeNames().stream()
				.map(Attribute::new)
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
		Stream.generate(classBalancer::output)
			.takeWhile(Objects::nonNull)
			.forEach(balancedInstances::add);
		return balancedInstances;
	}
	
	@SneakyThrows
	private static BarSeries getBars(String scripCode) {
		final Path path = Paths.get("C:\\Users\\parag\\Documents\\finance\\trading\\ml\\data-small", scripCode + ".csv");
		final List<Bar> bars = Files.lines(path)
			.map(Test::parse)
			.distinct()
			.sorted(Comparator.comparing(Bar::getEndTime))
			.collect(Collectors.toList());
		return new BaseBarSeries(bars);
	}
	
	private static Bar parse(String line) {
		final String[] tokens = line.split(",");
		return BaseBar.builder()
				.timePeriod(Duration.ofDays(1))
				.endTime(LocalDate.parse(tokens[1], formatter).atStartOfDay(ZoneId.systemDefault()))
				.openPrice(DoubleNum.valueOf(tokens[2]))
				.highPrice(DoubleNum.valueOf(tokens[3]))
				.lowPrice(DoubleNum.valueOf(tokens[4]))
				.closePrice(DoubleNum.valueOf(tokens[5]))
				.volume(DoubleNum.valueOf(tokens[6]))
				.build();
	}
	
}
