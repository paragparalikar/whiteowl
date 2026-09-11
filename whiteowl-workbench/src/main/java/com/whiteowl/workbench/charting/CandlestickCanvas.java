package com.whiteowl.workbench.charting;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.trendline.Trendline;
import com.whiteowl.workbench.charting.drawing.CoordinateMapper;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.charting.drawing.DrawingAnchor;
import com.whiteowl.workbench.charting.drawing.DrawingManager;
import com.whiteowl.workbench.charting.drawing.DrawingRenderer;
import com.whiteowl.workbench.charting.drawing.type.RulerDrawing;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileData;
import com.whiteowl.workbench.charting.renderer.CandlestickLayer;
import com.whiteowl.workbench.charting.renderer.CrosshairLayer;
import com.whiteowl.workbench.charting.renderer.GridLayer;
import com.whiteowl.workbench.charting.renderer.InfoBarLayer;
import com.whiteowl.workbench.charting.renderer.OrderLineLayer;
import com.whiteowl.workbench.charting.renderer.OverlayLayer;
import com.whiteowl.workbench.charting.renderer.PriceAxisLayer;
import com.whiteowl.workbench.charting.renderer.VolumeProfileLayer;
import com.whiteowl.workbench.charting.renderer.VolumeProfileLayer.EdgeHit;
import com.whiteowl.workbench.charting.renderer.ScreenMarkerLayer;
import com.whiteowl.workbench.charting.renderer.TrendlineLayer;
import com.whiteowl.workbench.charting.renderer.TimeAxisLayer;
import com.whiteowl.workbench.charting.renderer.TradeOverlayLayer;
import com.whiteowl.workbench.charting.renderer.VolumeLayer;
import com.whiteowl.workbench.charting.renderer.VolumeOverlayLayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public final class CandlestickCanvas extends Canvas {

    private static final double PRICE_RANGE_MARGIN = 0.05f;

    private final ChartViewport viewport;
    private final BarDataProvider dataProvider;
    private final List<ChartLayer> layers;
    private final VolumeProfileLayer volumeProfileLayer = new VolumeProfileLayer();
    private final TradeOverlayLayer tradeOverlayLayer = new TradeOverlayLayer();
    private final OrderLineLayer orderLineLayer = new OrderLineLayer();
    private Timeframe activeTimeframe = Timeframe.DAILY;
    private Scrip activeScrip;
    private List<IndicatorResult> overlayResults = Collections.emptyList();
    private boolean[] screenMarkers;
    private List<Trendline> trendlines = Collections.emptyList();
    private VolumeProfileData volumeProfileData;
    private DrawingManager drawingManager;
    private DrawingAnchor previewAnchor;
    private Runnable onHoverChanged;
    private Runnable onViewportChanged;
    private Runnable onDrawingComplete;
    private ContextMenuRequester onContextMenuRequested;
    private Runnable onContextMenuHide;
    @lombok.Getter private double mouseX = -1;
    private double mouseY = -1;
    private double dragStartX;
    private int dragStartIndex;
    private boolean draggingProfile;
    private double profileDragStartX;
    private EdgeHit draggingEdge = EdgeHit.NONE;
    private Runnable onProfileRangeChanged;
    private boolean logScale;
    private GttOrder draggingGttOrder;
    private float dragOriginalTriggerPrice;
    private Order draggingOrder;
    private float dragOriginalOrderPrice;
    private Consumer<GttOrder> onGttModified;
    private Consumer<GttOrder> onGttCancelled;
    private Consumer<OcoGttOrder> onOcoModified;
    private Consumer<OcoGttOrder> onOcoCancelled;
    private OcoGttOrder draggingOcoOrder;
    private boolean draggingOcoStoplossLeg;
    private float dragOriginalOcoPrice;
    private Consumer<Order> onOrderClicked;
    private Consumer<Order> onOrderCancelled;
    private Consumer<Order> onOrderModified;
    private PricePickerMode pricePickerMode;
    private BiConsumer<PricePickerMode, Float> onPricePicked;
    private RulerAddButtonHandler onRulerAddButtonClicked;

    public CandlestickCanvas(ChartViewport viewport, BarDataProvider dataProvider) {
        this.viewport = viewport;
        this.dataProvider = dataProvider;
        this.layers = List.of(
                new GridLayer(),
                new VolumeLayer(),
                new VolumeOverlayLayer(),
                new CandlestickLayer(),
                new OverlayLayer(),
                volumeProfileLayer,
                new TrendlineLayer(),
                new ScreenMarkerLayer(),
                new PriceAxisLayer(),
                new TimeAxisLayer(),
                new CrosshairLayer(),
                new InfoBarLayer()
        );
        setFocusTraversable(true);
        setupInteraction();
    }

    public void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);
        ChartContext ctx = buildContext(w, h);
        if (ctx == null) return;
        for (ChartLayer layer : layers) {
            layer.render(gc, ctx);
        }
        tradeOverlayLayer.render(gc, ctx);
        orderLineLayer.render(gc, ctx);
        renderLtpLine(gc, ctx);
        renderDrawings(gc, ctx);
        renderPricePickerLine(gc, ctx);
    }

    private static final Color LTP_LINE_COLOR = Color.web("#2196F3", 0.7);
    private static final double LTP_LINE_WIDTH = 1.0;
    private static final double[] LTP_DASH = {6, 4};

    private void renderLtpLine(GraphicsContext gc, ChartContext ctx) {
        if (livePrice <= 0) return;
        if (livePrice < ctx.minPrice() || livePrice > ctx.maxPrice()) return;
        double y = ctx.toY(livePrice);
        double chartWidth = ctx.chartWidth();
        gc.setStroke(LTP_LINE_COLOR);
        gc.setLineWidth(LTP_LINE_WIDTH);
        gc.setLineDashes(LTP_DASH);
        gc.strokeLine(0, y, chartWidth, y);
        gc.setLineDashes((double[]) null);

        String label = String.format("%.2f", livePrice);
        gc.setFill(LTP_LINE_COLOR);
        gc.setFont(Font.font(10));
        gc.fillText(label, chartWidth + 4, y + 4);
    }

    private ChartContext buildContext(double w, double h) {
        int start = viewport.getStartIndex();
        int end = viewport.getEndIndex();
        if (start >= end) return null;
        Bars bars;
        try {
            bars = dataProvider.fetchBars(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch bars", e);
            return null;
        }
        if (bars.size() == 0) return null;
        double chartWidth = w - PRICE_AXIS_WIDTH;
        double chartHeight = h - TIME_AXIS_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
        float[] priceRange = computePriceRange(bars, start, end);
        float minPrice = priceRange[0];
        float maxPrice = priceRange[1];
        if (maxPrice == minPrice) maxPrice = minPrice + 1;
        double barWidth = viewport.computeBarWidth(chartWidth);
        double bodyWidth = viewport.computeBodyWidth(barWidth);
        List<IndicatorResult> priceOverlays = new ArrayList<>();
        List<IndicatorResult> volumeOverlays = new ArrayList<>();
        for (IndicatorResult result : overlayResults) {
            if (result.isVolumeOverlay()) {
                volumeOverlays.add(result);
            } else {
                priceOverlays.add(result);
            }
        }
        return new ChartContext(
                bars, dataProvider, start, end,
                chartWidth, chartHeight, w, h,
                minPrice, maxPrice, barWidth, bodyWidth,
                mouseX, mouseY,
                activeScrip, activeTimeframe, priceOverlays, volumeOverlays,
                screenMarkers,
                trendlines,
                volumeProfileData,
                logScale
        );
    }

    private float[] computePriceRange(Bars bars, int viewStart, int viewEnd) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= bars.size()) continue;
            float lo = bars.getLow(idx);
            float hi = bars.getHigh(idx);
            if (lo < min) min = lo;
            if (hi > max) max = hi;
        }
        float margin = (max - min) * (float) PRICE_RANGE_MARGIN;
        return new float[]{min - margin, max + margin};
    }

    public int getHoveredBarIndex() {
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        if (mouseX < 0 || mouseX > chartW) return -1;
        double barWidth = viewport.computeBarWidth(chartW);
        int offset = (int) (mouseX / barWidth);
        int viewIndex = viewport.getStartIndex() + offset;
        return dataProvider.translateIndex(viewIndex);
    }

    public float getHoveredPrice() {
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return -1;
        return (float) yToPrice(mouseY, priceRange);
    }

    public void setTimeframe(Timeframe timeframe) {
        this.activeTimeframe = timeframe;
    }

    public void setScrip(Scrip scrip) {
        this.activeScrip = scrip;
        this.livePrice = 0;
    }

    private float livePrice;

    public void setLivePrice(float price) {
        this.livePrice = price;
    }

    public void setOverlayResults(List<IndicatorResult> results) {
        this.overlayResults = results != null ? results : Collections.emptyList();
    }

    public void setScreenMarkers(boolean[] markers) {
        this.screenMarkers = markers;
    }

    public void setTrendlines(List<Trendline> trendlines) {
        this.trendlines = trendlines != null ? trendlines : Collections.emptyList();
    }

    public void setVolumeProfileData(VolumeProfileData data) {
        this.volumeProfileData = data;
    }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    public VolumeProfileData getVolumeProfileData() {
        return volumeProfileData;
    }

    public void setOnProfileRangeChanged(Runnable callback) {
        this.onProfileRangeChanged = callback;
    }

    public void setGttOrders(List<GttOrder> orders) {
        orderLineLayer.setGttOrders(orders);
    }

    public void setRegularOrders(List<Order> orders) {
        orderLineLayer.setRegularOrders(orders);
    }

    public void setOnGttModified(Consumer<GttOrder> callback) {
        this.onGttModified = callback;
    }

    public void setOnGttCancelled(Consumer<GttOrder> callback) {
        this.onGttCancelled = callback;
    }

    public void setOcoOrders(List<OcoGttOrder> orders) {
        orderLineLayer.setOcoOrders(orders);
    }

    public void setOnOcoModified(Consumer<OcoGttOrder> callback) {
        this.onOcoModified = callback;
    }

    public void setOnOcoCancelled(Consumer<OcoGttOrder> callback) {
        this.onOcoCancelled = callback;
    }

    public void setOnOrderClicked(Consumer<Order> callback) {
        this.onOrderClicked = callback;
    }

    public void setOnOrderCancelled(Consumer<Order> callback) {
        this.onOrderCancelled = callback;
    }

    public void setOnOrderModified(Consumer<Order> callback) {
        this.onOrderModified = callback;
    }

    public void setOnPricePicked(BiConsumer<PricePickerMode, Float> callback) {
        this.onPricePicked = callback;
    }

    public void startPricePicker(PricePickerMode mode) {
        this.pricePickerMode = mode;
        render();
    }

    public void clearPricePicker() {
        this.pricePickerMode = null;
        render();
    }

    public void setTradeOverlay(List<TradeRecord> trades) {
        tradeOverlayLayer.setTrades(trades);
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

    public void setOnRulerAddButtonClicked(RulerAddButtonHandler handler) {
        this.onRulerAddButtonClicked = handler;
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
            if (e.getButton() == MouseButton.SECONDARY) {
                if (pricePickerMode != null) {
                    pricePickerMode = null;
                    render();
                    e.consume();
                    return;
                }
                handleRightClick(e.getX(), e.getY(), e.getScreenX(), e.getScreenY());
                e.consume();
                return;
            }
            if (pricePickerMode != null) {
                handlePricePickerClick();
                e.consume();
                return;
            }
            if (drawingManager != null && drawingManager.getActiveTool() != null) {
                handleDrawingClick();
                e.consume();
            }
        });
        setOnMousePressed(e -> {
            requestFocus();
            if (e.getButton() == MouseButton.PRIMARY && onContextMenuHide != null) {
                onContextMenuHide.run();
            }
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (pricePickerMode != null) return;
            if (drawingManager != null && drawingManager.getActiveTool() != null) return;
            if (tryHandleOrderLineClick(e.getX(), e.getY())) return;
            if (tryStartOcoDrag(e.getX(), e.getY())) return;
            if (tryStartGttDrag(e.getX(), e.getY())) return;
            if (tryStartEdgeDrag(e.getX())) return;
            if (tryStartProfileDrag(e.getX(), e.getY())) return;
            if (drawingManager != null && tryStartDrawingDrag(e.getX(), e.getY())) return;
            dragStartX = e.getX();
            dragStartIndex = viewport.getStartIndex();
        });
        setOnMouseDragged(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            if (draggingOcoOrder != null) {
                updateOcoDrag(e.getY());
                render();
                return;
            }
            if (draggingGttOrder != null) {
                updateGttDrag(e.getY());
                render();
                return;
            }
            if (draggingOrder != null) {
                updateOrderDrag(e.getY());
                render();
                return;
            }
            if (draggingEdge != EdgeHit.NONE) {
                updateEdgeDrag(e.getX());
                if (onProfileRangeChanged != null) onProfileRangeChanged.run();
                return;
            }
            if (draggingProfile) {
                updateProfileDrag(e.getX());
                render();
                return;
            }
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
            if (draggingOcoOrder != null) {
                endOcoDrag();
                return;
            }
            if (draggingGttOrder != null) {
                endGttDrag();
                return;
            }
            if (draggingOrder != null) {
                endOrderDrag();
                return;
            }
            if (draggingEdge != EdgeHit.NONE) {
                draggingEdge = EdgeHit.NONE;
                return;
            }
            if (draggingProfile) {
                draggingProfile = false;
                convertProfileOffsetToBarShift();
                if (onProfileRangeChanged != null) onProfileRangeChanged.run();
                return;
            }
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

    private void notifyHoverChanged() {
        if (onHoverChanged != null) onHoverChanged.run();
    }

    private void notifyViewportChanged() {
        if (onViewportChanged != null) onViewportChanged.run();
    }

    private void notifyDrawingComplete() {
        if (onDrawingComplete != null) onDrawingComplete.run();
    }

    public void setOnContextMenuRequested(ContextMenuRequester handler) {
        this.onContextMenuRequested = handler;
    }

    public void setOnContextMenuHide(Runnable handler) {
        this.onContextMenuHide = handler;
    }

    private void handleRightClick(double px, double py, double screenX, double screenY) {
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx != null) {
            OcoGttOrder ocoHit = orderLineLayer.hitTestOco(px, py, ctx);
            if (ocoHit != null && onOcoCancelled != null) {
                onOcoCancelled.accept(ocoHit);
                return;
            }
            GttOrder gttHit = orderLineLayer.hitTestGtt(px, py, ctx);
            if (gttHit != null && onGttCancelled != null) {
                onGttCancelled.accept(gttHit);
                return;
            }
            Order orderHit = orderLineLayer.hitTestOrder(px, py, ctx);
            if (orderHit != null && onOrderCancelled != null) {
                onOrderCancelled.accept(orderHit);
                return;
            }
        }
        if (drawingManager != null) {
            float[] priceRange = getCurrentPriceRange();
            if (priceRange != null) {
                CoordinateMapper mapper = buildMapper(priceRange);
                if (mapper != null) {
                    Drawing hit = drawingManager.findDrawingAt(px, py, DrawingManager.mainCanvasId(), mapper);
                    if (hit != null) {
                        drawingManager.removeDrawing(hit);
                        render();
                        return;
                    }
                }
            }
        }
        if (onContextMenuRequested != null) {
            onContextMenuRequested.show(screenX, screenY);
        }
    }

    @FunctionalInterface
    public interface ContextMenuRequester {
        void show(double screenX, double screenY);
    }

    private void handleDrawingClick() {
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        double value = yToPrice(mouseY, priceRange);
        int canvasId = DrawingManager.mainCanvasId();
        Drawing result = drawingManager.handleClick(timestamp, value, canvasId, null);
        if (result != null && result.isComplete()) {
            if (result.getTool().getType().isTextInput()) {
                promptDrawingText(result);
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
        if (drawingManager == null || drawingManager.getActiveTool() == null) {
            previewAnchor = null;
            return;
        }
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0) { previewAnchor = null; return; }
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) { previewAnchor = null; return; }
        double value = yToPrice(mouseY, priceRange);
        previewAnchor = drawingManager.buildPreviewAnchor(timestamp, value);
    }

    private boolean tryStartEdgeDrag(double px) {
        if (volumeProfileData == null) return false;
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return false;
        EdgeHit hit = volumeProfileLayer.hitTestEdge(px, ctx);
        if (hit == EdgeHit.NONE) return false;
        draggingEdge = hit;
        return true;
    }

    private void updateEdgeDrag(double currentX) {
        if (volumeProfileData == null) return;
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return;
        int barIndex = VolumeProfileLayer.xToBarIndex(currentX, ctx);
        if (barIndex < 0) barIndex = 0;
        if (draggingEdge == EdgeHit.LEFT) {
            if (barIndex < volumeProfileData.getToBar()) {
                volumeProfileData.setFromBar(barIndex);
            }
        } else if (draggingEdge == EdgeHit.RIGHT) {
            if (barIndex > volumeProfileData.getFromBar()) {
                volumeProfileData.setToBar(barIndex);
            }
        }
    }

    private boolean tryStartProfileDrag(double px, double py) {
        if (volumeProfileData == null) return false;
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return false;
        if (!volumeProfileLayer.isHit(px, py, ctx)) return false;
        draggingProfile = true;
        profileDragStartX = px;
        return true;
    }

    private void updateProfileDrag(double currentX) {
        if (volumeProfileData == null) return;
        double delta = currentX - profileDragStartX;
        volumeProfileData.addOffsetX(delta);
        profileDragStartX = currentX;
    }

    private void convertProfileOffsetToBarShift() {
        if (volumeProfileData == null) return;
        double offsetX = volumeProfileData.getOffsetX();
        if (Math.abs(offsetX) < 1) return;
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        double barWidth = viewport.computeBarWidth(chartW);
        if (barWidth <= 0) return;
        int barShift = (int) (offsetX / barWidth);
        volumeProfileData.setFromBar(volumeProfileData.getFromBar() + barShift);
        volumeProfileData.setToBar(volumeProfileData.getToBar() + barShift);
        volumeProfileData.resetOffsetX();
    }

    private boolean tryStartDrawingDrag(double px, double py) {
        long timestamp = getExactTimestamp(px);
        if (timestamp < 0) return false;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return false;
        CoordinateMapper mapper = buildMapper(priceRange);
        if (mapper == null) return false;
        int canvasId = DrawingManager.mainCanvasId();
        DrawingManager.HitResult hit = drawingManager.findHitAt(px, py, canvasId, mapper);
        if (hit != null && hit.anchorIndex() == RulerDrawing.ADD_BUTTON_HIT) {
            if (onRulerAddButtonClicked != null) {
                Drawing d = hit.drawing();
                onRulerAddButtonClicked.handle(d, px, py);
            }
            return true;
        }
        double value = yToPrice(py, priceRange);
        return drawingManager.tryStartDrag(px, py, canvasId, timestamp, value, mapper);
    }

    private void updateDrawingDrag() {
        long timestamp = getExactTimestamp(mouseX);
        if (timestamp < 0) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        double value = yToPrice(mouseY, priceRange);
        drawingManager.updateDrag(timestamp, value);
    }

    public long getHoveredTimestamp() {
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
        if (px < 0 || px > chartW) return -1;
        Bars bars = dataProvider.peekBars();
        if (bars == null || bars.size() == 0) return -1;
        double barWidth = viewport.computeBarWidth(chartW);
        double fractionalOffset = px / barWidth;
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

    private double yToPrice(double y, float[] priceRange) {
        double chartH = getHeight() - TIME_AXIS_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
        double ratio = (PADDING_TOP + chartH - y) / chartH;
        if (logScale && priceRange[0] > 0) {
            double logMin = Math.log(priceRange[0]);
            double logMax = Math.log(priceRange[1]);
            return Math.exp(logMin + ratio * (logMax - logMin));
        }
        return priceRange[0] + ratio * (priceRange[1] - priceRange[0]);
    }

    private double priceToY(double price, float[] priceRange, double chartH) {
        if (logScale && priceRange[0] > 0 && price > 0) {
            double logMin = Math.log(priceRange[0]);
            double logMax = Math.log(priceRange[1]);
            double logSpan = logMax - logMin;
            if (logSpan == 0) logSpan = 1;
            return PADDING_TOP + chartH * (1 - (Math.log(price) - logMin) / logSpan);
        }
        float priceSpan = priceRange[1] - priceRange[0];
        return PADDING_TOP + chartH * (1 - (price - priceRange[0]) / priceSpan);
    }

    private CoordinateMapper buildMapper(float[] priceRange) {
        double chartW = getWidth() - PRICE_AXIS_WIDTH;
        double chartH = getHeight() - TIME_AXIS_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
        if (chartW <= 0 || chartH <= 0) return null;
        double barWidth = viewport.computeBarWidth(chartW);
        int viewStart = viewport.getStartIndex();
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
            public double toY(double value) {
                return priceToY(value, priceRange, chartH);
            }
            @Override
            public double chartWidth() { return chartW; }
            @Override
            public double chartHeight() { return chartH + PADDING_TOP; }
            @Override
            public int countBars(long ts1, long ts2) {
                double idx1 = findFractionalBarIndex(bars, cachedFrom, ts1, tfSeconds);
                double idx2 = findFractionalBarIndex(bars, cachedFrom, ts2, tfSeconds);
                return (int) Math.abs(idx2 - idx1);
            }
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

    private float[] getCurrentPriceRange() {
        int start = viewport.getStartIndex();
        int end = viewport.getEndIndex();
        if (start >= end) return null;
        Bars bars = dataProvider.peekBars();
        if (bars == null) return null;
        return computePriceRange(bars, start, end);
    }

    private boolean tryHandleOrderLineClick(double px, double py) {
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return false;
        Order hit = orderLineLayer.hitTestOrder(px, py, ctx);
        if (hit == null) return false;
        float price = hit.getPrice() > 0 ? hit.getPrice() : hit.getTriggerPrice();
        if (orderLineLayer.hitTestCancelAt(px, py, ctx, price)) {
            if (onOrderCancelled != null) onOrderCancelled.accept(hit);
            return true;
        }
        draggingOrder = hit;
        dragOriginalOrderPrice = price;
        return true;
    }

    private boolean tryStartGttDrag(double px, double py) {
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return false;
        GttOrder hit = orderLineLayer.hitTestGtt(px, py, ctx);
        if (hit == null) return false;
        if (orderLineLayer.hitTestCancelAt(px, py, ctx, hit.getTriggerPrice())) {
            if (onGttCancelled != null) onGttCancelled.accept(hit);
            return true;
        }
        draggingGttOrder = hit;
        dragOriginalTriggerPrice = hit.getTriggerPrice();
        return true;
    }

    private void updateGttDrag(double py) {
        if (draggingGttOrder == null) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        float newPrice = (float) yToPrice(py, priceRange);
        if (newPrice > 0) {
            draggingGttOrder.setTriggerPrice(newPrice);
            draggingGttOrder.setOrderPrice(newPrice);
        }
    }

    private void endGttDrag() {
        if (draggingGttOrder == null) return;
        GttOrder order = draggingGttOrder;
        float newPrice = order.getTriggerPrice();
        draggingGttOrder = null;
        order.setTriggerPrice(dragOriginalTriggerPrice);
        order.setOrderPrice(dragOriginalTriggerPrice);
        render();
        if (Math.abs(newPrice - dragOriginalTriggerPrice) > 0.001f) {
            GttOrder modified = GttOrder.builder()
                    .id(order.getId())
                    .scripId(order.getScripId())
                    .side(order.getSide())
                    .limitType(order.getLimitType())
                    .product(order.getProduct())
                    .quantity(order.getQuantity())
                    .triggerPrice(newPrice)
                    .orderPrice(newPrice)
                    .lastPrice(order.getLastPrice())
                    .trailingPoints(order.getTrailingPoints())
                    .status(order.getStatus())
                    .expiresAt(order.getExpiresAt())
                    .build();
            if (onGttModified != null) onGttModified.accept(modified);
        }
    }

    private boolean tryStartOcoDrag(double px, double py) {
        ChartContext ctx = buildContext(getWidth(), getHeight());
        if (ctx == null) return false;
        OcoGttOrder hit = orderLineLayer.hitTestOco(px, py, ctx);
        if (hit == null) return false;
        draggingOcoStoplossLeg = orderLineLayer.isOcoStoplossLeg(hit, py, ctx);
        float legPrice = draggingOcoStoplossLeg
                ? hit.getStoplossTriggerPrice() : hit.getTargetTriggerPrice();
        if (orderLineLayer.hitTestCancelAt(px, py, ctx, legPrice)) {
            if (onOcoCancelled != null) onOcoCancelled.accept(hit);
            return true;
        }
        draggingOcoOrder = hit;
        dragOriginalOcoPrice = legPrice;
        return true;
    }

    private void updateOcoDrag(double py) {
        if (draggingOcoOrder == null) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        float newPrice = (float) yToPrice(py, priceRange);
        if (newPrice > 0) {
            if (draggingOcoStoplossLeg) {
                draggingOcoOrder.setStoplossTriggerPrice(newPrice);
                draggingOcoOrder.setStoplossOrderPrice(newPrice);
            } else {
                draggingOcoOrder.setTargetTriggerPrice(newPrice);
                draggingOcoOrder.setTargetOrderPrice(newPrice);
            }
        }
    }

    private void endOcoDrag() {
        if (draggingOcoOrder == null) return;
        OcoGttOrder oco = draggingOcoOrder;
        draggingOcoOrder = null;
        float newSlTrigger = oco.getStoplossTriggerPrice();
        float newTgtTrigger = oco.getTargetTriggerPrice();
        if (draggingOcoStoplossLeg) {
            oco.setStoplossTriggerPrice(dragOriginalOcoPrice);
            oco.setStoplossOrderPrice(dragOriginalOcoPrice);
        } else {
            oco.setTargetTriggerPrice(dragOriginalOcoPrice);
            oco.setTargetOrderPrice(dragOriginalOcoPrice);
        }
        render();
        float currentLegPrice = draggingOcoStoplossLeg ? newSlTrigger : newTgtTrigger;
        if (Math.abs(currentLegPrice - dragOriginalOcoPrice) > 0.001f) {
            OcoGttOrder modified = OcoGttOrder.builder()
                    .id(oco.getId())
                    .scripId(oco.getScripId())
                    .side(oco.getSide())
                    .limitType(oco.getLimitType())
                    .product(oco.getProduct())
                    .quantity(oco.getQuantity())
                    .stoplossTriggerPrice(draggingOcoStoplossLeg ? currentLegPrice : oco.getStoplossTriggerPrice())
                    .stoplossOrderPrice(draggingOcoStoplossLeg ? currentLegPrice : oco.getStoplossOrderPrice())
                    .targetTriggerPrice(draggingOcoStoplossLeg ? oco.getTargetTriggerPrice() : currentLegPrice)
                    .targetOrderPrice(draggingOcoStoplossLeg ? oco.getTargetOrderPrice() : currentLegPrice)
                    .lastPrice(oco.getLastPrice())
                    .trailingPoints(oco.getTrailingPoints())
                    .status(oco.getStatus())
                    .expiresAt(oco.getExpiresAt())
                    .build();
            if (onOcoModified != null) onOcoModified.accept(modified);
        } else {
            if (onOcoModified != null) onOcoModified.accept(oco);
        }
    }

    private void updateOrderDrag(double py) {
        if (draggingOrder == null) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        float newPrice = (float) yToPrice(py, priceRange);
        if (newPrice > 0) {
            draggingOrder.setPrice(newPrice);
        }
    }

    private void endOrderDrag() {
        if (draggingOrder == null) return;
        Order order = draggingOrder;
        float newPrice = order.getPrice();
        draggingOrder = null;
        order.setPrice(dragOriginalOrderPrice);
        render();
        if (Math.abs(newPrice - dragOriginalOrderPrice) > 0.001f) {
            Order modified = Order.builder()
                    .id(order.getId())
                    .exchangeOrderId(order.getExchangeOrderId())
                    .scripId(order.getScripId())
                    .side(order.getSide())
                    .limitType(order.getLimitType())
                    .product(order.getProduct())
                    .variety(order.getVariety())
                    .validity(order.getValidity())
                    .status(order.getStatus())
                    .quantity(order.getQuantity())
                    .price(newPrice)
                    .triggerPrice(order.getTriggerPrice())
                    .build();
            if (onOrderModified != null) onOrderModified.accept(modified);
        }
    }

    private void handlePricePickerClick() {
        if (pricePickerMode == null) return;
        float price = getHoveredPrice();
        if (price <= 0) return;
        PricePickerMode mode = pricePickerMode;
        pricePickerMode = null;
        render();
        if (onPricePicked != null) onPricePicked.accept(mode, price);
    }

    private void renderPricePickerLine(GraphicsContext gc, ChartContext ctx) {
        if (pricePickerMode == null || mouseY < 0) return;
        float[] priceRange = getCurrentPriceRange();
        if (priceRange == null) return;
        float price = (float) yToPrice(mouseY, priceRange);
        if (price <= 0) return;
        double y = ctx.toY(price);
        if (y < PADDING_TOP || y > PADDING_TOP + ctx.chartHeight()) return;
        Color lineColor = resolvePickerColor(pricePickerMode);
        gc.setStroke(lineColor);
        gc.setLineWidth(ORDER_LINE_WIDTH);
        gc.setLineDashes(PRICE_PICKER_DASH[0], PRICE_PICKER_DASH[1]);
        gc.strokeLine(0, y, ctx.chartWidth(), y);
        gc.setLineDashes(null);
        String label = String.format("%.2f", price);
        gc.setFont(Font.font(PRICE_PICKER_LABEL_FONT_SIZE));
        double textWidth = label.length() * PRICE_PICKER_LABEL_FONT_SIZE * 0.6;
        double labelWidth = textWidth + ORDER_LINE_LABEL_PADDING * 2;
        double lx = ctx.chartWidth() - labelWidth;
        double ly = y - ORDER_LINE_LABEL_HEIGHT / 2;
        gc.setFill(lineColor);
        gc.fillRect(lx, ly, labelWidth, ORDER_LINE_LABEL_HEIGHT);
        gc.setFill(ORDER_LINE_LABEL_TEXT);
        gc.fillText(label, lx + ORDER_LINE_LABEL_PADDING, ly + ORDER_LINE_LABEL_HEIGHT - 3);
    }

    private Color resolvePickerColor(PricePickerMode mode) {
        return switch (mode) {
            case BUY -> ORDER_LINE_BUY;
            case SELL -> ORDER_LINE_SELL;
            case GTT_BUY -> GTT_LINE_BUY;
            case GTT_SELL -> GTT_LINE_SELL;
            case OCO_FIRST, OCO_SECOND -> GTT_LINE_SELL;
        };
    }

    private void renderDrawings(GraphicsContext gc, ChartContext ctx) {
        if (drawingManager == null) return;
        float[] priceRange = new float[]{ctx.minPrice(), ctx.maxPrice()};
        CoordinateMapper mapper = buildMapper(priceRange);
        if (mapper == null) return;
        int canvasId = DrawingManager.mainCanvasId();
        DrawingRenderer.renderDrawings(gc, drawingManager.getDrawingsForCanvas(canvasId),
                mapper, drawingManager.getPendingDrawing(), previewAnchor);
    }

}
