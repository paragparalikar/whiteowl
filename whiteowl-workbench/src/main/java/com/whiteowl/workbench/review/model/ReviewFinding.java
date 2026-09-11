package com.whiteowl.workbench.review.model;

import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.portfolio.service.EffectiveQuantityCalculator;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(of = "id")
public abstract class ReviewFinding {

    private static final String ERR_NO_SCRIP = "Cannot resolve scrip for autofix";

    private final String id;
    private final ReviewFindingCode code;
    private final Scrip scrip;
    private final String message;
    @Setter private boolean selected;

    public abstract String describeAction(AutofixContext context);

    public abstract void fix(AutofixContext context);

    public static void checkAll(ReviewContext context, List<ReviewFinding> findings) {
        MissingStopLossFinding.check(context, findings);
        UnderCoveredFinding.check(context, findings);
        OverCoveredFinding.check(context, findings);
        OrphanGttFinding.check(context, findings);
        WideStopLossFinding.check(context, findings);
        TightStopLossFinding.check(context, findings);
        ConcentratedPositionFinding.check(context, findings);
        StalePositionFinding.check(context, findings);
        ExpiringGttFinding.check(context, findings);
        MissingTargetFinding.check(context, findings);
        UnfavorableRiskRewardFinding.check(context, findings);
    }

    protected int computeEffectiveExposure(List<Holding> holdings, List<Position> positions, List<Order> sellOrders) {
        int exposure = EffectiveQuantityCalculator.computeAll(holdings, positions)
                .values().stream().mapToInt(Integer::intValue).sum();
        int pendingSellQty = 0;
        for (Order order : sellOrders) {
            pendingSellQty += order.getQuantity() - order.getFilledQuantity() - order.getCancelledQuantity();
        }
        return Math.max(0, exposure - pendingSellQty);
    }

    protected float computeLastPrice(List<Holding> holdings, List<Position> positions) {
        for (Holding h : holdings) {
            if (h.getLastPrice() > 0) return h.getLastPrice();
        }
        for (Position p : positions) {
            if (p.getLastPrice() > 0) return p.getLastPrice();
        }
        return 0;
    }

    protected String resolveSymbol(Scrip scrip) {
        if (scrip == null) return ERR_NO_SCRIP;
        return scrip.getSymbol() != null ? scrip.getSymbol() : scrip.getId();
    }

}
