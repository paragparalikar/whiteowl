package com.whiteowl.ml.attribute.selection;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.core.Instances;

public abstract class AbstractAttributeIndexSelector implements AttributeIndexSelector {
	
	protected abstract ASSearch createSearch();
	
	protected abstract ASEvaluation createEvaluation();

	@Override
	@SneakyThrows
	public synchronized Set<Integer> selectAttributeIndices(Instances instances, int countToSelect) {
		final ASSearch ranker = createSearch();
		ranker.setOptions(new String[] {"-N", String.valueOf(countToSelect)});
		final AttributeSelection attributeSelection = new AttributeSelection();
		attributeSelection.setEvaluator(createEvaluation());
		attributeSelection.setSearch(ranker);
		attributeSelection.SelectAttributes(instances);
		return IntStream.of(attributeSelection.selectedAttributes()).boxed().collect(Collectors.toSet());
	}

}
