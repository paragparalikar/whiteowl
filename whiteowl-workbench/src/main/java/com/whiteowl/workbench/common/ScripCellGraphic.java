package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class ScripCellGraphic extends HBox {

    private static final int BADGE_GAP = 6;
    private static final String SYMBOL_STYLE = "scrip-cell-symbol";

    private final Label symbolLabel = new Label();
    private final PortfolioQuantityLabel portfolioQtyLabel = new PortfolioQuantityLabel();

    public ScripCellGraphic() {
        super(BADGE_GAP);
        symbolLabel.getStyleClass().add(SYMBOL_STYLE);
        setAlignment(Pos.CENTER_LEFT);
    }

    public PortfolioQuantityLabel getPortfolioQtyLabel() {
        return portfolioQtyLabel;
    }

    public void updateScrip(Scrip scrip) {
        update(scrip.getScripType(), scrip.getSymbol(), scrip.getId());
    }

    public void update(ScripType scripType, String symbol) {
        update(scripType, symbol, null);
    }

    public void update(ScripType scripType, String symbol, String scripId) {
        symbolLabel.setText(symbol);
        getChildren().setAll(ScripBadge.create(scripType), symbolLabel);
        if (scripId != null) {
            portfolioQtyLabel.updateQuantity(scripId);
        }
    }

}
