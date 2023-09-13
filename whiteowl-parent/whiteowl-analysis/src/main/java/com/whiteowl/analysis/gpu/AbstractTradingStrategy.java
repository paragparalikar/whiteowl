package com.whiteowl.analysis.gpu;

import java.util.List;

import com.aparapi.Kernel;

public abstract class AbstractTradingStrategy extends Kernel {

	protected final int[] barCount = new int[1];
	protected float[] open, high, low, close;
	protected long[] volume, date;
	
	@SuppressWarnings("deprecation")
	public AbstractTradingStrategy() {
		setExplicit(true);
		setExecutionModeWithoutFallback(EXECUTION_MODE.GPU);
	}
	
	public void put(List<Bar> reverseBars) {
		final int size = reverseBars.size();
		this.open = new float[size];
		this.high = new float[size];
		this.low = new float[size];
		this.close = new float[size];
		this.volume = new long[size];
		this.date = new long[size];
		this.barCount[0] = size;
		for(int index = 0; index < size; index++) {
			final Bar bar = reverseBars.get(size - index - 1);
			open[index] = bar.getOpen();
			high[index] = bar.getHigh();
			low[index] = bar.getLow();
			close[index] = bar.getClose();
			volume[index] = bar.getVolume();
			date[index] = bar.getDate();
		}
		put(open);
		put(high);
		put(low);
		put(close);
		put(volume);
		put(date);
	}
	
	public void put(float[] open, float[] high, float[] low, float[] close, long[] volume, long[] date) {
		if(null != open) 	{ this.open = open; 		put(open); 	}
		if(null != high) 	{ this.high = high; 		put(high); 	}
		if(null != low)  	{ this.low = low; 			put(low); 	}
		if(null != close) 	{ this.close = close; 		put(close); }
		if(null != volume) 	{ this.volume = volume; 	put(volume);}
		this.date = date; 		
		put(date);
		this.barCount[0] = date.length;
		put(barCount);
	}

	@Override
	public void run() {
		preCompute();
		globalBarrier();
		execute();
	}
	
	protected abstract void preCompute();
	
	protected abstract void execute();

}
