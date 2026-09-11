package com.whiteowl.workbench.charting.renderer;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.order.model.OrderStatus;
import com.whiteowl.workbench.charting.ChartContext;
import com.whiteowl.workbench.charting.ChartLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Collections;
import java.util.List;

public final class OrderLineLayer implements ChartLayer {

    private static final String BUY_PREFIX = "B";
    private static final String SELL_PREFIX = "S";
    private static final String GTT_PREFIX = "GTT ";
    private static final String OCO_SL_PREFIX = "OCO SL ";
    private static final String OCO_TGT_PREFIX = "OCO TGT ";
    private static final String TRAILING_SUFFIX = " TSL";
    private static final String QTY_SEPARATOR = " × ";
    private static final String CANCEL_LABEL = "×";
    private static final double CANCEL_BOX_SIZE = 14;
    private static final double CANCEL_BOX_GAP = 2;
    private static final float PRICE_FORMAT_THRESHOLD = 1000f;

    private List<GttOrder> gttOrders = Collections.emptyList();
    private List<OcoGttOrder> ocoOrders = Collections.emptyList();
    private List<Order> regularOrders = Collections.emptyList();

    public void setGttOrders(List<GttOrder> orders) {
        this.gttOrders = orders != null ? orders : Collections.emptyList();
    }

    public List<GttOrder> getGttOrders() {
        return gttOrders;
    }

    public void setRegularOrders(List<Order> orders) {
        this.regularOrders = orders != null ? orders : Collections.emptyList();
    }

    public void setOcoOrders(List<OcoGttOrder> orders) {
        this.ocoOrders = orders != null ? orders : Collections.emptyList();
    }

    public List<OcoGttOrder> getOcoOrders() {
        return ocoOrders;
    }

    public List<Order> getRegularOrders() {
        return regularOrders;
    }

    @Override
    public void render(GraphicsContext gc, ChartContext ctx) {
        for (GttOrder order : gttOrders) {
            if (order.getStatus() != GttStatus.ACTIVE) continue;
            renderGttLine(gc, ctx, order);
        }
        for (OcoGttOrder oco : ocoOrders) {
            if (oco.getStatus() != GttStatus.ACTIVE) continue;
            renderOcoLines(gc, ctx, oco);
        }
        for (Order order : regularOrders) {
            if (order.getStatus().isTerminal()) continue;
            renderOrderLine(gc, ctx, order);
        }
    }

    private void renderGttLine(GraphicsContext gc, ChartContext ctx, GttOrder order) {
        boolean isBuy = order.getSide() == OrderSide.BUY;
        Color lineColor = isBuy ? GTT_LINE_BUY : GTT_LINE_SELL;
        Color bgColor = isBuy ? GTT_LINE_BUY_BG : GTT_LINE_SELL_BG;
        double y = ctx.toY(order.getTriggerPrice());
        if (y < PADDING_TOP || y > PADDING_TOP + ctx.chartHeight()) return;
        gc.setFill(bgColor);
        gc.fillRect(0, y - ORDER_LINE_LABEL_HEIGHT / 2, ctx.chartWidth(), ORDER_LINE_LABEL_HEIGHT);
        gc.setStroke(lineColor);
        gc.setLineWidth(ORDER_LINE_WIDTH);
        gc.setLineDashes(GTT_LINE_DASH[0], GTT_LINE_DASH[1]);
        gc.strokeLine(0, y, ctx.chartWidth(), y);
        gc.setLineDashes(null);
        String sideText = GTT_PREFIX + (isBuy ? BUY_PREFIX : SELL_PREFIX);
        String trailTag = order.getTrailingPoints() > 0 ? TRAILING_SUFFIX : "";
        String label = sideText + QTY_SEPARATOR + order.getQuantity() + " @ " + formatPrice(order.getTriggerPrice()) + trailTag;
        renderLabelBox(gc, ctx, y, lineColor, label);
        renderCancelBox(gc, ctx, y, lineColor);
    }

    private void renderOrderLine(GraphicsContext gc, ChartContext ctx, Order order) {
        boolean isBuy = order.getSide() == OrderSide.BUY;
        Color lineColor = isBuy ? ORDER_LINE_BUY : ORDER_LINE_SELL;
        Color bgColor = isBuy ? ORDER_LINE_BUY_BG : ORDER_LINE_SELL_BG;
        float price = order.getPrice() > 0 ? order.getPrice() : order.getTriggerPrice();
        if (price <= 0) return;
        double y = ctx.toY(price);
        if (y < PADDING_TOP || y > PADDING_TOP + ctx.chartHeight()) return;
        gc.setFill(bgColor);
        gc.fillRect(0, y - ORDER_LINE_LABEL_HEIGHT / 2, ctx.chartWidth(), ORDER_LINE_LABEL_HEIGHT);
        gc.setStroke(lineColor);
        gc.setLineWidth(ORDER_LINE_WIDTH);
        gc.setLineDashes(ORDER_LINE_DASH[0], ORDER_LINE_DASH[1]);
        gc.strokeLine(0, y, ctx.chartWidth(), y);
        gc.setLineDashes(null);
        String sideText = isBuy ? BUY_PREFIX : SELL_PREFIX;
        String label = sideText + QTY_SEPARATOR + order.getQuantity() + " @ " + formatPrice(price);
        renderLabelBox(gc, ctx, y, lineColor, label);
        renderCancelBox(gc, ctx, y, lineColor);
    }

    private void renderLabelBox(GraphicsContext gc, ChartContext ctx, double y, Color lineColor, String label) {
        gc.setFont(Font.font(ORDER_LINE_LABEL_FONT_SIZE));
        double textWidth = label.length() * ORDER_LINE_LABEL_FONT_SIZE * 0.6;
        double labelWidth = textWidth + ORDER_LINE_LABEL_PADDING * 2;
        double lx = ctx.chartWidth() - labelWidth;
        double ly = y - ORDER_LINE_LABEL_HEIGHT / 2;
        gc.setFill(lineColor);
        gc.fillRect(lx, ly, labelWidth, ORDER_LINE_LABEL_HEIGHT);
        gc.setFill(ORDER_LINE_LABEL_TEXT);
        gc.fillText(label, lx + ORDER_LINE_LABEL_PADDING, ly + ORDER_LINE_LABEL_HEIGHT - 3);
    }

    private void renderCancelBox(GraphicsContext gc, ChartContext ctx, double y, Color lineColor) {
        double cx = ctx.chartWidth() + CANCEL_BOX_GAP;
        double cy = y - CANCEL_BOX_SIZE / 2;
        gc.setFill(lineColor);
        gc.fillRect(cx, cy, CANCEL_BOX_SIZE, CANCEL_BOX_SIZE);
        gc.setFill(ORDER_LINE_LABEL_TEXT);
        gc.setFont(Font.font(ORDER_LINE_LABEL_FONT_SIZE));
        gc.fillText(CANCEL_LABEL, cx + 3, cy + CANCEL_BOX_SIZE - 3);
    }

    private void renderOcoLines(GraphicsContext gc, ChartContext ctx, OcoGttOrder oco) {
        boolean isBuy = oco.getSide() == OrderSide.BUY;
        Color lineColor = isBuy ? GTT_LINE_BUY : GTT_LINE_SELL;
        Color bgColor = isBuy ? GTT_LINE_BUY_BG : GTT_LINE_SELL_BG;
        float trailing = oco.getTrailingPoints();
        renderOcoLeg(gc, ctx, oco.getStoplossTriggerPrice(), oco.getQuantity(),
                lineColor, bgColor, OCO_SL_PREFIX, isBuy, trailing);
        renderOcoLeg(gc, ctx, oco.getTargetTriggerPrice(), oco.getQuantity(),
                lineColor, bgColor, OCO_TGT_PREFIX, isBuy, trailing);
    }

    private void renderOcoLeg(GraphicsContext gc, ChartContext ctx, float triggerPrice,
                              int quantity, Color lineColor, Color bgColor,
                              String prefix, boolean isBuy, float trailing) {
        double y = ctx.toY(triggerPrice);
        if (y < PADDING_TOP || y > PADDING_TOP + ctx.chartHeight()) return;
        gc.setFill(bgColor);
        gc.fillRect(0, y - ORDER_LINE_LABEL_HEIGHT / 2, ctx.chartWidth(), ORDER_LINE_LABEL_HEIGHT);
        gc.setStroke(lineColor);
        gc.setLineWidth(ORDER_LINE_WIDTH);
        gc.setLineDashes(GTT_LINE_DASH[0], GTT_LINE_DASH[1]);
        gc.strokeLine(0, y, ctx.chartWidth(), y);
        gc.setLineDashes(null);
        String sideText = prefix + (isBuy ? BUY_PREFIX : SELL_PREFIX);
        String trailTag = trailing > 0 ? TRAILING_SUFFIX : "";
        String label = sideText + QTY_SEPARATOR + quantity + " @ " + formatPrice(triggerPrice) + trailTag;
        renderLabelBox(gc, ctx, y, lineColor, label);
        renderCancelBox(gc, ctx, y, lineColor);
    }

    public OcoGttOrder hitTestOco(double px, double py, ChartContext ctx) {
        for (OcoGttOrder oco : ocoOrders) {
            if (oco.getStatus() != GttStatus.ACTIVE) continue;
            double slY = ctx.toY(oco.getStoplossTriggerPrice());
            if (Math.abs(py - slY) <= ORDER_LINE_HIT_RADIUS) return oco;
            double tgtY = ctx.toY(oco.getTargetTriggerPrice());
            if (Math.abs(py - tgtY) <= ORDER_LINE_HIT_RADIUS) return oco;
        }
        return null;
    }

    public boolean isOcoStoplossLeg(OcoGttOrder oco, double py, ChartContext ctx) {
        double slY = ctx.toY(oco.getStoplossTriggerPrice());
        double tgtY = ctx.toY(oco.getTargetTriggerPrice());
        return Math.abs(py - slY) <= Math.abs(py - tgtY);
    }

    public GttOrder hitTestGtt(double px, double py, ChartContext ctx) {
        for (GttOrder order : gttOrders) {
            if (order.getStatus() != GttStatus.ACTIVE) continue;
            double y = ctx.toY(order.getTriggerPrice());
            if (Math.abs(py - y) <= ORDER_LINE_HIT_RADIUS) {
                return order;
            }
        }
        return null;
    }

    public Order hitTestOrder(double px, double py, ChartContext ctx) {
        for (Order order : regularOrders) {
            if (order.getStatus().isTerminal()) continue;
            float price = order.getPrice() > 0 ? order.getPrice() : order.getTriggerPrice();
            if (price <= 0) continue;
            double y = ctx.toY(price);
            if (Math.abs(py - y) <= ORDER_LINE_HIT_RADIUS) {
                return order;
            }
        }
        return null;
    }

    public boolean hitTestCancelAt(double px, double py, ChartContext ctx, double price) {
        double y = ctx.toY((float) price);
        double cx = ctx.chartWidth() + CANCEL_BOX_GAP;
        double cy = y - CANCEL_BOX_SIZE / 2;
        return px >= cx && px <= cx + CANCEL_BOX_SIZE && py >= cy && py <= cy + CANCEL_BOX_SIZE;
    }

    private String formatPrice(float price) {
        return price >= PRICE_FORMAT_THRESHOLD ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
