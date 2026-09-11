package com.whiteowl.workbench.review.model;

import static com.whiteowl.workbench.review.model.ReviewFindingCode.STALE_POSITION;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Getter
public final class StalePositionFinding extends ReviewFinding {

    private static final String MSG_TEMPLATE = "%s: Held for %d days with %.1f%% returns (threshold: %d days)";
    private static final String ACTION_TEMPLATE = "Review stale position in %s — consider exiting or revising thesis";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final long daysHeld;
    private final double returnPercentage;

    public StalePositionFinding(String id, Scrip scrip, String message,
                                 long daysHeld, double returnPercentage) {
        super(id, STALE_POSITION, scrip, message);
        this.daysHeld = daysHeld;
        this.returnPercentage = returnPercentage;
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(ACTION_TEMPLATE, resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        int thresholdDays = ctx.getReviewConfig().staleHoldingDays();
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, List<GttOrder>> entry : ctx.getActiveGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            checkStaleness(ctx, findings, scripId, thresholdDays, today);
        }
        for (String scripId : ctx.getHoldingsByScripId().keySet()) {
            if (ctx.getActiveGttsByScripId().containsKey(scripId)) continue;
            checkStaleness(ctx, findings, scripId, thresholdDays, today);
        }
    }

    private static void checkStaleness(ReviewContext ctx, List<ReviewFinding> findings,
                                        String scripId, int thresholdDays, LocalDate today) {
        List<Holding> holdings = ctx.getHoldingsByScripId().getOrDefault(scripId, List.of());
        if (holdings.isEmpty()) return;
        long oldestDays = computeOldestHoldingDays(ctx, scripId, today);
        if (oldestDays < thresholdDays) return;
        double returnPct = computeReturnPercentage(holdings);
        if (returnPct >= 0) return;
        Scrip scrip = ctx.resolveScrip(scripId);
        String symbol = scrip.getSymbol() != null ? scrip.getSymbol() : scripId;
        String message = String.format(MSG_TEMPLATE, symbol, oldestDays, returnPct, thresholdDays);
        findings.add(new StalePositionFinding(ctx.generateId(), scrip, message, oldestDays, returnPct));
    }

    private static long computeOldestHoldingDays(ReviewContext ctx, String scripId, LocalDate today) {
        List<GttOrder> gtts = ctx.getActiveGttsByScripId().getOrDefault(scripId, List.of());
        long oldestDays = 0;
        for (GttOrder gtt : gtts) {
            if (gtt.getCreatedAt() == null) continue;
            try {
                LocalDate created = LocalDate.parse(gtt.getCreatedAt().substring(0, 10), DATE_FORMAT);
                long days = ChronoUnit.DAYS.between(created, today);
                oldestDays = Math.max(oldestDays, days);
            } catch (Exception ignored) {
            }
        }
        if (oldestDays > 0) return oldestDays;
        List<Holding> holdings = ctx.getHoldingsByScripId().getOrDefault(scripId, List.of());
        for (Holding h : holdings) {
            if (h.getAveragePrice() > 0 && h.getLastPrice() > 0) {
                return thresholdFallback(holdings);
            }
        }
        return 0;
    }

    private static long thresholdFallback(List<Holding> holdings) {
        int totalQty = 0;
        for (Holding h : holdings) {
            totalQty += h.getQuantity() + h.getT1Quantity();
        }
        return totalQty > 0 ? Integer.MAX_VALUE : 0;
    }

    private static double computeReturnPercentage(List<Holding> holdings) {
        float totalCost = 0;
        float totalCurrentValue = 0;
        for (Holding h : holdings) {
            int qty = h.getQuantity() + h.getT1Quantity();
            if (qty <= 0 || h.getAveragePrice() <= 0) continue;
            totalCost += qty * h.getAveragePrice();
            totalCurrentValue += qty * h.getLastPrice();
        }
        if (totalCost <= 0) return 0;
        return ((totalCurrentValue - totalCost) / totalCost) * 100.0;
    }

}
