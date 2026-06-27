package com.whiteowl.core.backtest.v2.feature;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureVector {

    private final TradeLifecyclePhase phase;
    private final String scripId;
    private final int positionId;
    private final int barIndex;
    private final long timestamp;
    private final Map<String, Float> features;

    public FeatureVector(TradeLifecyclePhase phase, String scripId, int positionId,
                         int barIndex, long timestamp) {
        this.phase = phase;
        this.scripId = scripId;
        this.positionId = positionId;
        this.barIndex = barIndex;
        this.timestamp = timestamp;
        this.features = new LinkedHashMap<>();
    }

    public void putFeature(String name, float value) {
        features.put(name, value);
    }

    public TradeLifecyclePhase getPhase() {
        return phase;
    }

    public String getScripId() {
        return scripId;
    }

    public int getPositionId() {
        return positionId;
    }

    public int getBarIndex() {
        return barIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Float> getFeatures() {
        return Collections.unmodifiableMap(features);
    }

    public List<String> getFeatureNames() {
        return List.copyOf(features.keySet());
    }

    public List<Float> getFeatureValues() {
        return List.copyOf(features.values());
    }

}
