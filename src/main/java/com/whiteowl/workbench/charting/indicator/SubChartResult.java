package com.whiteowl.workbench.charting.indicator;

import javafx.scene.paint.Color;
import lombok.Getter;

import java.util.List;

@Getter
public final class SubChartResult {

    private final double[] values;
    private final Color color;
    private final String label;
    private final double minValue;
    private final double maxValue;
    private final double[] referenceLines;
    private final boolean autoRange;
    private final List<ExtraSeries> extraSeries;
    private final double[] histogram;
    private final Color histogramPositiveColor;
    private final Color histogramNegativeColor;

    public SubChartResult(double[] values, Color color, String label,
                          double minValue, double maxValue, double... referenceLines) {
        this(values, color, label, minValue, maxValue, false, List.of(), null, null, null, referenceLines);
    }

    public SubChartResult(double[] values, Color color, String label,
                          double minValue, double maxValue,
                          List<ExtraSeries> extraSeries, double... referenceLines) {
        this(values, color, label, minValue, maxValue, false, extraSeries, null, null, null, referenceLines);
    }

    public SubChartResult(double[] values, Color color, String label,
                          boolean autoRange, List<ExtraSeries> extraSeries,
                          double[] histogram, Color histPositive, Color histNegative,
                          double... referenceLines) {
        this(values, color, label, 0, 0, autoRange, extraSeries, histogram, histPositive, histNegative, referenceLines);
    }

    private SubChartResult(double[] values, Color color, String label,
                           double minValue, double maxValue, boolean autoRange,
                           List<ExtraSeries> extraSeries, double[] histogram,
                           Color histPositive, Color histNegative, double[] referenceLines) {
        this.values = values;
        this.color = color;
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.autoRange = autoRange;
        this.extraSeries = extraSeries;
        this.histogram = histogram;
        this.histogramPositiveColor = histPositive;
        this.histogramNegativeColor = histNegative;
        this.referenceLines = referenceLines;
    }

    @Getter
    public static final class ExtraSeries {
        private final double[] values;
        private final Color color;
        private final double strokeWidth;

        public ExtraSeries(double[] values, Color color) {
            this(values, color, 0);
        }

        public ExtraSeries(double[] values, Color color, double strokeWidth) {
            this.values = values;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }

}
