package com.whiteowl.ml.feature;

import org.ta4j.core.BarSeries;

public interface BarSeriesNormaliser {

	BarSeries normalise(BarSeries series);

}