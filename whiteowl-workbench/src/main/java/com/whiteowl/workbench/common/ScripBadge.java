package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.ScripType;
import javafx.scene.control.Label;

import java.util.Map;

public final class ScripBadge extends Label {

    private static final String BADGE_STYLE = "scrip-badge";
    private static final String EQUITY_STYLE = "scrip-badge-equity";
    private static final String INDEX_STYLE = "scrip-badge-index";
    private static final String FUTURES_STYLE = "scrip-badge-futures";
    private static final String OPTIONS_STYLE = "scrip-badge-options";
    private static final String ETF_STYLE = "scrip-badge-etf";
    private static final String CURRENCY_STYLE = "scrip-badge-currency";
    private static final String DEBT_STYLE = "scrip-badge-debt";
    private static final String DEFAULT_STYLE = "scrip-badge-default";
    private static final Map<ScripType, String> BADGE_LABELS = Map.of(
            ScripType.EQUITY, "E",
            ScripType.INDEX, "I",
            ScripType.FUTURES, "F",
            ScripType.OPTIONS, "O",
            ScripType.ETF, "T",
            ScripType.CURRENCY, "C",
            ScripType.DEBT, "D",
            ScripType.OTHER, "?"
    );
    private static final Map<ScripType, String> TYPE_STYLES = Map.of(
            ScripType.EQUITY, EQUITY_STYLE,
            ScripType.INDEX, INDEX_STYLE,
            ScripType.FUTURES, FUTURES_STYLE,
            ScripType.OPTIONS, OPTIONS_STYLE,
            ScripType.ETF, ETF_STYLE,
            ScripType.CURRENCY, CURRENCY_STYLE,
            ScripType.DEBT, DEBT_STYLE,
            ScripType.OTHER, DEFAULT_STYLE
    );
    private static final String FALLBACK_LABEL = "?";

    private ScripBadge() {
    }

    public static Label create(ScripType scripType) {
        Label badge = new Label(scripType != null ? BADGE_LABELS.getOrDefault(scripType, FALLBACK_LABEL) : FALLBACK_LABEL);
        badge.getStyleClass().add(BADGE_STYLE);
        badge.getStyleClass().add(scripType != null ? TYPE_STYLES.getOrDefault(scripType, DEFAULT_STYLE) : DEFAULT_STYLE);
        return badge;
    }

}
