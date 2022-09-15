package com.whiteowl.ml.attribute.selection;

import java.util.Set;

import lombok.SneakyThrows;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class AttributeSelectionService {

	public Instances selectAttributes(Instances instances, int countToSelect) {
		final CompositeAttributeIndexSelector selector = new CompositeAttributeIndexSelector();
		final Set<Integer> selectedIndices = selector.selectAttributeIndices(instances, countToSelect);
		return remove(selectedIndices, instances);
	}
	
	@SneakyThrows
	private Instances remove(Set<Integer> selectedIndices, Instances instances) {
		final Remove remove = new Remove();
		final int[] indices = selectedIndices.stream().mapToInt(Integer::intValue).toArray();
		remove.setAttributeIndicesArray(indices);
		remove.setInvertSelection(true);
		remove.setInputFormat(instances);
		return Filter.useFilter(instances, remove);
	}

}
