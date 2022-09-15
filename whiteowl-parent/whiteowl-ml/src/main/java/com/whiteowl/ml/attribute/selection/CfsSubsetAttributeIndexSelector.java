package com.whiteowl.ml.attribute.selection;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;

public class CfsSubsetAttributeIndexSelector extends AbstractAttributeIndexSelector {

	@Override
	protected ASSearch createSearch() {
		return new BestFirst();
	}

	@Override
	protected ASEvaluation createEvaluation() {
		return new CfsSubsetEval();
	}

}