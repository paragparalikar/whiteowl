package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteExchange;
import com.whiteowl.core.scrip.model.Exchange;

final class KiteExchangeMapper {

    public Exchange toExchange(KiteExchange kiteExchange) {
        if (kiteExchange == null) return null;
        return switch (kiteExchange) {
            case NSE -> Exchange.NSE;
            case BSE -> Exchange.BSE;
            default -> throw new IllegalArgumentException("Unsupported KiteExchange: " + kiteExchange);
        };
    }

    public KiteExchange toKiteExchange(Exchange exchange) {
        if (exchange == null) return null;
        return switch (exchange) {
            case NSE -> KiteExchange.NSE;
            case BSE -> KiteExchange.BSE;
        };
    }

}
