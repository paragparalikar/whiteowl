package com.whiteowl.workbench.charting.indicator.volumeprofile;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import com.whiteowl.workbench.charting.indicator.IndicatorSetting;
import com.whiteowl.workbench.charting.indicator.OverlayIndicator;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class VolumeProfileIndicator implements OverlayIndicator {

    private static final String INDICATOR_NAME = "Volume Profile";
    private static final String ATR_MULTIPLE_SETTING = "ATR Multiple";
    private static final String COUNT_MODE_SETTING = "Count Mode";
    private static final double DEFAULT_ATR_MULTIPLE = 0.5;

    private VolumeProfileData lastData;
    private int defaultViewStart;
    private int defaultViewEnd;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(ATR_MULTIPLE_SETTING, Double.class, DEFAULT_ATR_MULTIPLE),
                new IndicatorSetting(COUNT_MODE_SETTING, VolumeProfileCountMode.class, VolumeProfileCountMode.RANGE)
        );
    }

    public void setViewportRange(int viewStart, int viewEnd) {
        this.defaultViewStart = viewStart;
        this.defaultViewEnd = viewEnd;
    }

    @Override
    public IndicatorResult compute(Bars bars, Map<String, Object> settings) {
        float atrMultiple = resolveFloat(settings, ATR_MULTIPLE_SETTING, (float) DEFAULT_ATR_MULTIPLE);
        VolumeProfileCountMode mode = resolveMode(settings);
        int fromBar = lastData != null ? lastData.getFromBar() : defaultViewStart;
        int toBar = lastData != null ? lastData.getToBar() : defaultViewEnd;
        if (toBar <= fromBar) toBar = defaultViewEnd;
        lastData = VolumeProfileComputer.computeRange(bars, fromBar, toBar, atrMultiple, mode);
        String label = INDICATOR_NAME + "(" + atrMultiple + "x, " + mode + ")";
        return new IndicatorResult(new double[0], Color.TRANSPARENT, label);
    }

    public VolumeProfileData getLastData() {
        return lastData;
    }

    public void reset() {
        this.lastData = null;
    }

    private float resolveFloat(Map<String, Object> settings, String key, float defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Double d) return d.floatValue();
        if (val instanceof Float f) return f;
        return defaultValue;
    }

    private VolumeProfileCountMode resolveMode(Map<String, Object> settings) {
        Object val = settings.get(COUNT_MODE_SETTING);
        if (val instanceof VolumeProfileCountMode m) return m;
        return VolumeProfileCountMode.RANGE;
    }

}
