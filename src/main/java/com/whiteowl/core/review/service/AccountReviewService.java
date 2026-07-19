package com.whiteowl.core.review.service;

import static com.whiteowl.core.gtt.model.GttStatus.ACTIVE;
import static com.whiteowl.core.order.model.OrderSide.SELL;
import static com.whiteowl.core.order.model.Product.MIS;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.portfolio.service.EffectiveQuantityCalculator;
import com.whiteowl.core.review.model.ReviewConfig;
import com.whiteowl.core.review.model.ReviewContext;
import com.whiteowl.core.review.model.ReviewFinding;
import com.whiteowl.core.scrip.repository.ScripRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AccountReviewService {

    private final ScripRepository scripRepository;

    public AccountReviewService(ScripRepository scripRepository) {
        this.scripRepository = scripRepository;
    }

    public List<ReviewFinding> runReview(List<Holding> holdings, List<Position> positions,
                                         List<GttOrder> gtts, List<OcoGttOrder> ocoGtts,
                                         List<Order> orders, ReviewConfig reviewConfig) {
        ReviewContext context = buildContext(holdings, positions, gtts, ocoGtts, orders, reviewConfig);
        List<ReviewFinding> findings = new ArrayList<>();
        ReviewFinding.checkAll(context, findings);
        return findings;
    }

    private ReviewContext buildContext(List<Holding> holdings, List<Position> positions,
                                       List<GttOrder> gtts, List<OcoGttOrder> ocoGtts,
                                       List<Order> orders, ReviewConfig reviewConfig) {
        Map<String, List<Holding>> holdingsByScripId = buildHoldingsMap(holdings);
        Map<String, List<Position>> positionsByScripId = buildPositionsMap(positions);
        Map<String, Integer> netExposureByScripId = EffectiveQuantityCalculator.computeAll(holdings, positions);
        Map<String, List<GttOrder>> activeGttsByScripId = buildActiveGttMap(gtts);
        Map<String, Integer> stopLossQtyByScripId = buildStopLossQtyMap(activeGttsByScripId);
        mergeOcoStopLossQty(ocoGtts, stopLossQtyByScripId);
        Map<String, List<OcoGttOrder>> activeOcoGttsByScripId = buildActiveOcoGttMap(ocoGtts);
        Map<String, Integer> targetQtyByScripId = buildTargetQtyMap(activeGttsByScripId, holdingsByScripId);
        mergeOcoTargetQty(ocoGtts, targetQtyByScripId);
        Map<String, List<Order>> sellOrdersByScripId = buildSellOrderMap(orders);
        Map<String, Integer> pendingSellQtyByScripId = buildPendingSellQtyMap(sellOrdersByScripId);
        Set<String> exposedScripIds = new HashSet<>(netExposureByScripId.keySet());
        return new ReviewContext(netExposureByScripId, stopLossQtyByScripId,
                activeGttsByScripId, activeOcoGttsByScripId, sellOrdersByScripId,
                holdingsByScripId, positionsByScripId,
                pendingSellQtyByScripId, targetQtyByScripId,
                exposedScripIds, scripRepository, reviewConfig);
    }

    private Map<String, List<Holding>> buildHoldingsMap(List<Holding> holdings) {
        Map<String, List<Holding>> map = new HashMap<>();
        for (Holding h : holdings) {
            if (h.getScrip() == null) continue;
            int totalQty = h.getQuantity() + h.getT1Quantity();
            if (totalQty <= 0) continue;
            map.computeIfAbsent(h.getScrip().getId(), k -> new ArrayList<>()).add(h);
        }
        return map;
    }

    private Map<String, List<Position>> buildPositionsMap(List<Position> positions) {
        Map<String, List<Position>> map = new HashMap<>();
        for (Position p : positions) {
            if (p.getScrip() == null || p.getProduct() == MIS) continue;
            if (p.getQuantity() <= 0) continue;
            map.computeIfAbsent(p.getScrip().getId(), k -> new ArrayList<>()).add(p);
        }
        return map;
    }

    private Map<String, List<GttOrder>> buildActiveGttMap(List<GttOrder> gtts) {
        Map<String, List<GttOrder>> map = new HashMap<>();
        for (GttOrder gtt : gtts) {
            if (gtt.getScripId() == null) continue;
            if (gtt.getSide() == SELL && gtt.getStatus() == ACTIVE) {
                map.computeIfAbsent(gtt.getScripId(), k -> new ArrayList<>()).add(gtt);
            }
        }
        return map;
    }

    private Map<String, Integer> buildStopLossQtyMap(Map<String, List<GttOrder>> activeGttsByScripId) {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, List<GttOrder>> entry : activeGttsByScripId.entrySet()) {
            int totalQty = 0;
            for (GttOrder gtt : entry.getValue()) {
                totalQty += gtt.getQuantity();
            }
            map.put(entry.getKey(), totalQty);
        }
        return map;
    }

    private Map<String, List<OcoGttOrder>> buildActiveOcoGttMap(List<OcoGttOrder> ocoGtts) {
        Map<String, List<OcoGttOrder>> map = new HashMap<>();
        for (OcoGttOrder oco : ocoGtts) {
            if (oco.getScripId() == null) continue;
            if (oco.getSide() == SELL && oco.getStatus() == ACTIVE) {
                map.computeIfAbsent(oco.getScripId(), k -> new ArrayList<>()).add(oco);
            }
        }
        return map;
    }

    private void mergeOcoStopLossQty(List<OcoGttOrder> ocoGtts, Map<String, Integer> stopLossQtyByScripId) {
        for (OcoGttOrder oco : ocoGtts) {
            if (oco.getScripId() == null) continue;
            if (oco.getSide() != SELL || oco.getStatus() != ACTIVE) continue;
            stopLossQtyByScripId.merge(oco.getScripId(), oco.getQuantity(), Integer::sum);
        }
    }

    private Map<String, Integer> buildTargetQtyMap(Map<String, List<GttOrder>> activeGttsByScripId,
                                                    Map<String, List<Holding>> holdingsByScripId) {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, List<GttOrder>> entry : activeGttsByScripId.entrySet()) {
            String scripId = entry.getKey();
            float avgBuyPrice = computeAvgBuyPrice(holdingsByScripId.getOrDefault(scripId, List.of()));
            for (GttOrder gtt : entry.getValue()) {
                if (avgBuyPrice > 0 && gtt.getTriggerPrice() > avgBuyPrice) {
                    map.merge(scripId, gtt.getQuantity(), Integer::sum);
                }
            }
        }
        return map;
    }

    private void mergeOcoTargetQty(List<OcoGttOrder> ocoGtts, Map<String, Integer> targetQtyByScripId) {
        for (OcoGttOrder oco : ocoGtts) {
            if (oco.getScripId() == null) continue;
            if (oco.getSide() != SELL || oco.getStatus() != ACTIVE) continue;
            if (oco.getTargetTriggerPrice() > 0) {
                targetQtyByScripId.merge(oco.getScripId(), oco.getQuantity(), Integer::sum);
            }
        }
    }

    private float computeAvgBuyPrice(List<Holding> holdings) {
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

    private Map<String, List<Order>> buildSellOrderMap(List<Order> orders) {
        Map<String, List<Order>> map = new HashMap<>();
        for (Order order : orders) {
            if (order.getScripId() == null) continue;
            if (order.getSide() == SELL && !order.getStatus().isTerminal()) {
                map.computeIfAbsent(order.getScripId(), k -> new ArrayList<>()).add(order);
            }
        }
        return map;
    }

    private Map<String, Integer> buildPendingSellQtyMap(Map<String, List<Order>> sellOrdersByScripId) {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, List<Order>> entry : sellOrdersByScripId.entrySet()) {
            int pendingQty = 0;
            for (Order o : entry.getValue()) {
                pendingQty += o.getQuantity() - o.getFilledQuantity() - o.getCancelledQuantity();
            }
            if (pendingQty > 0) {
                map.put(entry.getKey(), pendingQty);
            }
        }
        return map;
    }

}
