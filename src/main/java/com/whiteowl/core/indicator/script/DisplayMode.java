package com.whiteowl.core.indicator.script;

public enum DisplayMode {

    SUBCHART,
    OVERLAY,
    VOLUME;

    private static final String SUBCHART_VALUE = "subchart";
    private static final String OVERLAY_VALUE = "overlay";
    private static final String VOLUME_VALUE = "volume";

    public static DisplayMode fromString(String value) {
        if (value == null) return SUBCHART;
        return switch (value.toLowerCase()) {
            case OVERLAY_VALUE -> OVERLAY;
            case VOLUME_VALUE -> VOLUME;
            default -> SUBCHART;
        };
    }

}
