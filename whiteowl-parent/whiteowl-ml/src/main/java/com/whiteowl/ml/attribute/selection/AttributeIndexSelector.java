package com.whiteowl.ml.attribute.selection;

import java.util.Set;

import weka.core.Instances;

public interface AttributeIndexSelector {

	Set<Integer> selectAttributeIndices(Instances instances, int countToSelect);

}
