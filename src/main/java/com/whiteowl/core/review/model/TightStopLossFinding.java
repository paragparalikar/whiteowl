package com.whiteowl.core.review.model;

import static com.whiteowl.core.review.model.ReviewFindingCode.TIGHT_STOP_LOSS;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public final class TightStopLossFinding extends ReviewFinding {

    private static final String MSG_TEMPLATE = "%s: SL trigger %.2f is only %.1f%% below avg buy price %.2f (threshold: %.1f%%)";
    private static final String ACTION_TEMPLATE = "Review stop loss for %s — trigger may be too close to entry";

    private final GttOrder gttOrder;
    private final float avgBuyPrice;
    private final double gapPercentage;

    public TightStopLossFinding(String id, Scrip scrip, String message, GttOrder gttOrder,
                                 float avgBuyPrice, double gapPercentage) {
        super(id, TIGHT_STOP_LOSS, scrip, message);
        this.gttOrder = gttOrder;
        this.avgBuyPrice = avgBuyPrice;
        this.gapPercentage = gapPercentage;
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(ACTION_TEMPLATE, resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        double threshold = ctx.getReviewConfig().tightStopLossPercentage();
        for (Map.Entry<String, List<GttOrder>> entry : ctx.getActiveGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            float avgBuyPrice = computeAvgBuyPrice(ctx, scripId);
            if (avgBuyPrice <= 0) continue;
            for (GttOrder gtt : entry.getValue()) {
                double gapPct = ((avgBuyPrice - gtt.getTriggerPrice()) / avgBuyPrice) * 100.0;
                if (gapPct >= 0 && gapPct < threshold) {
                    Scrip scrip = ctx.resolveScrip(scripId);
                    String symbol = scrip.getSymbol() != null ? scrip.getSymbol() : scripId;
                    String message = String.format(MSG_TEMPLATE, symbol,
                            gtt.getTriggerPrice(), gapPct, avgBuyPrice, threshold);
                    findings.add(new TightStopLossFinding(ctx.generateId(), scrip, message,
                            gtt, avgBuyPrice, gapPct));
                }
            }
        }
    }

    private static float computeAvgBuyPrice(ReviewContext ctx, String scripId) {
        List<Holding> holdings = ctx.getHoldingsByScripId().getOrDefault(scripId, List.of());
        int totalQty = 0;
        float totalValue = 0;
        for (Holding h : holdings) {
            int qty = h.getQuantity() + h.getT1Quantity();
            if (qty > 0 && h.getAveragePrice() > 0) {
                totalQty += qty;
                totalValue += qty * h.getAveragePrice();
            }
        }
        return totalQty > 0 ? totalValue / totalQty : 0;
    }

}
