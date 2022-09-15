package com.whiteowl.ml.attribute.selection;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;

public class InfoGainAttributeIndexSelector extends AbstractAttributeIndexSelector {

	@Override
	protected ASSearch createSearch() {
		return new Ranker();
	}

	@Override
	protected ASEvaluation createEvaluation() {
		return new InfoGainAttributeEval();
	}

}