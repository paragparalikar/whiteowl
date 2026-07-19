package com.whiteowl.core.review.model;

import static com.whiteowl.core.review.model.ReviewFindingCode.EXPIRING_GTT;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Getter
public final class ExpiringGttFinding extends ReviewFinding {

    private static final String MSG_TEMPLATE = "%s: GTT #%d expires on %s (%d days remaining, threshold: %d days)";
    private static final String ACTION_TEMPLATE = "Renew expiring GTT for %s before protection lapses";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final GttOrder gttOrder;
    private final long daysRemaining;

    public ExpiringGttFinding(String id, Scrip scrip, String message,
                               GttOrder gttOrder, long daysRemaining) {
        super(id, EXPIRING_GTT, scrip, message);
        this.gttOrder = gttOrder;
        this.daysRemaining = daysRemaining;
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(ACTION_TEMPLATE, resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        int warningDays = ctx.getReviewConfig().gttExpiryWarningDays();
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, List<GttOrder>> entry : ctx.getActiveGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            for (GttOrder gtt : entry.getValue()) {
                if (gtt.getExpiresAt() == null) continue;
                long daysRemaining = computeDaysRemaining(gtt.getExpiresAt(), today);
                if (daysRemaining < 0) continue;
                if (daysRemaining <= warningDays) {
                    Scrip scrip = ctx.resolveScrip(scripId);
                    String symbol = scrip.getSymbol() != null ? scrip.getSymbol() : scripId;
                    String expiryDate = gtt.getExpiresAt().length() >= 10
                            ? gtt.getExpiresAt().substring(0, 10) : gtt.getExpiresAt();
                    String message = String.format(MSG_TEMPLATE, symbol,
                            gtt.getId(), expiryDate, daysRemaining, warningDays);
                    findings.add(new ExpiringGttFinding(ctx.generateId(), scrip, message,
                            gtt, daysRemaining));
                }
            }
        }
    }

    private static long computeDaysRemaining(String expiresAt, LocalDate today) {
        try {
            LocalDate expiryDate = LocalDate.parse(expiresAt.substring(0, 10), DATE_FORMAT);
            return ChronoUnit.DAYS.between(today, expiryDate);
        } catch (Exception e) {
            return -1;
        }
    }

}
