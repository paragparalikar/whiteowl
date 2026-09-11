package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.Exchange;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.Map;

public final class ExchangeFilterCombo extends ComboBox<Exchange> {

    private static final String ALL_LABEL = "All Exchanges";
    private static final int LOGO_HEIGHT = 16;
    private static final int LOGO_GAP = 6;
    private static final String NSE_LOGO_PATH = "/images/nse-logo.jpg";
    private static final String BSE_LOGO_PATH = "/images/bse-logo.png";
    private static final Map<Exchange, String> LOGO_PATHS = Map.of(
            Exchange.NSE, NSE_LOGO_PATH,
            Exchange.BSE, BSE_LOGO_PATH
    );

    public ExchangeFilterCombo(boolean includeAll, Exchange defaultValue) {
        if (includeAll) {
            getItems().add(null);
        }
        getItems().addAll(Exchange.values());
        setValue(defaultValue);
        setCellFactory(lv -> new ExchangeLogoCell());
        setButtonCell(new ExchangeLogoCell());
    }

    public ExchangeFilterCombo(boolean includeAll) {
        this(includeAll, Exchange.NSE);
    }

    private static ImageView createLogo(Exchange exchange) {
        String path = LOGO_PATHS.get(exchange);
        if (path == null) return null;
        Image image = new Image(ExchangeFilterCombo.class.getResourceAsStream(path));
        ImageView view = new ImageView(image);
        view.setFitHeight(LOGO_HEIGHT);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static final class ExchangeLogoCell extends ListCell<Exchange> {

        @Override
        protected void updateItem(Exchange exchange, boolean empty) {
            super.updateItem(exchange, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }
            if (exchange == null) {
                setGraphic(null);
                setText(ALL_LABEL);
                return;
            }
            ImageView logo = createLogo(exchange);
            Label desc = new Label(exchange.getDescription());
            HBox container = new HBox(LOGO_GAP);
            container.setAlignment(Pos.CENTER_LEFT);
            if (logo != null) {
                container.getChildren().add(logo);
            }
            container.getChildren().add(desc);
            setGraphic(container);
            setText(null);
        }

    }

}
