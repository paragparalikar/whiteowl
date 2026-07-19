package com.whiteowl.core.review.model;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public final class ReviewContext {

    private final Map<String, Integer> netExposureByScripId;
    private final Map<String, Integer> stopLossQtyByScripId;
    private final Map<String, List<GttOrder>> activeGttsByScripId;
    private final Map<String, List<OcoGttOrder>> activeOcoGttsByScripId;
    private final Map<String, List<Order>> sellOrdersByScripId;
    private final Map<String, List<Holding>> holdingsByScripId;
    private final Map<String, List<Position>> positionsByScripId;
    private final Map<String, Integer> pendingSellQtyByScripId;
    private final Map<String, Integer> targetQtyByScripId;
    private final Set<String> exposedScripIds;
    private final ScripRepository scripRepository;
    private final ReviewConfig reviewConfig;

    public ReviewContext(Map<String, Integer> netExposureByScripId,
                         Map<String, Integer> stopLossQtyByScripId,
                         Map<String, List<GttOrder>> activeGttsByScripId,
                         Map<String, List<OcoGttOrder>> activeOcoGttsByScripId,
                         Map<String, List<Order>> sellOrdersByScripId,
                         Map<String, List<Holding>> holdingsByScripId,
                         Map<String, List<Position>> positionsByScripId,
                         Map<String, Integer> pendingSellQtyByScripId,
                         Map<String, Integer> targetQtyByScripId,
                         Set<String> exposedScripIds,
                         ScripRepository scripRepository,
                         ReviewConfig reviewConfig) {
        this.netExposureByScripId = netExposureByScripId;
        this.stopLossQtyByScripId = stopLossQtyByScripId;
        this.activeGttsByScripId = activeGttsByScripId;
        this.activeOcoGttsByScripId = activeOcoGttsByScripId;
        this.sellOrdersByScripId = sellOrdersByScripId;
        this.holdingsByScripId = holdingsByScripId;
        this.positionsByScripId = positionsByScripId;
        this.pendingSellQtyByScripId = pendingSellQtyByScripId;
        this.targetQtyByScripId = targetQtyByScripId;
        this.exposedScripIds = exposedScripIds;
        this.scripRepository = scripRepository;
        this.reviewConfig = reviewConfig;
    }

    public Scrip resolveScrip(String scripId) {
        if (scripId == null) return buildUnknownScrip(scripId);
        return scripRepository.findById(scripId).orElseGet(() -> buildUnknownScrip(scripId));
    }

    private Scrip buildUnknownScrip(String scripId) {
        return Scrip.builder().id(scripId).symbol(scripId).name(scripId).build();
    }

    public String generateId() {
        return UUID.randomUUID().toString();
    }

}
