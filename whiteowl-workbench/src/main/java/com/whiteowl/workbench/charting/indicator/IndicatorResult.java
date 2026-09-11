package com.whiteowl.workbench.charting.indicator;

import javafx.scene.paint.Color;
import lombok.Getter;

import java.util.List;

@Getter
public final class IndicatorResult {

    private final double[] values;
    private final Color color;
    private final String label;
    private final boolean volumeOverlay;
    private final Color[] barColors;
    private final List<ExtraSeries> extraSeries;

    public IndicatorResult(double[] values, Color color, String label) {
        this(values, color, label, false, null, List.of());
    }

    public IndicatorResult(double[] values, Color color, String label, boolean volumeOverlay) {
        this(values, color, label, volumeOverlay, null, List.of());
    }

    public IndicatorResult(double[] values, Color color, String label, boolean volumeOverlay, Color[] barColors) {
        this(values, color, label, volumeOverlay, barColors, List.of());
    }

    public IndicatorResult(double[] values, Color color, String label,
                           boolean volumeOverlay, Color[] barColors, List<ExtraSeries> extraSeries) {
        this.values = values;
        this.color = color;
        this.label = label;
        this.volumeOverlay = volumeOverlay;
        this.barColors = barColors;
        this.extraSeries = extraSeries;
    }

    @Getter
    public static final class ExtraSeries {
        private final double[] values;
        private final Color color;

        public ExtraSeries(double[] values, Color color) {
            this.values = values;
            this.color = color;
        }
    }

}
