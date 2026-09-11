package com.whiteowl.workbench.review.model;

import static com.whiteowl.core.order.model.LimitType.LIMIT;
import static com.whiteowl.core.order.model.OrderSide.SELL;
import static com.whiteowl.core.order.model.Product.CNC;
import static com.whiteowl.workbench.review.model.ReviewFindingCode.MISSING_STOP_LOSS;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Getter
public final class MissingStopLossFinding extends ReviewFinding {

    private static final String MSG = "%s: No stop loss GTT found (net exposure: %d)";
    private static final String FIX_DESC = "Create SELL GTT for %s: qty=%d, trigger=%.2f";
    private static final String ERR_NO_PRICE = "Cannot determine buy price for %s";
    private static final String EXPIRES_AT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int DEFAULT_EXPIRY_YEARS = 1;

    private final List<Holding> holdings;
    private final List<Position> positions;
    private final List<GttOrder> activeGtts;
    private final List<Order> sellOrders;

    public MissingStopLossFinding(String id, Scrip scrip, String message,
                                   List<Holding> holdings, List<Position> positions,
                                   List<GttOrder> activeGtts, List<Order> sellOrders) {
        super(id, MISSING_STOP_LOSS, scrip, message);
        this.holdings = holdings;
        this.positions = positions;
        this.activeGtts = activeGtts;
        this.sellOrders = sellOrders;
    }

    @Override
    public String describeAction(AutofixContext context) {
        int qty = computeEffectiveExposure(holdings, positions, sellOrders);
        float triggerPrice = computeTriggerPrice(context.stopLossPercentage());
        return String.format(FIX_DESC, resolveSymbol(getScrip()), qty, triggerPrice);
    }

    @Override
    public void fix(AutofixContext context) {
        int qty = computeEffectiveExposure(holdings, positions, sellOrders);
        if (qty <= 0) return;
        float triggerPrice = computeTriggerPrice(context.stopLossPercentage());
        if (triggerPrice <= 0) {
            throw new IllegalStateException(String.format(ERR_NO_PRICE, resolveSymbol(getScrip())));
        }
        GttOrder gtt = GttOrder.builder()
                .scripId(getScrip().getId())
                .side(SELL)
                .limitType(LIMIT)
                .product(CNC)
                .quantity(qty)
                .triggerPrice(triggerPrice)
                .orderPrice(triggerPrice)
                .lastPrice(computeLastPrice(holdings, positions))
                .expiresAt(computeDefaultExpiry())
                .build();
        context.chartOrderService().createGtt(gtt);
        log.info("Created SL GTT for {}: qty={}, trigger={}", getScrip().getId(), qty, triggerPrice);
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        for (String scripId : ctx.getExposedScripIds()) {
            int slQty = ctx.getStopLossQtyByScripId().getOrDefault(scripId, 0);
            if (slQty > 0) continue;
            int exposure = ctx.getNetExposureByScripId().getOrDefault(scripId, 0);
            int pendingSellQty = ctx.getPendingSellQtyByScripId().getOrDefault(scripId, 0);
            int effectiveExposure = exposure - pendingSellQty;
            if (effectiveExposure <= 0) continue;
            Scrip scrip = ctx.resolveScrip(scripId);
            findings.add(new MissingStopLossFinding(ctx.generateId(), scrip,
                    String.format(MSG, scripId, exposure),
                    ctx.getHoldingsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getPositionsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getActiveGttsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getSellOrdersByScripId().getOrDefault(scripId, List.of())));
        }
    }

    private float computeTriggerPrice(double stopLossPercentage) {
        float totalCost = 0;
        int totalQty = 0;
        for (Holding h : holdings) {
            int qty = h.getQuantity() + h.getT1Quantity();
            if (qty > 0 && h.getAveragePrice() > 0) {
                totalCost += h.getAveragePrice() * qty;
                totalQty += qty;
            }
        }
        for (Position p : positions) {
            if (p.getQuantity() > 0 && p.getAveragePrice() > 0) {
                totalCost += p.getAveragePrice() * p.getQuantity();
                totalQty += p.getQuantity();
            }
        }
        if (totalQty == 0 || totalCost <= 0) return 0;
        float weightedAvgPrice = totalCost / totalQty;
        float rawTrigger = weightedAvgPrice * (1 - (float) stopLossPercentage / 100);
        return roundToTickSize(rawTrigger);
    }

    private float roundToTickSize(float price) {
        Scrip scrip = getScrip();
        float tickSize = scrip != null ? scrip.getTickSize() : 0;
        if (tickSize <= 0) tickSize = 0.05f;
        BigDecimal bdPrice = new BigDecimal(String.valueOf(price));
        BigDecimal bdTick = new BigDecimal(String.valueOf(tickSize));
        BigDecimal rounded = bdPrice.divide(bdTick, 0, RoundingMode.HALF_UP).multiply(bdTick);
        return rounded.floatValue();
    }

    private String computeDefaultExpiry() {
        return LocalDateTime.now().plusYears(DEFAULT_EXPIRY_YEARS)
                .format(DateTimeFormatter.ofPattern(EXPIRES_AT_FORMAT));
    }

}
