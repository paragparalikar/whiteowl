package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.Exchange;
import javafx.scene.control.Label;

public final class ExchangeBadge extends Label {

    private static final String BADGE_STYLE = "exchange-badge";
    private static final String NSE_STYLE = "exchange-badge-nse";
    private static final String BSE_STYLE = "exchange-badge-bse";

    private ExchangeBadge() {
    }

    public static Label create(Exchange exchange) {
        Label badge = new Label(exchange.getCode());
        badge.getStyleClass().add(BADGE_STYLE);
        switch (exchange) {
            case NSE -> badge.getStyleClass().add(NSE_STYLE);
            case BSE -> badge.getStyleClass().add(BSE_STYLE);
        }
        return badge;
    }

}
