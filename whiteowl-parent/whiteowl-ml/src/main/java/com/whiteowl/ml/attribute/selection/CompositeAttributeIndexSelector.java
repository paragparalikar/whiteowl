package com.whiteowl.ml.attribute.selection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import weka.core.Instances;

public class CompositeAttributeIndexSelector implements AttributeIndexSelector {

	private final Set<AttributeIndexSelector> selectors = new HashSet<>();
	
	public CompositeAttributeIndexSelector() {
		this(new CfsSubsetAttributeIndexSelector(),
				new CorrelationAttributeIndexSelector(),
				new GainRatioAttributeIndexSelector(),
				new InfoGainAttributeIndexSelector());
	}
	
	public CompositeAttributeIndexSelector(AttributeIndexSelector...selectors) {
		this(Arrays.asList(selectors));
	}
	
	public CompositeAttributeIndexSelector(Iterable<AttributeIndexSelector> selectors) {
		selectors.forEach(this.selectors::add);
	}
	
	@Override
	public Set<Integer> selectAttributeIndices(Instances instances, int countToSelect) {
		return selectors.stream()
				.map(selector -> selector.selectAttributeIndices(instances, countToSelect))
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

}
