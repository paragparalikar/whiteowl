package com.whiteowl.ml.feature.extracter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.ta4j.core.BarSeries;

public class CompositeFeatureExtracter implements FeatureExtracter {

	private final List<FeatureExtracter> featureExtracters = new LinkedList<>();
	
	@Override
	public String getName() {
		return featureExtracters.stream()
				.map(FeatureExtracter::getName)
				.collect(Collectors.joining("."));
	}
	
	@Override
	public int getMinBarCount() {
		return featureExtracters.stream()
				.mapToInt(FeatureExtracter::getMinBarCount)
				.max().orElse(0);
	}
	
	public CompositeFeatureExtracter(FeatureExtracter ...featureExtracters) {
		this(Arrays.asList(featureExtracters));
	}
	
	public CompositeFeatureExtracter(Iterable<FeatureExtracter> featureExtracters) {
		for(FeatureExtracter featureExtracter : featureExtracters) {
			this.featureExtracters.add(featureExtracter);
		}
	}

	@Override
	public List<String> getAttributeNames() {
		final List<String> attributeNames = new ArrayList<>();
		for(FeatureExtracter featureExtracter : featureExtracters) {
			featureExtracter.getAttributeNames().stream()
				.map(name -> featureExtracter.getName() + "-" + name)
				.forEach(attributeNames::add);
				
		}
		return attributeNames;
	}

	@Override
	public List<Double> extract(int index, BarSeries barSeries) {
		return featureExtracters.stream()
				.map(featureExtracter -> featureExtracter.extract(index, barSeries))
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

}
