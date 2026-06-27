package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteOrderValidity;
import com.whiteowl.core.order.model.Validity;

final class KiteValidityMapper {

    public Validity toValidity(KiteOrderValidity kiteValidity) {
        if (kiteValidity == null) return null;
        return switch (kiteValidity) {
            case DAY -> Validity.DAY;
            case IOC -> Validity.IOC;
            case TTL -> Validity.GTT;
        };
    }

    public KiteOrderValidity toKiteOrderValidity(Validity validity) {
        if (validity == null) return null;
        return switch (validity) {
            case DAY -> KiteOrderValidity.DAY;
            case IOC -> KiteOrderValidity.IOC;
            case GTT -> KiteOrderValidity.TTL;
        };
    }

}
