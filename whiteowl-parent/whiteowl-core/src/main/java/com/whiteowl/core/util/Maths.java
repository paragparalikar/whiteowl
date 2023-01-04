package com.whiteowl.core.util;

public interface Maths {

	public static double calculateStandardDeviation(final double[] array) {
	    double sum = 0.0;
	    for (double i : array) sum += i;
	    int length = array.length;
	    double mean = sum / length;
	    double standardDeviation = 0.0;
	    for (double num : array) standardDeviation += Math.pow(num - mean, 2);
	    return Math.sqrt(standardDeviation / length);
	}
	
}
