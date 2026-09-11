package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.ScripType;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public final class ScripTypeFilterCombo extends ComboBox<ScripType> {

    private static final String ALL_LABEL = "All Types";
    private static final int BADGE_GAP = 6;

    public ScripTypeFilterCombo(boolean includeAll, ScripType defaultValue) {
        if (includeAll) {
            getItems().add(null);
        }
        getItems().addAll(ScripType.values());
        setValue(defaultValue);
        setCellFactory(lv -> new ScripTypeCell());
        setButtonCell(new ScripTypeCell());
    }

    public ScripTypeFilterCombo(boolean includeAll) {
        this(includeAll, ScripType.EQUITY);
    }

    private static final class ScripTypeCell extends ListCell<ScripType> {

        @Override
        protected void updateItem(ScripType type, boolean empty) {
            super.updateItem(type, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }
            if (type == null) {
                setGraphic(null);
                setText(ALL_LABEL);
                return;
            }
            HBox container = new HBox(BADGE_GAP);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(ScripBadge.create(type), new Label(type.getDisplayLabel()));
            setGraphic(container);
            setText(null);
        }

    }

}
