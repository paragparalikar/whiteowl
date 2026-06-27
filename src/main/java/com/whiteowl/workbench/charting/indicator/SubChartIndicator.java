package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;

import java.util.List;
import java.util.Map;

public interface SubChartIndicator {

    String getName();

    List<IndicatorSetting> getSettings();

    SubChartResult compute(Bars bars, Map<String, Object> settings);

}
