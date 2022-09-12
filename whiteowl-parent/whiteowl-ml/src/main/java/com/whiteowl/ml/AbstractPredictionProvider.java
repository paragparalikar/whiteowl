package com.whiteowl.ml;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.prediction.Prediction;
import com.whiteowl.core.prediction.PredictionProvider;
import com.whiteowl.core.scrip.Scrip;

import lombok.SneakyThrows;
import weka.core.Instances;
import weka.filters.supervised.instance.ClassBalancer;

public abstract class AbstractPredictionProvider implements PredictionProvider {

	@SneakyThrows
	protected Instances balance(Instances instances) {
		final ClassBalancer classBalancer = new ClassBalancer();
		classBalancer.setInputFormat(instances);
		classBalancer.input(instances);
		classBalancer.batchFinished();
		final Instances balancedInstances = new Instances(instances);
		balancedInstances.clear();
		Stream.generate(classBalancer::output).takeWhile(Objects::nonNull).forEach(balancedInstances::add);
		return balancedInstances;
	}
	
	@Override
	public void rebuild(Duration ttl) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Prediction> predict(Scrip scrip, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

}
