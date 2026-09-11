package com.whiteowl.workbench.common;

import com.whiteowl.core.portfolio.service.PortfolioQuantityProvider;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

public final class PortfolioQuantityLabel extends HBox {

    private static final int GAP = 2;
    private static final int ICON_SIZE = 12;
    private static final String CONTAINER_STYLE = "portfolio-qty";
    private static final String ICON_STYLE = "portfolio-qty-icon";
    private static final String LONG_STYLE = "portfolio-qty-long";
    private static final String SHORT_STYLE = "portfolio-qty-short";

    private final FontIcon icon = new FontIcon(FluentUiRegularAL.BRIEFCASE_12);
    private final Label label = new Label();
    private String currentScripId;

    public PortfolioQuantityLabel() {
        super(GAP);
        setAlignment(Pos.CENTER_RIGHT);
        getStyleClass().add(CONTAINER_STYLE);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(ICON_STYLE);
        setVisible(false);
        setManaged(false);
        PortfolioQuantityProvider.getInstance().addListener(() ->
                Platform.runLater(() -> {
                    if (currentScripId != null) updateQuantity(currentScripId);
                }));
    }

    public void updateQuantity(String scripId) {
        this.currentScripId = scripId;
        int qty = PortfolioQuantityProvider.getInstance().getQuantity(scripId);
        if (qty == 0) {
            setVisible(false);
            setManaged(false);
            return;
        }
        label.setText(String.valueOf(Math.abs(qty)));
        label.getStyleClass().removeAll(LONG_STYLE, SHORT_STYLE);
        icon.getStyleClass().removeAll(LONG_STYLE, SHORT_STYLE);
        String style = qty > 0 ? LONG_STYLE : SHORT_STYLE;
        label.getStyleClass().add(style);
        icon.getStyleClass().add(style);
        getChildren().setAll(icon, label);
        setVisible(true);
        setManaged(true);
    }

}
