package com.whiteowl.developer;

import java.util.List;

public class StrategyExecutor {


	public void execute(List<Series> serieses) {
		serieses.parallelStream().forEach(series -> {
			final int barCount = 1000 * series.timeframe.getDayMultiple();

		});
	}

}
