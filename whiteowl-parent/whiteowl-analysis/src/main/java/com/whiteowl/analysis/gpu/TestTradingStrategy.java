package com.whiteowl.analysis.gpu;

public class TestTradingStrategy extends AbstractTradingStrategy {
	
	float[] open1, high1, low1, close1;
	long[] volume1, date1;

	@Override
	public void put(float[] open, float[] high, float[] low, float[] close, long[] volume, long[] date) {
		super.put(open, high, low, close, volume, date);
		final int size = barCount[0];
		open1 = new float[size];
		high1 = new float[size];
		low1 = new float[size];
		close1 = new float[size];
		volume1 = new long[size];
		date1 = new long[size];
	}
	
	@Override
	protected void preCompute() {
		final int globalId = getGlobalId();
		volume1[globalId] = volume[globalId] * 2;
		date1[globalId] = date[globalId] * 2;
	}

	@Override
	protected void execute() {
		final int globalId = getGlobalId();
		open1[globalId] = open[globalId] * 2;
		high1[globalId] = high[globalId] * 2;
		low1[globalId] = low[globalId] * 2;
		close1[globalId] = close[globalId] * 2;
	}
	
	public void print() {
		get(open1);
		get(high1);
		get(low1);
		get(close1);
		get(volume1);
		get(date1);
		for(int index = 0; index < barCount[0]; index++) {
			System.out.printf("%f\t%f\t%f\t%f\t%d\t%d\n", 
					open1[index], high1[index], low1[index], close1[index], volume1[index], date1[index]);
		}
		
		System.out.println(getProfileInfo());
	}

}
