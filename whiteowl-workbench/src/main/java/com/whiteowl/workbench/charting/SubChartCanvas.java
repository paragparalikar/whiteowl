package com.whiteowl.workbench.charting;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingAnchor;
import com.whiteowl.workbench.charting.drawing.DrawingManager;
import com.whiteowl.workbench.charting.drawing.DrawingRenderer;
import com.whiteowl.workbench.charting.indicator.SubChartResult;
import com.whiteowl.workbench.charting.indicator.SubChartResult.ExtraSeries;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.Setter;

public final class SubChartCanvas extends Canvas {

    private static final double PADDING_V = 4;
    private static final double SEPARATOR_HEIGHT = 1;
    private static final double LINE_WIDTH = 1.2;
    private static final double HISTOGRAM_OPACITY = 0.7;
    private static final double HISTOGRAM_BAR_GAP = 1;
    private static final double REFERENCE_LINE_DASH = 4;
    private static final double REFERENCE_LINE_GAP = 4;
    private static final double REFERENCE_LINE_OPACITY = 0.5;
    private static final double CROSSHAIR_DASH = 4;
    private static final double CROSSHAIR_GAP = 4;
    private static final double CROSSHAIR_WIDTH = 0.5;
    private static final double LABEL_BOX_PAD = 3;
    private static final double BILLION = 1_000_000_000;
    private static final double MILLION = 1_000_000;
    private static final double THOUSAND = 1_000;

    private final ChartViewport viewport;
    private final BarDataProvider dataProvider;
    @Getter @Setter private SubChartResult result;
    @Setter private double mainMouseX = -1;
    @Getter @Setter private int canvasId;
    @Setter private Timeframe activeTimeframe = Timeframe.DAILY;
    private DrawingManager drawingManager;
    private DrawingAnchor previewAnchor;
    private double mouseX = -1;
    private double mouseY = -1;
    private double dragStartX;
    private int dragStartIndex;
    private Runnable onHoverChanged;
    private Runnable onViewportChanged;
    private Runnable onDrawingComplete;

    public SubChartCanvas(ChartViewport viewport, BarDataProvider dataProvider) {
        this.viewport = viewport;
        this.dataProvider = dataProvider;
        setupInteraction();
    }

    public void setOnHoverChanged(Runnable callback) {
        this.onHoverChanged = callback;
    }

    public void setOnViewportChanged(Runnable callback) {
        this.onViewportChanged = callback;
    }

    public void setDrawingManager(DrawingManager drawingManager) {
        this.drawingManager = drawingManager;
    }

    public void setOnDrawingComplete(Runnable callback) {
        this.onDrawingComplete = callback;
    }

    public void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);
        drawSeparator(gc, w);
        if (result == null) return;
        double chartW = w - PRICE_AXIS_WIDTH;
        double chartH = h - PADDING_V * 2 - SEPARATOR_HEIGHT;
        double barWidth = viewport.computeBarWidth(chartW);
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        drawReferenceLines(gc, chartW, chartH, rangeMin, range);
        drawHistogram(gc, chartW, chartH, barWidth, viewStart, viewEnd, rangeMin, range);
        drawIndicatorLine(gc, chartW, chartH, barWidth, viewStart, viewEnd, rangeMin, range);
        drawExtraSeries(gc, chartW, chartH, barWidth, viewStart, viewEnd, rangeMin, range);
        drawAxisLabels(gc, w, chartH, rangeMin, rangeMax, range);
        drawCrosshair(gc, chartW, chartH, rangeMin, range);
        renderDrawings(gc, chartW, chartH, barWidth, viewStart, rangeMin, range);
    }

    public int getHoveredBarIndex() {
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        double effectiveX = mouseX >= 0 ? mouseX : mainMouseX;
        if (effectiveX < 0 || effectiveX > chartW) return -1;
        double barWidth = viewport.computeBarWidth(chartW);
        int offset = (int) (effectiveX / barWidth);
        int viewIndex = viewport.getStartIndex() + offset;
        return dataProvider.translateIndex(viewIndex);
    }

    private void setupInteraction() {
        setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            updatePreviewAnchor();
            render();
            notifyHoverChanged();
        });
        setOnMouseExited(e -> {
            mouseX = -1;
            mouseY = -1;
            previewAnchor = null;
            render();
            notifyHoverChanged();
        });
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY && drawingManager != null) {
                handleDrawingRightClick(e.getX(), e.getY());
                e.consume();
                return;
            }
            if (drawingManager != null && drawingManager.getActiveTool() != null) {
                handleDrawingClick();
                e.consume();
            }
        });
        setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (drawingManager != null && drawingManager.getActiveTool() != null) return;
            if (drawingManager != null && tryStartDrawingDrag(e.getX(), e.getY())) return;
            dragStartX = e.getX();
            dragStartIndex = viewport.getStartIndex();
        });
        setOnMouseDragged(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            if (drawingManager != null && drawingManager.getActiveTool() != null) return;
            if (drawingManager != null && drawingManager.isDragging()) {
                updateDrawingDrag();
                render();
                return;
            }
            double chartW = getWidth() - PRICE_AXIS_WIDTH;
            double barWidth = viewport.computeBarWidth(chartW);
            double dx = dragStartX - e.getX();
            int barDelta = (int) (dx / barWidth);
            viewport.panBy(barDelta - (viewport.getStartIndex() - dragStartIndex));
            render();
            notifyHoverChanged();
            notifyViewportChanged();
        });
        setOnMouseReleased(e -> {
            if (drawingManager != null && drawingManager.isDragging()) {
                drawingManager.endDrag();
                render();
            }
        });
        setOnScroll(e -> {
            int zoomDelta = e.getDeltaY() > 0 ? -10 : 10;
            viewport.zoomBy(zoomDelta);
            render();
            notifyViewportChanged();
        });
    }

    private void drawSeparator(GraphicsContext gc, double w) {
        gc.setFill(GRID);
        gc.fillRect(0, 0, w, SEPARATOR_HEIGHT);
    }

    private double computeRangeMin(int viewStart, int viewEnd) {
        if (!result.isAutoRange()) return result.getMinValue();
        double min = Double.MAX_VALUE;
        min = seriesMin(result.getValues(), viewStart, viewEnd, min);
        for (ExtraSeries es : result.getExtraSeries()) {
            min = seriesMin(es.getValues(), viewStart, viewEnd, min);
        }
        if (result.getHistogram() != null) {
            min = seriesMin(result.getHistogram(), viewStart, viewEnd, min);
            if (min > 0) min = 0;
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    private double computeRangeMax(int viewStart, int viewEnd) {
        if (!result.isAutoRange()) return result.getMaxValue();
        double max = -Double.MAX_VALUE;
        max = seriesMax(result.getValues(), viewStart, viewEnd, max);
        for (ExtraSeries es : result.getExtraSeries()) {
            max = seriesMax(es.getValues(), viewStart, viewEnd, max);
        }
        if (result.getHistogram() != null) {
            max = seriesMax(result.getHistogram(), viewStart, viewEnd, max);
            if (max < 0) max = 0;
        }
        return max == -Double.MAX_VALUE ? 0 : max;
    }

    private double seriesMin(double[] data, int viewStart, int viewEnd, double current) {
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= data.length) continue;
            if (!Double.isNaN(data[idx]) && data[idx] < current) current = data[idx];
        }
        return current;
    }

    private double seriesMax(double[] data, int viewStart, int viewEnd, double current) {
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= data.length) continue;
            if (!Double.isNaN(data[idx]) && data[idx] > current) current = data[idx];
        }
        return current;
    }

    private void drawReferenceLines(GraphicsContext gc, double chartW, double chartH,
                                    double rangeMin, double range) {
        if (result.getReferenceLines().length == 0) return;
        gc.setStroke(AXIS_TEXT);
        gc.setLineWidth(CROSSHAIR_WIDTH);
        gc.setGlobalAlpha(REFERENCE_LINE_OPACITY);
        gc.setLineDashes(REFERENCE_LINE_DASH, REFERENCE_LINE_GAP);
        for (double refVal : result.getReferenceLines()) {
            double y = toY(refVal, chartH, rangeMin, range);
            gc.strokeLine(0, y, chartW, y);
        }
        gc.setLineDashes();
        gc.setGlobalAlpha(1.0);
    }

    private void drawHistogram(GraphicsContext gc, double chartW, double chartH,
                               double barWidth, int viewStart, int viewEnd,
                               double rangeMin, double range) {
        if (result.getHistogram() == null) return;
        gc.setGlobalAlpha(HISTOGRAM_OPACITY);
        double zeroY = toY(0, chartH, rangeMin, range);
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= result.getHistogram().length) continue;
            double val = result.getHistogram()[idx];
            if (Double.isNaN(val)) continue;
            double x = (i - viewStart) * barWidth + HISTOGRAM_BAR_GAP;
            double w = barWidth - HISTOGRAM_BAR_GAP * 2;
            if (w < 1) w = 1;
            double y = toY(val, chartH, rangeMin, range);
            gc.setFill(val >= 0 ? result.getHistogramPositiveColor() : result.getHistogramNegativeColor());
            if (val >= 0) {
                gc.fillRect(x, y, w, zeroY - y);
            } else {
                gc.fillRect(x, zeroY, w, y - zeroY);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawIndicatorLine(GraphicsContext gc, double chartW, double chartH,
                                   double barWidth, int viewStart, int viewEnd,
                                   double rangeMin, double range) {
        gc.setLineWidth(LINE_WIDTH);
        drawSeries(gc, result.getValues(), result.getColor(), chartH, barWidth, viewStart, viewEnd, rangeMin, range);
    }

    private void drawExtraSeries(GraphicsContext gc, double chartW, double chartH,
                                 double barWidth, int viewStart, int viewEnd,
                                 double rangeMin, double range) {
        for (ExtraSeries es : result.getExtraSeries()) {
            gc.setLineWidth(es.getStrokeWidth() > 0 ? es.getStrokeWidth() : LINE_WIDTH);
            drawSeries(gc, es.getValues(), es.getColor(), chartH, barWidth, viewStart, viewEnd, rangeMin, range);
        }
    }

    private void drawSeries(GraphicsContext gc, double[] data, javafx.scene.paint.Color color,
                            double chartH, double barWidth, int viewStart, int viewEnd,
                            double rangeMin, double range) {
        gc.setStroke(color);
        double prevX = Double.NaN;
        double prevY = Double.NaN;
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= data.length) continue;
            double val = data[idx];
            if (Double.isNaN(val)) {
                prevX = Double.NaN;
                continue;
            }
            double x = (i - viewStart) * barWidth + barWidth / 2;
            double y = toY(val, chartH, rangeMin, range);
            if (!Double.isNaN(prevX)) {
                gc.strokeLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }
    }

    private void drawAxisLabels(GraphicsContext gc, double w, double chartH,
                                double rangeMin, double rangeMax, double range) {
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        gc.setFill(AXIS_TEXT);
        double axisX = w - PRICE_AXIS_WIDTH + 4;
        for (double refVal : result.getReferenceLines()) {
            double y = toY(refVal, chartH, rangeMin, range);
            gc.fillText(formatAxisValue(refVal), axisX, y + 3);
        }
        double topY = toY(rangeMax, chartH, rangeMin, range);
        gc.fillText(formatAxisValue(rangeMax), axisX, topY + 10);
        double botY = toY(rangeMin, chartH, rangeMin, range);
        gc.fillText(formatAxisValue(rangeMin), axisX, botY - 2);
    }

    private void drawCrosshair(GraphicsContext gc, double chartW, double chartH,
                               double rangeMin, double range) {
        double effectiveX = mouseX >= 0 ? mouseX : mainMouseX;
        if (effectiveX < 0 || effectiveX > chartW) return;
        gc.setStroke(CROSSHAIR);
        gc.setLineWidth(CROSSHAIR_WIDTH);
        gc.setLineDashes(CROSSHAIR_DASH, CROSSHAIR_GAP);
        gc.strokeLine(effectiveX, SEPARATOR_HEIGHT, effectiveX, SEPARATOR_HEIGHT + PADDING_V * 2 + chartH);
        if (mouseY >= SEPARATOR_HEIGHT && mouseY <= SEPARATOR_HEIGHT + PADDING_V * 2 + chartH) {
            gc.strokeLine(0, mouseY, chartW, mouseY);
            gc.setLineDashes();
            drawCrosshairValueLabel(gc, chartW, chartH, rangeMin, range);
        } else {
            gc.setLineDashes();
        }
    }

    private void drawCrosshairValueLabel(GraphicsContext gc, double chartW, double chartH,
                                         double rangeMin, double range) {
        double ratio = (SEPARATOR_HEIGHT + PADDING_V + chartH - mouseY) / chartH;
        double value = rangeMin + ratio * range;
        String text = formatAxisValue(value);
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        double labelW = text.length() * AXIS_FONT_SIZE * 0.65 + 6;
        double labelH = AXIS_FONT_SIZE + 6;
        double lx = chartW;
        double ly = mouseY - labelH / 2;
        gc.setFill(CROSSHAIR_LABEL_BG);
        gc.fillRect(lx, ly, labelW, labelH);
        gc.setFill(CROSSHAIR_LABEL_TEXT);
        gc.fillText(text, lx + LABEL_BOX_PAD, ly + labelH - LABEL_BOX_PAD);
    }

    private double toY(double value, double chartH, double rangeMin, double range) {
        double ratio = (value - rangeMin) / range;
        return SEPARATOR_HEIGHT + PADDING_V + chartH * (1 - ratio);
    }

    private String formatAxisValue(double value) {
        double abs = Math.abs(value);
        if (abs >= BILLION) return String.format("%.1fB", value / BILLION);
        if (abs >= MILLION) return String.format("%.1fM", value / MILLION);
        if (abs >= THOUSAND) return String.format("%.1fK", value / THOUSAND);
        if (value == (long) value) return String.valueOf((long) value);
        if (abs < 1) return String.format("%.4f", value);
        return String.format("%.2f", value);
    }

    private void notifyHoverChanged() {
        if (onHoverChanged != null) onHoverChanged.run();
    }

    private void notifyViewportChanged() {
        if (onViewportChanged != null) onViewportChanged.run();
    }

    private void notifyDrawingComplete() {
        if (onDrawingComplete != null) onDrawingComplete.run();
    }

    private void handleDrawingRightClick(double px, double py) {
        if (result == null) return;
        CoordinateMapper mapper = buildMapper();
        if (mapper == null) return;
        Drawing hit = drawingManager.findDrawingAt(px, py, canvasId, mapper);
        if (hit != null) {
            drawingManager.removeDrawing(hit);
            render();
        }
    }

    private void handleDrawingClick() {
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0 || result == null) return;
        double chartH = getHeight() - PADDING_V * 2 - SEPARATOR_HEIGHT;
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        double ratio = (SEPARATOR_HEIGHT + PADDING_V + chartH - mouseY) / chartH;
        double value = rangeMin + ratio * range;
        Drawing d = drawingManager.handleClick(timestamp, value, canvasId, null);
        if (d != null && d.isComplete()) {
            if (d.getTool().getType().isTextInput()) {
                promptDrawingText(d);
            }
            notifyDrawingComplete();
        }
        render();
    }

    private void promptDrawingText(Drawing drawing) {
        if (!(getParent() instanceof Pane parent)) return;
        TextField field = new TextField();
        field.getStyleClass().add("drawing-text-field");
        field.setLayoutX(mouseX);
        field.setLayoutY(mouseY - 10);
        field.setPrefWidth(120);
        field.setOnAction(e -> {
            drawing.setText(field.getText());
            parent.getChildren().remove(field);
            render();
        });
        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                parent.getChildren().remove(field);
            }
        });
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                drawing.setText(field.getText());
                parent.getChildren().remove(field);
                render();
            }
        });
        parent.getChildren().add(field);
        field.requestFocus();
    }

    private void updatePreviewAnchor() {
        if (drawingManager == null || drawingManager.getActiveTool() == null || result == null) {
            previewAnchor = null;
            return;
        }
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0) { previewAnchor = null; return; }
        double chartH = getHeight() - PADDING_V * 2 - SEPARATOR_HEIGHT;
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        double ratio = (SEPARATOR_HEIGHT + PADDING_V + chartH - mouseY) / chartH;
        double value = rangeMin + ratio * range;
        previewAnchor = drawingManager.buildPreviewAnchor(timestamp, value);
    }

    private boolean tryStartDrawingDrag(double px, double py) {
        if (result == null) return false;
        long timestamp = getExactTimestamp(px);
        if (timestamp < 0) return false;
        CoordinateMapper mapper = buildMapper();
        if (mapper == null) return false;
        double chartH = getHeight() - PADDING_V * 2 - SEPARATOR_HEIGHT;
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        double ratio = (SEPARATOR_HEIGHT + PADDING_V + chartH - py) / chartH;
        double value = rangeMin + ratio * range;
        return drawingManager.tryStartDrag(px, py, canvasId, timestamp, value, mapper);
    }

    private void updateDrawingDrag() {
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0 || result == null) return;
        double chartH = getHeight() - PADDING_V * 2 - SEPARATOR_HEIGHT;
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        double ratio = (SEPARATOR_HEIGHT + PADDING_V + chartH - mouseY) / chartH;
        double value = rangeMin + ratio * range;
        drawingManager.updateDrag(timestamp, value);
    }

    private long getHoveredTimestamp() {
        int barIndex = getHoveredBarIndex();
        if (barIndex < 0) return -1;
        Bars bars = dataProvider.peekBars();
        if (bars == null || bars.size() == 0) return -1;
        if (barIndex < bars.size()) return bars.getTimestamp(barIndex);
        long lastTs = bars.getTimestamp(bars.size() - 1);
        int overshoot = barIndex - (bars.size() - 1);
        return lastTs + overshoot * activeTimeframe.getSeconds() * 1000L;
    }

    private long getExactTimestamp(double px) {
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        double effectiveX = px >= 0 ? px : mainMouseX;
        if (effectiveX < 0 || effectiveX > chartW) return -1;
        Bars bars = dataProvider.peekBars();
        if (bars == null || bars.size() == 0) return -1;
        double barWidth = viewport.computeBarWidth(chartW);
        double fractionalOffset = effectiveX / barWidth;
        int viewIndex = viewport.getStartIndex() + (int) fractionalOffset;
        double frac = fractionalOffset - (int) fractionalOffset;
        int localIndex = dataProvider.translateIndex(viewIndex);
        int nextLocalIndex = dataProvider.translateIndex(viewIndex + 1);
        if (localIndex >= 0 && localIndex < bars.size()) {
            long ts = bars.getTimestamp(localIndex);
            if (nextLocalIndex >= 0 && nextLocalIndex < bars.size()) {
                long nextTs = bars.getTimestamp(nextLocalIndex);
                return ts + (long) (frac * (nextTs - ts));
            }
            return ts + (long) (frac * activeTimeframe.getSeconds() * 1000L);
        }
        if (bars.size() > 0) {
            long lastTs = bars.getTimestamp(bars.size() - 1);
            int globalLast = dataProvider.getCachedFrom() + bars.size() - 1;
            int overshoot = viewIndex - globalLast;
            return lastTs + (long) ((overshoot + frac) * activeTimeframe.getSeconds() * 1000L);
        }
        return -1;
    }

    private CoordinateMapper buildMapper() {
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        double chartH = getHeight() - PADDING_V * 2 - SEPARATOR_HEIGHT;
        if (chartW <= 0 || chartH <= 0) return null;
        double barWidth = viewport.computeBarWidth(chartW);
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        double rangeMin = computeRangeMin(viewStart, viewEnd);
        double rangeMax = computeRangeMax(viewStart, viewEnd);
        double range = rangeMax - rangeMin;
        if (range <= 0) range = 1;
        double finalRange = range;
        Bars bars = dataProvider.peekBars();
        int cachedFrom = dataProvider.getCachedFrom();
        long tfSeconds = activeTimeframe.getSeconds();
        return new CoordinateMapper() {
            @Override
            public double toX(long timestamp) {
                double barIndex = findFractionalBarIndex(bars, cachedFrom, timestamp, tfSeconds);
                return (barIndex - viewStart) * barWidth + barWidth / 2;
            }
            @Override
            public double toY(double val) {
                double r = (val - rangeMin) / finalRange;
                return SEPARATOR_HEIGHT + PADDING_V + chartH * (1 - r);
            }
            @Override
            public double chartWidth() { return chartW; }
            @Override
            public double chartHeight() { return chartH + SEPARATOR_HEIGHT + PADDING_V; }
        };
    }

    private static double findFractionalBarIndex(Bars bars, int cachedFrom, long timestamp, long tfSeconds) {
        if (bars == null || bars.size() == 0) return 0;
        long lastTs = bars.getTimestamp(bars.size() - 1);
        long tfMillis = tfSeconds * 1000L;
        if (timestamp > lastTs && tfMillis > 0) {
            double overshoot = (double) (timestamp - lastTs) / tfMillis;
            return cachedFrom + bars.size() - 1 + overshoot;
        }
        int lo = 0;
        int hi = bars.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long midTs = bars.getTimestamp(mid);
            if (midTs < timestamp) lo = mid + 1;
            else if (midTs > timestamp) hi = mid - 1;
            else return cachedFrom + mid;
        }
        if (lo > 0 && lo < bars.size()) {
            long prevTs = bars.getTimestamp(lo - 1);
            long nextTs = bars.getTimestamp(lo);
            if (nextTs != prevTs) {
                double frac = (double) (timestamp - prevTs) / (nextTs - prevTs);
                return cachedFrom + lo - 1 + frac;
            }
        }
        return cachedFrom + lo;
    }

    private void renderDrawings(GraphicsContext gc, double chartW, double chartH,
                                double barWidth, int viewStart,
                                double rangeMin, double range) {
        if (drawingManager == null) return;
        CoordinateMapper mapper = buildMapper();
        if (mapper == null) return;
        Drawing pending = drawingManager.getPendingDrawing();
        DrawingAnchor preview = (pending != null && pending.getCanvasId() == canvasId) ? previewAnchor : null;
        DrawingRenderer.renderDrawings(gc, drawingManager.getDrawingsForCanvas(canvasId),
                mapper, pending != null && pending.getCanvasId() == canvasId ? pending : null, preview);
    }

}
