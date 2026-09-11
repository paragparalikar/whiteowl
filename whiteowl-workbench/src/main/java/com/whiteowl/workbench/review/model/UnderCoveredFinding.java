package com.whiteowl.workbench.review.model;

import static com.whiteowl.workbench.review.model.ReviewFindingCode.UNDER_COVERED;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Getter
public final class UnderCoveredFinding extends ReviewFinding {

    private static final String MSG = "%s: Stop loss qty %d is less than net exposure %d (gap: %d)";
    private static final String FIX_DESC = "Modify GTT #%d for %s: qty %d → %d";

    private final List<Holding> holdings;
    private final List<Position> positions;
    private final List<GttOrder> activeGtts;
    private final List<Order> sellOrders;

    public UnderCoveredFinding(String id, Scrip scrip, String message,
                                List<Holding> holdings, List<Position> positions,
                                List<GttOrder> activeGtts, List<Order> sellOrders) {
        super(id, UNDER_COVERED, scrip, message);
        this.holdings = holdings;
        this.positions = positions;
        this.activeGtts = activeGtts;
        this.sellOrders = sellOrders;
    }

    @Override
    public String describeAction(AutofixContext context) {
        GttOrder target = findLowestTriggerGtt();
        int gap = computeGap();
        int newQty = target.getQuantity() + gap;
        return String.format(FIX_DESC, target.getId(), resolveSymbol(getScrip()), target.getQuantity(), newQty);
    }

    @Override
    public void fix(AutofixContext context) {
        GttOrder target = findLowestTriggerGtt();
        int gap = computeGap();
        if (gap <= 0) return;
        int newQty = target.getQuantity() + gap;
        GttOrder updated = GttOrder.builder()
                .id(target.getId())
                .scripId(target.getScripId())
                .side(target.getSide())
                .limitType(target.getLimitType())
                .product(target.getProduct())
                .quantity(newQty)
                .triggerPrice(target.getTriggerPrice())
                .orderPrice(target.getOrderPrice())
                .lastPrice(target.getLastPrice())
                .trailingPoints(target.getTrailingPoints())
                .expiresAt(target.getExpiresAt())
                .build();
        context.chartOrderService().updateGtt(updated);
        log.info("Updated GTT #{} for {}: qty {} → {}", target.getId(), target.getScripId(), target.getQuantity(), newQty);
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        for (String scripId : ctx.getExposedScripIds()) {
            int slQty = ctx.getStopLossQtyByScripId().getOrDefault(scripId, 0);
            if (slQty <= 0) continue;
            int exposure = ctx.getNetExposureByScripId().getOrDefault(scripId, 0);
            int pendingSellQty = ctx.getPendingSellQtyByScripId().getOrDefault(scripId, 0);
            int effectiveExposure = exposure - pendingSellQty;
            if (effectiveExposure <= 0 || slQty >= effectiveExposure) continue;
            Scrip scrip = ctx.resolveScrip(scripId);
            findings.add(new UnderCoveredFinding(ctx.generateId(), scrip,
                    String.format(MSG, scripId, slQty, exposure, effectiveExposure - slQty),
                    ctx.getHoldingsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getPositionsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getActiveGttsByScripId().getOrDefault(scripId, List.of()),
                    ctx.getSellOrdersByScripId().getOrDefault(scripId, List.of())));
        }
    }

    private int computeGap() {
        int effectiveExposure = computeEffectiveExposure(holdings, positions, sellOrders);
        int totalSlQty = activeGtts.stream().mapToInt(GttOrder::getQuantity).sum();
        return effectiveExposure - totalSlQty;
    }

    private GttOrder findLowestTriggerGtt() {
        return activeGtts.stream()
                .min(Comparator.comparingDouble(GttOrder::getTriggerPrice))
                .orElseThrow(() -> new IllegalStateException("No active GTTs found"));
    }

}
