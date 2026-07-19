package com.whiteowl.core.review.model;

import static com.whiteowl.core.review.model.ReviewFindingCode.CONCENTRATED_POSITION;

import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public final class ConcentratedPositionFinding extends ReviewFinding {

    private static final String MSG_TEMPLATE = "%s: Position value ₹%.0f is %.1f%% of portfolio (threshold: %.1f%%)";
    private static final String ACTION_TEMPLATE = "Consider reducing position in %s to improve diversification";

    private final double positionValue;
    private final double concentrationPercentage;

    public ConcentratedPositionFinding(String id, Scrip scrip, String message,
                                        double positionValue, double concentrationPercentage) {
        super(id, CONCENTRATED_POSITION, scrip, message);
        this.positionValue = positionValue;
        this.concentrationPercentage = concentrationPercentage;
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(ACTION_TEMPLATE, resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        double threshold = ctx.getReviewConfig().maxConcentrationPercentage();
        double totalPortfolioValue = computeTotalPortfolioValue(ctx);
        if (totalPortfolioValue <= 0) return;
        for (Map.Entry<String, Integer> entry : ctx.getNetExposureByScripId().entrySet()) {
            String scripId = entry.getKey();
            double scripValue = computeScripValue(ctx, scripId);
            if (scripValue <= 0) continue;
            double concentrationPct = (scripValue / totalPortfolioValue) * 100.0;
            if (concentrationPct > threshold) {
                Scrip scrip = ctx.resolveScrip(scripId);
                String symbol = scrip.getSymbol() != null ? scrip.getSymbol() : scripId;
                String message = String.format(MSG_TEMPLATE, symbol,
                        scripValue, concentrationPct, threshold);
                findings.add(new ConcentratedPositionFinding(ctx.generateId(), scrip, message,
                        scripValue, concentrationPct));
            }
        }
    }

    private static double computeTotalPortfolioValue(ReviewContext ctx) {
        double total = 0;
        for (String scripId : ctx.getNetExposureByScripId().keySet()) {
            total += computeScripValue(ctx, scripId);
        }
        return total;
    }

    private static double computeScripValue(ReviewContext ctx, String scripId) {
        int qty = ctx.getNetExposureByScripId().getOrDefault(scripId, 0);
        if (qty <= 0) return 0;
        float lastPrice = resolveLastPrice(ctx, scripId);
        return qty * (double) lastPrice;
    }

    private static float resolveLastPrice(ReviewContext ctx, String scripId) {
        List<Holding> holdings = ctx.getHoldingsByScripId().getOrDefault(scripId, List.of());
        for (Holding h : holdings) {
            if (h.getLastPrice() > 0) return h.getLastPrice();
        }
        List<Position> positions = ctx.getPositionsByScripId().getOrDefault(scripId, List.of());
        for (Position p : positions) {
            if (p.getLastPrice() > 0) return p.getLastPrice();
        }
        return 0;
    }

}
