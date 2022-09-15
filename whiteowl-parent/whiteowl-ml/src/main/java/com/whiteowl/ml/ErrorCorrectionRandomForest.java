package com.whiteowl.ml;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Stream;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.instance.ClassBalancer;

public class ErrorCorrectionRandomForest extends RandomForest {
	private static final long serialVersionUID = 1L;

	private final int folds;
	private RandomForest errorCorrectionClassifier;
	
	public ErrorCorrectionRandomForest(int folds) {
		this.folds = folds;
	}
	
	@Override
	public void buildClassifier(Instances data) throws Exception {
		final ArrayList<Attribute> attributes = new ArrayList<>();
		final Enumeration<Attribute> attributesEnum = data.enumerateAttributes();
		while(attributesEnum.hasMoreElements()) attributes.add(attributesEnum.nextElement());
		attributes.add(data.classAttribute());
		final Instances errorInstances = new Instances("error", attributes, data.numInstances());
		errorInstances.setClass(data.classAttribute());
		for(int fold = 0; fold < folds; fold++) {
			final Instances trainInstances = data.trainCV(folds, fold);
			final Instances testInstances = data.testCV(folds, fold);
			final RandomForest forest = new RandomForest();
			forest.setNumExecutionSlots(getNumExecutionSlots());
			forest.buildClassifier(trainInstances);
			for(int index = 0; index < testInstances.size(); index++) {
				final Instance instance = testInstances.instance(index);
				final Instance copyInstance = (Instance) instance.copy();
				copyInstance.setDataset(testInstances);
				copyInstance.setClassMissing();
				final double classValue = forest.classifyInstance(copyInstance);
				
				// Class can be either numeric or boolean, we do not support multiple classes
				if(instance.classAttribute().isNumeric()) {
					copyInstance.setClassValue(instance.classValue() - classValue);
				} else {
					copyInstance.setClassValue(String.valueOf(instance.classValue() == classValue));
				}
				copyInstance.setDataset(errorInstances);
				errorInstances.add(copyInstance);
			}
		}
		
		final ClassBalancer classBalancer = new ClassBalancer();
		classBalancer.setInputFormat(errorInstances);
		classBalancer.input(errorInstances);
		classBalancer.batchFinished();
		final Instances balancedInstances = new Instances(errorInstances);
		balancedInstances.clear();
		Stream.generate(classBalancer::output).takeWhile(Objects::nonNull).forEach(balancedInstances::add);
		
		errorCorrectionClassifier = new RandomForest();
		errorCorrectionClassifier.setNumExecutionSlots(getNumExecutionSlots());
		errorCorrectionClassifier.buildClassifier(balancedInstances);
		super.buildClassifier(data);
	}

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		final double[] distribution = super.distributionForInstance(instance);
		final double[] errorDistribution = errorCorrectionClassifier.distributionForInstance(instance);
		final Attribute classAttribute = instance.dataset().classAttribute();
		if(classAttribute.isNumeric()) {
			for(int index = 0; index < distribution.length; index++) {
				distribution[index] += errorDistribution[index];
			}
		} else {
			for(int index = 0; index < errorDistribution.length; index++) {
				if(0 < errorDistribution[index]) {
					distribution[index] = 0 == distribution[index] ? 1 : 0;
				}
			}
		}
		return distribution;
	}

}
