package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteOrderVariety;
import com.whiteowl.core.order.model.Variety;

final class KiteVarietyMapper {

    public Variety toVariety(KiteOrderVariety kiteVariety) {
        if (kiteVariety == null) return null;
        return switch (kiteVariety) {
            case REGULAR -> Variety.REGULAR;
            case AMO -> Variety.AMO;
            default -> throw new IllegalArgumentException("Unsupported KiteOrderVariety: " + kiteVariety);
        };
    }

    public KiteOrderVariety toKiteOrderVariety(Variety variety) {
        if (variety == null) return null;
        return switch (variety) {
            case REGULAR -> KiteOrderVariety.REGULAR;
            case AMO -> KiteOrderVariety.AMO;
        };
    }

}
