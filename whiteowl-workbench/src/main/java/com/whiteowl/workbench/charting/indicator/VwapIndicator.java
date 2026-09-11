package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class VwapIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "VWAP";

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of();
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        int size = bars.size();
        double[] values = new double[size];
        if (size == 0) return new IndicatorResult(values, Color.WHITE, INDICATOR_NAME);
        double cumTypicalVolume = 0;
        double cumVolume = 0;
        for (int i = 0; i < size; i++) {
            double typical = (bars.getHigh(i) + bars.getLow(i) + bars.getClose(i)) / 3.0;
            long vol = bars.getVolume(i);
            cumTypicalVolume += typical * vol;
            cumVolume += vol;
            values[i] = cumVolume > 0 ? cumTypicalVolume / cumVolume : typical;
        }
        return new IndicatorResult(values, Color.WHITE, INDICATOR_NAME);
    }

}
