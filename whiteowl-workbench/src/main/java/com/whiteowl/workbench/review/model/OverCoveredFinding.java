package com.whiteowl.workbench.review.model;

import static com.whiteowl.workbench.review.model.ReviewFindingCode.OVER_COVERED;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Getter
public final class OverCoveredFinding extends ReviewFinding {

    private static final String MSG = "%s: Stop loss qty %d exceeds net exposure %d (unexplained excess: %d)";
    private static final String FIX_MODIFY = "Modify GTT #%d for %s: qty %d → %d";
    private static final String FIX_CANCEL = "Cancel GTT #%d for %s (qty: %d)";

    private final List<Holding> holdings;
    private final List<Position> positions;
    private final List<GttOrder> activeGtts;
    private final List<Order> sellOrders;

    public OverCoveredFinding(String id, Scrip scrip, String message,
                               List<Holding> holdings, List<Position> positions,
                               List<GttOrder> activeGtts, List<Order> sellOrders) {
        super(id, OVER_COVERED, scrip, message);
        this.holdings = holdings;
        this.positions = positions;
        this.activeGtts = activeGtts;
        this.sellOrders = sellOrders;
    }

    @Override
    public String describeAction(AutofixContext context) {
        int excess = computeExcess();
        String symbol = resolveSymbol(getScrip());
        List<GttOrder> sorted = sortByTriggerPriceDesc();
        StringBuilder sb = new StringBuilder();
        int remaining = excess;
        for (GttOrder gtt : sorted) {
            if (remaining <= 0) break;
            if (sb.length() > 0) sb.append("; ");
            if (gtt.getQuantity() <= remaining) {
                sb.append(String.format(FIX_CANCEL, gtt.getId(), symbol, gtt.getQuantity()));
                remaining -= gtt.getQuantity();
            } else {
                int newQty = gtt.getQuantity() - remaining;
                sb.append(String.format(FIX_MODIFY, gtt.getId(), symbol, gtt.getQuantity(), newQty));
                remaining = 0;
            }
        }
        return sb.toString();
    }

    @Override
    public void fix(AutofixContext context) {
        int excess = computeExcess();
        if (excess <= 0) return;
        List<GttOrder> sorted = sortByTriggerPriceDesc();
        int remaining = excess;
        for (GttOrder gtt : sorted) {
            if (remaining <= 0) break;
            if (gtt.getQuantity() <= remaining) {
                context.chartOrderService().cancelGtt(gtt.getId());
                log.info("Cancelled over-covered GTT #{} for {} (qty: {})", gtt.getId(), gtt.getScripId(), gtt.getQuantity());
                remaining -= gtt.getQuantity();
            } else {
                int newQty = gtt.getQuantity() - remaining;
                GttOrder updated = GttOrder.builder()
                        .id(gtt.getId())
                        .scripId(gtt.getScripId())
                        .side(gtt.getSide())
                        .limitType(gtt.getLimitType())
                        .product(gtt.getProduct())
                        .quantity(newQty)
                        .triggerPrice(gtt.getTriggerPrice())
                        .orderPrice(gtt.getOrderPrice())
                        .lastPrice(gtt.getLastPrice())
                        .trailingPoints(gtt.getTrailingPoints())
                        .expiresAt(gtt.getExpiresAt())
                        .build();
                context.chartOrderService().updateGtt(updated);
                log.info("Reduced GTT #{} for {}: qty {} → {}", gtt.getId(), gtt.getScripId(), gtt.getQuantity(), newQty);
                remaining = 0;
            }
        }
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        for (String scripId : ctx.getExposedScripIds()) {
            int slQty = ctx.getStopLossQtyByScripId().getOrDefault(scripId, 0);
            if (slQty <= 0) continue;
            int exposure = ctx.getNetExposureByScripId().getOrDefault(scripId, 0);
            if (slQty <= exposure) continue;
            int pendingSellQty = ctx.getPendingSellQtyByScripId().getOrDefault(scripId, 0);
            if (slQty <= exposure + pendingSellQty) continue;
            int unexplainedExcess = slQty - exposure - pendingSellQty;
            Scrip scrip = ctx.resolveScrip(scripId);
            findings.add(new OverCoveredFinding(ctx.generateId(), scrip,
                    String.format(MSG, scripId, slQty, exposure, unexplainedExcess),
                    ctx.getHoldingsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getPositionsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getActiveGttsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getSellOrdersByScripId().getOrDefault(scripId, List.of())));
        }
    }

    private int computeExcess() {
        int effectiveExposure = computeEffectiveExposure(holdings, positions, sellOrders);
        int totalSlQty = activeGtts.stream().mapToInt(GttOrder::getQuantity).sum();
        return totalSlQty - effectiveExposure;
    }

    private List<GttOrder> sortByTriggerPriceDesc() {
        List<GttOrder> sorted = new ArrayList<>(activeGtts);
        sorted.sort(Comparator.comparingDouble(GttOrder::getTriggerPrice).reversed());
        return sorted;
    }

}
