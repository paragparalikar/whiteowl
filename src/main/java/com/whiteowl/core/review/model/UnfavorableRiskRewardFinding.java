package com.whiteowl.core.review.model;

import static com.whiteowl.core.review.model.ReviewFindingCode.UNFAVORABLE_RISK_REWARD;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public final class UnfavorableRiskRewardFinding extends ReviewFinding {

    private static final String MSG_TEMPLATE =
            "%s: Reward:Risk ratio %.2f:1 is below minimum %.1f:1 (target=%.2f, SL=%.2f, avg buy=%.2f)";
    private static final String ACTION_TEMPLATE =
            "Review risk/reward for %s — consider adjusting target or stop loss";

    private final float avgBuyPrice;
    private final float stopLossTrigger;
    private final float targetTrigger;
    private final double actualRatio;

    public UnfavorableRiskRewardFinding(String id, Scrip scrip, String message,
                                        float avgBuyPrice, float stopLossTrigger,
                                        float targetTrigger, double actualRatio) {
        super(id, UNFAVORABLE_RISK_REWARD, scrip, message);
        this.avgBuyPrice = avgBuyPrice;
        this.stopLossTrigger = stopLossTrigger;
        this.targetTrigger = targetTrigger;
        this.actualRatio = actualRatio;
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(ACTION_TEMPLATE, resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        double minRatio = ctx.getReviewConfig().minRiskRewardRatio();
        Set<String> checked = new java.util.HashSet<>();
        checkFromGtts(ctx, findings, minRatio, checked);
        checkFromOcoGtts(ctx, findings, minRatio, checked);
    }

    private static void checkFromGtts(ReviewContext ctx, List<ReviewFinding> findings,
                                       double minRatio, Set<String> checked) {
        for (Map.Entry<String, List<GttOrder>> entry : ctx.getActiveGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            float avgBuyPrice = computeAvgBuyPrice(ctx, scripId);
            if (avgBuyPrice <= 0) continue;
            float bestSl = findBestStopLoss(entry.getValue(), avgBuyPrice);
            float bestTarget = findBestTarget(entry.getValue(), avgBuyPrice);
            if (bestSl <= 0 || bestTarget <= 0) continue;
            evaluateRatio(ctx, findings, scripId, avgBuyPrice, bestSl, bestTarget, minRatio);
            checked.add(scripId);
        }
    }

    private static void checkFromOcoGtts(ReviewContext ctx, List<ReviewFinding> findings,
                                          double minRatio, Set<String> checked) {
        for (Map.Entry<String, List<OcoGttOrder>> entry : ctx.getActiveOcoGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            if (checked.contains(scripId)) continue;
            float avgBuyPrice = computeAvgBuyPrice(ctx, scripId);
            if (avgBuyPrice <= 0) continue;
            for (OcoGttOrder oco : entry.getValue()) {
                float sl = oco.getStoplossTriggerPrice();
                float target = oco.getTargetTriggerPrice();
                if (sl <= 0 || sl >= avgBuyPrice) continue;
                if (target <= 0 || target <= avgBuyPrice) continue;
                evaluateRatio(ctx, findings, scripId, avgBuyPrice, sl, target, minRatio);
            }
        }
    }

    private static void evaluateRatio(ReviewContext ctx, List<ReviewFinding> findings,
                                       String scripId, float avgBuyPrice,
                                       float sl, float target, double minRatio) {
        double risk = avgBuyPrice - sl;
        double reward = target - avgBuyPrice;
        if (risk <= 0) return;
        double ratio = reward / risk;
        if (ratio >= minRatio) return;
        Scrip scrip = ctx.resolveScrip(scripId);
        String symbol = scrip.getSymbol() != null ? scrip.getSymbol() : scripId;
        String message = String.format(MSG_TEMPLATE, symbol, ratio, minRatio, target, sl, avgBuyPrice);
        findings.add(new UnfavorableRiskRewardFinding(ctx.generateId(), scrip, message,
                avgBuyPrice, sl, target, ratio));
    }

    private static float findBestStopLoss(List<GttOrder> gtts, float avgBuyPrice) {
        float bestSl = 0;
        for (GttOrder gtt : gtts) {
            float trigger = gtt.getTriggerPrice();
            if (trigger > 0 && trigger < avgBuyPrice) {
                if (bestSl == 0 || trigger > bestSl) {
                    bestSl = trigger;
                }
            }
        }
        return bestSl;
    }

    private static float findBestTarget(List<GttOrder> gtts, float avgBuyPrice) {
        float bestTarget = 0;
        for (GttOrder gtt : gtts) {
            float trigger = gtt.getTriggerPrice();
            if (trigger > avgBuyPrice) {
                if (bestTarget == 0 || trigger < bestTarget) {
                    bestTarget = trigger;
                }
            }
        }
        return bestTarget;
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
