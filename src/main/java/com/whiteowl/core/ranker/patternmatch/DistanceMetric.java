package com.whiteowl.core.ranker.patternmatch;

public enum DistanceMetric {

    SUBSEQUENCE_DTW("Subsequence DTW"),
    SBD("Shape-Based Distance");

    private final String displayName;

    DistanceMetric(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
