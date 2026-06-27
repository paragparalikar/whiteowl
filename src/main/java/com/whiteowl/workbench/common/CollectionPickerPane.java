package com.whiteowl.workbench.common;

import com.whiteowl.core.collection.CollectionResolver;
import com.whiteowl.core.collection.CollectionSelection;
import com.whiteowl.core.collection.CollectionType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.whiteowl.core.collection.CollectionType.ALL_SCRIPS;

public final class CollectionPickerPane extends VBox {

    private static final String CONTAINER_STYLE = "collection-picker";
    private static final String HEADER_STYLE = "collection-picker-header";
    private static final String COMBO_STYLE = "collection-picker-combo";
    private static final String ITEMS_CONTAINER_STYLE = "collection-picker-items";
    private static final String CHECKBOX_STYLE = "collection-picker-checkbox";
    private static final String HEADER_TEXT = "Collections";
    private static final String ALL_HINT_TEXT = "All items in this collection will be used";
    private static final String HINT_STYLE = "collection-picker-hint";
    private static final int SPACING = 6;
    private static final int ITEMS_SPACING = 4;

    private final CollectionResolver resolver;
    private final ComboBox<CollectionType> typeCombo;
    private final VBox itemsContainer;
    private final Label hintLabel;
    private final List<CheckBox> itemChecks;

    public CollectionPickerPane(CollectionResolver resolver) {
        this.resolver = resolver;
        this.typeCombo = buildTypeCombo();
        this.itemsContainer = buildItemsContainer();
        this.hintLabel = buildHintLabel();
        this.itemChecks = new ArrayList<>();
        getStyleClass().add(CONTAINER_STYLE);
        setSpacing(SPACING);
        buildLayout();
    }

    public CollectionSelection getSelection() {
        CollectionType type = typeCombo.getValue();
        if (type == ALL_SCRIPS) {
            return new CollectionSelection(ALL_SCRIPS);
        }
        Set<String> selected = new LinkedHashSet<>();
        for (CheckBox cb : itemChecks) {
            if (cb.isSelected()) {
                selected.add(cb.getText());
            }
        }
        return new CollectionSelection(type, selected);
    }

    public Set<String> resolveScripIds() {
        return resolver.resolve(getSelection());
    }

    public void setPickerDisabled(boolean disabled) {
        typeCombo.setDisable(disabled);
        itemChecks.forEach(cb -> cb.setDisable(disabled));
    }

    private void buildLayout() {
        Label header = new Label(HEADER_TEXT);
        header.getStyleClass().add(HEADER_STYLE);
        getChildren().addAll(header, typeCombo, hintLabel, itemsContainer);
    }

    private ComboBox<CollectionType> buildTypeCombo() {
        ComboBox<CollectionType> combo = new ComboBox<>();
        combo.getItems().addAll(CollectionType.values());
        combo.setValue(ALL_SCRIPS);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setCellFactory(lv -> new CollectionTypeCell());
        combo.setButtonCell(new CollectionTypeCell());
        combo.valueProperty().addListener((obs, oldVal, newVal) -> onTypeChanged(newVal));
        return combo;
    }

    private VBox buildItemsContainer() {
        VBox container = new VBox(ITEMS_SPACING);
        container.getStyleClass().add(ITEMS_CONTAINER_STYLE);
        container.setVisible(false);
        container.setManaged(false);
        return container;
    }

    private Label buildHintLabel() {
        Label label = new Label();
        label.getStyleClass().add(HINT_STYLE);
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void onTypeChanged(CollectionType type) {
        itemChecks.clear();
        itemsContainer.getChildren().clear();
        if (type == ALL_SCRIPS) {
            showItems(false);
            showHint(false);
            return;
        }
        List<String> names = resolver.getAvailableNames(type);
        if (names.isEmpty()) {
            showItems(false);
            showHint(false);
            return;
        }
        for (String name : names) {
            CheckBox cb = new CheckBox(name);
            cb.getStyleClass().add(CHECKBOX_STYLE);
            itemChecks.add(cb);
        }
        itemsContainer.getChildren().addAll(itemChecks);
        showItems(true);
        hintLabel.setText(ALL_HINT_TEXT);
        showHint(true);
    }

    private void showItems(boolean show) {
        itemsContainer.setVisible(show);
        itemsContainer.setManaged(show);
    }

    private void showHint(boolean show) {
        hintLabel.setVisible(show);
        hintLabel.setManaged(show);
    }

    private static final class CollectionTypeCell extends ListCell<CollectionType> {

        @Override
        protected void updateItem(CollectionType type, boolean empty) {
            super.updateItem(type, empty);
            if (empty || type == null) {
                setText(null);
                return;
            }
            setText(type.getDisplayLabel());
        }

    }

}
