package com.whiteowl.workbench.surface;

import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * 2D line chart shown when exactly two axis columns are selected. Points are
 * pre-aggregated and sorted along the horizontal axis by the caller. Column
 * types are abstracted by {@link AxisScale}: numeric columns get auto-ranging
 * axes while categorical columns are laid out at integer positions labeled
 * with their distinct values.
 *
 * <p>Clicking a symbol reports the representative CSV row via
 * {@link #setOnPointSelected(IntConsumer)}.</p>
 */
public final class Line2DPane extends StackPane {

    private static final int MAX_SYMBOLS = 500;

    /** One plotted point; {@code rowIndex} is the representative CSV row. */
    public record Point(int rowIndex, double x, double y) {
    }

    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

    private IntConsumer onPointSelected;

    public Line2DPane() {
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        setMinSize(0, 0);
        getChildren().add(chart);
    }

    public void setOnPointSelected(IntConsumer listener) {
        this.onPointSelected = listener;
    }

    public void setData(List<Point> points, AxisScale xScale, AxisScale yScale,
                        String xName, String yName) {
        chart.getData().clear();
        if (points.isEmpty()) {
            return;
        }
        configureAxis(xAxis, xScale, xName);
        configureAxis(yAxis, yScale, yName);
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (Point p : points) {
            series.getData().add(datum(p, xScale, yScale, xName, yName));
        }
        chart.setCreateSymbols(points.size() <= MAX_SYMBOLS);
        chart.getData().setAll(series);
    }

    private XYChart.Data<Number, Number> datum(Point p, AxisScale xScale,
                                             AxisScale yScale,
                                             String xName, String yName) {
        XYChart.Data<Number, Number> datum = new XYChart.Data<>(p.x(), p.y());
        datum.nodeProperty().addListener((obs, o, node) -> {
            if (node == null) {
                return;
            }
            node.setCursor(Cursor.HAND);
            Tooltip.install(node, new Tooltip(
                    xName + ": " + xScale.format(p.x()) + "\n"
                            + yName + ": " + yScale.format(p.y())));
            node.setOnMouseClicked(e -> {
                if (onPointSelected != null) {
                    onPointSelected.accept(p.rowIndex());
                }
            });
        });
        return datum;
    }

    private void configureAxis(NumberAxis axis, AxisScale scale, String name) {
        axis.setLabel(name);
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return scale.format(value.doubleValue());
            }

            @Override
            public Number fromString(String text) {
                return CsvData.parseNumber(text);
            }
        });
        if (scale.isCategorical()) {
            axis.setAutoRanging(false);
            axis.setLowerBound(-0.5);
            axis.setUpperBound(scale.categories().size() - 0.5);
            axis.setTickUnit(1);
        } else {
            axis.setAutoRanging(true);
        }
    }

}
