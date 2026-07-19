package com.whiteowl.core.portfolio.service;

import static com.whiteowl.core.order.model.Product.MIS;

import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EffectiveQuantityCalculator {

    private EffectiveQuantityCalculator() {
    }

    public static Map<String, Integer> computeAll(List<Holding> holdings, List<Position> positions) {
        Map<String, Integer> map = new HashMap<>();
        Set<String> holdingScripIds = new HashSet<>();
        for (Holding h : holdings) {
            if (h.getScrip() == null || h.getScrip().getId() == null) continue;
            int qty = h.getQuantity() + h.getT1Quantity();
            if (qty == 0) continue;
            String scripId = h.getScrip().getId();
            holdingScripIds.add(scripId);
            map.merge(scripId, qty, Integer::sum);
        }
        for (Position p : positions) {
            if (p.getScrip() == null || p.getScrip().getId() == null) continue;
            if (p.getProduct() == MIS) continue;
            String scripId = p.getScrip().getId();
            if (holdingScripIds.contains(scripId)) {
                int intradayDelta = p.getQuantity() - p.getOvernightQuantity();
                if (intradayDelta != 0) {
                    map.merge(scripId, intradayDelta, Integer::sum);
                }
            } else {
                if (p.getQuantity() != 0) {
                    map.merge(scripId, p.getQuantity(), Integer::sum);
                }
            }
        }
        map.entrySet().removeIf(e -> e.getValue() <= 0);
        return map;
    }

    public static int computeForScrip(String scripId, List<Holding> holdings, List<Position> positions) {
        int holdingQty = 0;
        for (Holding h : holdings) {
            if (h.getScrip() != null && scripId.equals(h.getScrip().getId())) {
                holdingQty += h.getQuantity() + h.getT1Quantity();
            }
        }
        boolean hasHoldings = holdingQty > 0;
        int positionQty = 0;
        for (Position p : positions) {
            if (p.getScrip() == null || !scripId.equals(p.getScrip().getId())) continue;
            if (p.getProduct() == MIS) continue;
            if (hasHoldings) {
                int intradayDelta = p.getQuantity() - p.getOvernightQuantity();
                positionQty += intradayDelta;
            } else {
                positionQty += p.getQuantity();
            }
        }
        return Math.max(0, holdingQty + positionQty);
    }

}
