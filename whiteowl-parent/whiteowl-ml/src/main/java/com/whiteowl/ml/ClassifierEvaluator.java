package com.whiteowl.ml;

import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.filters.supervised.instance.ClassBalancer;

public class ClassifierEvaluator {
	
	public Evaluation evaluateMultifold(Classifier classifier, Instances instances) {
		return evaluateMultifold(classifier, instances, 10, (index, eval) -> {});
	}
	
	@SneakyThrows
	public Evaluation evaluateMultifold(Classifier classifier, Instances instances, int folds, 
			BiConsumer<Integer, Evaluation> foldEvaluationListener) {
		final Instances balancedInstances = balance(instances);
		balancedInstances.randomize(new Random());
		balancedInstances.stratify(folds);
		final Evaluation evaluation = new Evaluation(balancedInstances);
		for(int index = 0; index < folds; index++) {
			final Instances train = balancedInstances.trainCV(folds, index);
			final Instances test = balancedInstances.testCV(folds, index);
			final Classifier classifierCopy = AbstractClassifier.makeCopy(classifier);
			classifierCopy.buildClassifier(train);
			evaluation.evaluateModel(classifierCopy, test);
			if(null != foldEvaluationListener) foldEvaluationListener.accept(index, evaluation);
		}
		print(evaluation);
		return evaluation;
	}
	
	private void print(Evaluation evaluation) {
		System.out.println("PctCorrect : " + evaluation.pctCorrect());
		System.out.println("Precision : " + evaluation.precision(0) + ", F : " + evaluation.fMeasure(0));
		final double[][] matrix = evaluation.confusionMatrix();
		for(double[] row : matrix) {
			for(double value : row) {
				System.out.printf("%.2f\t", value);
			}
			System.out.println();
		}
	}

	private Instances balance(Instances instances) throws Exception {
		final ClassBalancer classBalancer = new ClassBalancer();
		classBalancer.setInputFormat(instances);
		classBalancer.input(instances);
		classBalancer.batchFinished();
		final Instances balancedInstances = new Instances(instances);
		balancedInstances.clear();
		Stream.generate(classBalancer::output).takeWhile(Objects::nonNull).forEach(balancedInstances::add);
		return balancedInstances;
	}

}
