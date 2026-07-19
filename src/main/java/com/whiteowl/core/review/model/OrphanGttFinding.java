package com.whiteowl.core.review.model;

import static com.whiteowl.core.review.model.ReviewFindingCode.ORPHAN_GTT;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public final class OrphanGttFinding extends ReviewFinding {

    private static final String MSG = "%s: SELL GTT #%d (qty: %d) has no matching holding or position";
    private static final String FIX_DESC = "Cancel orphan GTT #%d for %s";

    private final GttOrder gttOrder;
    private final List<Order> sellOrders;

    public OrphanGttFinding(String id, Scrip scrip, String message,
                             GttOrder gttOrder, List<Order> sellOrders) {
        super(id, ORPHAN_GTT, scrip, message);
        this.gttOrder = gttOrder;
        this.sellOrders = Collections.unmodifiableList(sellOrders);
    }

    @Override
    public String describeAction(AutofixContext context) {
        return String.format(FIX_DESC, gttOrder.getId(), resolveSymbol(getScrip()));
    }

    @Override
    public void fix(AutofixContext context) {
        context.chartOrderService().cancelGtt(gttOrder.getId());
        log.info("Cancelled orphan GTT #{} for {}", gttOrder.getId(), gttOrder.getScripId());
    }

    public static void check(ReviewContext ctx, List<ReviewFinding> findings) {
        for (Map.Entry<String, List<GttOrder>> entry : ctx.getActiveGttsByScripId().entrySet()) {
            String scripId = entry.getKey();
            if (ctx.getExposedScripIds().contains(scripId)) continue;
            if (ctx.getHoldingsByScripId().containsKey(scripId)) continue;
            if (ctx.getPositionsByScripId().containsKey(scripId)) continue;
            List<Order> scripOrders = ctx.getSellOrdersByScripId().getOrDefault(scripId, List.of());
            for (GttOrder gtt : entry.getValue()) {
                Scrip scrip = ctx.resolveScrip(gtt.getScripId());
                findings.add(new OrphanGttFinding(ctx.generateId(), scrip,
                        String.format(MSG, gtt.getScripId(), gtt.getId(), gtt.getQuantity()),
                        gtt, scripOrders));
            }
        }
    }

}
