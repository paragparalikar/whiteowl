package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.indicator.IndicatorFunctions;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class AccumulationDistributionIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "A/D Line";
    private static final Color AD_COLOR = Color.web("#42a5f5");

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of();
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int size = bars.size();
        BarsArrays a = bars.arrays();
        float[] raw = IndicatorFunctions.accumulationDistribution(
                a.high(), a.low(), a.close(),
                a.volume(), size);
        double[] values = toDouble(raw, size);
        return new SubChartResult(values, AD_COLOR, INDICATOR_NAME, true, List.of(),
                null, null, null);
    }

    private double[] toDouble(float[] src, int size) {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = Float.isNaN(src[i]) ? Double.NaN : src[i];
        }
        return result;
    }

}
