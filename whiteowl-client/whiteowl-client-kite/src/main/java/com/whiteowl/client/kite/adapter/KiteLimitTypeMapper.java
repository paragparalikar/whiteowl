package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteLimitType;
import com.whiteowl.core.order.model.LimitType;

final class KiteLimitTypeMapper {

    public LimitType toLimitType(KiteLimitType kiteLimitType) {
        if (kiteLimitType == null) return null;
        return switch (kiteLimitType) {
            case MARKET -> LimitType.MARKET;
            case LIMIT -> LimitType.LIMIT;
            case SL -> LimitType.STOP_LOSS;
            case SLM -> LimitType.STOP_LOSS_MARKET;
        };
    }

    public KiteLimitType toKiteLimitType(LimitType limitType) {
        if (limitType == null) return null;
        return switch (limitType) {
            case MARKET -> KiteLimitType.MARKET;
            case LIMIT -> KiteLimitType.LIMIT;
            case STOP_LOSS -> KiteLimitType.SL;
            case STOP_LOSS_MARKET -> KiteLimitType.SLM;
        };
    }

}
