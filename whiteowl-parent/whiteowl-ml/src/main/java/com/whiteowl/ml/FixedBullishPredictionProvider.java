package com.whiteowl.ml;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.internal.util.SerializationHelper;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.PersistentBar;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.prediction.Prediction;
import com.whiteowl.core.prediction.PredictionRange;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.util.Constant;
import com.whiteowl.ml.feature.BarSeriesNormaliser;
import com.whiteowl.ml.feature.BuyTradeRule;
import com.whiteowl.ml.feature.extracter.FeatureExtracter;
import com.whiteowl.ml.feature.extracter.ValueSeriesFeatureExtracter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedBullishPredictionProvider extends AbstractPredictionProvider {
	private static final int timeStopBarCount = 10;
	private static final double targetPercentage = 10;
	private static final double stopLossPercentage = 5;
	
	private final int minBarCount = 150;
	private final BarService barService;
	private final ScripService scripService;
	private final String modelIdPrefix = "fixed-bullish";
	private final BarSeriesNormaliser barSeriesNormalizer;
	private volatile SoftReference<Classifier> classifierRef;

	private String getModelId(Scrip scrip, Timeframe timeframe) {
		return String.join("-", modelIdPrefix, scrip.getCode(), timeframe.name());
	}
	
	private Path getPath(Scrip scrip, Timeframe timeframe) {
		return Constant.HOME.resolve("models").resolve(getModelId(scrip, timeframe));
	}
	
	@SneakyThrows
	private synchronized Classifier getClassifier(Scrip scrip, Timeframe timeframe) {
		Classifier classifier = null == classifierRef ? null : classifierRef.get();
		if(null == classifier) {
			final Path path = getPath(scrip, timeframe);
			if(Files.exists(path)) {
				classifier = SerializationHelper.deserialize(Files.newInputStream(path));
				classifierRef = new SoftReference<>(classifier);
			}
		}
		return classifier;
	}
	
	private FeatureExtracter createFeatureExtracter(BarSeries series) {
		final BarSeries normalSeries = barSeriesNormalizer.normalise(series);
		final TypicalPriceIndicator typicalPriceIndicator = new TypicalPriceIndicator(normalSeries);
		final SMAIndicator smaIndicator = new SMAIndicator(typicalPriceIndicator, 21);
		return new ValueSeriesFeatureExtracter("SMA(TP,21)", smaIndicator, 129);
	}
	
	private Instance createInstance(List<Double> featureValues, Instances instances) {
		final double[] features = featureValues.stream().mapToDouble(Double::doubleValue).toArray();
		final Instance instance = new DenseInstance(instances.numAttributes());
		instance.setDataset(instances);
		for (int f = 0; f < features.length; f++) {
			instance.setValue(f, features[f]);
		}
		return instance;
	}
	
	@SneakyThrows
	private Classifier build(Scrip scrip, Timeframe timeframe) {
		final List<Bar> bars = barService.findByCodeAndTimeframe(scrip.getCode(), timeframe).stream()
			.map(PersistentBar::toBar).collect(Collectors.toList());
		final BarSeries series = new BaseBarSeries(bars);
		final BuyTradeRule buyTradeule = new BuyTradeRule(series, timeStopBarCount, targetPercentage, stopLossPercentage);
		final FeatureExtracter featureExtracter = createFeatureExtracter(series);
		final Instances instances = createInstances(featureExtracter.getAttributeNames());
		for (int index = featureExtracter.getMinBarCount(); index < series.getBarCount(); index++) {
			final Instance instance = createInstance(featureExtracter.extract(index), instances);
			instance.setClassValue(String.valueOf(buyTradeule.isSatisfied(index)));
			instances.add(instance);
		}
		final Instances balancedInstances = balance(instances);
		final IBk classifier = new IBk(3);
		classifier.setMeanSquared(true);
		classifier.setDistanceWeighting(new SelectedTag(IBk.WEIGHT_INVERSE, IBk.TAGS_WEIGHTING));
		classifier.buildClassifier(balancedInstances);
		return classifier;
	}
	
	protected Instances createInstances(List<String> attributeNames) {
		final Attribute classAttribute = new Attribute("success",
				Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()));
		final ArrayList<Attribute> attributes = attributeNames.stream().map(Attribute::new)
				.collect(Collectors.toCollection(ArrayList::new));
		attributes.add(classAttribute);
		final Instances instances = new Instances("dataset", attributes, 100);
		instances.setClass(classAttribute);
		return instances;
	}
	
	private boolean isExpired(Scrip scrip, Timeframe timeframe, Duration ttl) throws IOException {
		final Path path = getPath(scrip, timeframe);
		final Instant lastModifiedTime = Files.exists(path) ? 
				Files.getLastModifiedTime(path).toInstant() : Instant.MIN;
		final Instant now = Instant.now();
		return 0 <= Duration.between(lastModifiedTime, now).abs().compareTo(ttl);
	}

	@Override
	@SneakyThrows
	public void rebuild(Duration ttl) {
		final Timeframe timeframe = Timeframe.D;
		for(Scrip scrip : scripService.findByIndices(Index.NIFTY50)) {
			if(isExpired(scrip, timeframe, ttl)) {
				log.warn("Model expired for {} {} - {}", modelIdPrefix, scrip.getCode(), timeframe);
				final Path path = getPath(scrip, timeframe);
				final Classifier classifier = build(scrip, timeframe);
				SerializationHelper.serialize((Serializable) classifier, Files.newOutputStream(path));
			}
		}
	}

	@Override
	@SneakyThrows
	public Set<Prediction> predict(Scrip scrip, Timeframe timeframe) {
		final Classifier classifier = getClassifier(scrip, timeframe);
		if(null == classifier) return Collections.emptySet();
		final List<Bar> bars = barService.findLatestByCodeAndTimeframe(scrip.getCode(), timeframe, minBarCount).stream()
				.map(PersistentBar::toBar).collect(Collectors.toList());
		if(minBarCount > bars.size()) return Collections.emptySet();
		final BarSeries series = new BaseBarSeries(bars);
		final FeatureExtracter featureExtracter = createFeatureExtracter(series);
		final Instances instances = createInstances(featureExtracter.getAttributeNames());
		final Instance instance = createInstance(featureExtracter.extract(series.getEndIndex()), instances);
		final double classValue = classifier.classifyInstance(instance);
		final Boolean result = Boolean.parseBoolean(instances.classAttribute().value((int)classValue));
		if(!result) return Collections.emptySet(); 
		return Collections.singleton(createPrediction(series.getLastBar(), scrip, timeframe));
	}
	
	private Prediction createPrediction(Bar lastBar, Scrip scrip, Timeframe timeframe) {
		final Num entryPrice = lastBar.getHighPrice().plus(lastBar.getClosePrice())
				.dividedBy(DoubleNum.valueOf(2));
		final Num stopPrice = entryPrice.minus(entryPrice.multipliedBy(
				DoubleNum.valueOf(stopLossPercentage)).dividedBy(DoubleNum.valueOf(100)));
		final Num targetPrice = entryPrice.plus(entryPrice.multipliedBy(
				DoubleNum.valueOf(targetPercentage)).dividedBy(DoubleNum.valueOf(100)));
		return Prediction.builder()
				.modelId(getModelId(scrip, timeframe))
				.scrip(scrip)
				.timeframe(timeframe)
				.timestamp(lastBar.getEndTime())
				.high(PredictionRange.builder()
						.min(targetPrice.doubleValue())
						.max(targetPrice.doubleValue())
						.build())
				.low(PredictionRange.builder()
						.min(stopPrice.doubleValue())
						.max(stopPrice.doubleValue())
						.build())
				.build();
	}

}
