package com.whiteowl.analysis.gpu;

import java.util.Arrays;

import com.aparapi.Range;

public class Main {

	public static void main(String[] args) {
		final int size = 10;
		final float[] values = new float[size];
		Arrays.fill(values, 1.0f);
		final long[] longValues = new long[size];
		Arrays.fill(longValues, 1l);
		
		final TestTradingStrategy strategy = new TestTradingStrategy();
		strategy.put(values, values, values, values, longValues, longValues);
		strategy.execute(Range.create(size));
		strategy.print();
	}

}
