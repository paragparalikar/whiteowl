package com.whiteowl.core.review.model;

import static com.whiteowl.core.order.model.LimitType.LIMIT;
import static com.whiteowl.core.order.model.OrderSide.SELL;
import static com.whiteowl.core.order.model.Product.CNC;
import static com.whiteowl.core.review.model.ReviewFindingCode.MISSING_TARGET;

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
public final class MissingTargetFinding extends ReviewFinding {

    private static final String MSG = "%s: No target/profit-booking GTT found (net exposure: %d)";
    private static final String FIX_DESC = "Create target SELL GTT for %s: qty=%d, trigger=%.2f";
    private static final String ERR_NO_PRICE = "Cannot determine buy price for %s";
    private static final String EXPIRES_AT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int DEFAULT_EXPIRY_YEARS = 1;
    private static final double DEFAULT_TARGET_PERCENTAGE = 10.0;

    private final List<Holding> holdings;
    private final List<Position> positions;
    private final List<Order> sellOrders;

    public MissingTargetFinding(String id, Scrip scrip, String message,
                                 List<Holding> holdings, List<Position> positions,
                                 List<Order> sellOrders) {
        super(id, MISSING_TARGET, scrip, message);
        this.holdings = holdings;
        this.positions = positions;
        this.sellOrders = sellOrders;
    }

    @Override
    public String describeAction(AutofixContext context) {
        int qty = computeEffectiveExposure(holdings, positions, sellOrders);
        float triggerPrice = computeTargetPrice(context.stopLossPercentage());
        return String.format(FIX_DESC, resolveSymbol(getScrip()), qty, triggerPrice);
    }

    @Override
    public void fix(AutofixContext context) {
        int qty = computeEffectiveExposure(holdings, positions, sellOrders);
        if (qty <= 0) return;
        float triggerPrice = computeTargetPrice(context.stopLossPercentage());
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
        log.info("Created target GTT for {}: qty={}, trigger={}", getScrip().getId(), qty, triggerPrice);
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        for (String scripId : ctx.getExposedScripIds()) {
            int targetQty = ctx.getTargetQtyByScripId().getOrDefault(scripId, 0);
            if (targetQty > 0) continue;
            int exposure = ctx.getNetExposureByScripId().getOrDefault(scripId, 0);
            int pendingSellQty = ctx.getPendingSellQtyByScripId().getOrDefault(scripId, 0);
            int effectiveExposure = exposure - pendingSellQty;
            if (effectiveExposure <= 0) continue;
            Scrip scrip = ctx.resolveScrip(scripId);
            findings.add(new MissingTargetFinding(ctx.generateId(), scrip,
                    String.format(MSG, scripId, exposure),
                    ctx.getHoldingsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getPositionsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getSellOrdersByScripId().getOrDefault(scripId, List.of())));
        }
    }

    private float computeTargetPrice(double stopLossPercentage) {
        float avgBuyPrice = computeWeightedAvgPrice();
        if (avgBuyPrice <= 0) return 0;
        double targetPct = Math.max(stopLossPercentage, DEFAULT_TARGET_PERCENTAGE);
        float rawTarget = avgBuyPrice * (1 + (float) targetPct / 100);
        return roundToTickSize(rawTarget);
    }

    private float computeWeightedAvgPrice() {
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
        return totalQty > 0 ? totalCost / totalQty : 0;
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
