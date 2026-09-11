package com.whiteowl.workbench.common;

import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;

public final class ScripSearchField extends VBox {

    private static final String FIELD_STYLE = "scrip-search-field";
    private static final String LIST_STYLE = "scrip-search-list";
    private static final String CELL_SYMBOL_STYLE = "scrip-search-cell-symbol";
    private static final String PROMPT_TEXT = "Search scrip...";
    private static final int MAX_SUGGESTIONS = 15;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int LIST_ROW_HEIGHT = 26;
    private static final int LIST_PADDING = 4;
    private static final int BADGE_GAP = 6;

    private final ScripRepository scripRepository;
    private final TextField textField;
    private final ListView<Scrip> suggestionList;
    private final Popup popup;
    private final ObjectProperty<Scrip> selectedScrip = new SimpleObjectProperty<>();
    private boolean suppressSearch;

    public ScripSearchField(ScripRepository scripRepository) {
        this(scripRepository, (String) null);
    }

    public ScripSearchField(ScripRepository scripRepository, String initialScripId) {
        this.scripRepository = scripRepository;
        this.textField = buildTextField();
        this.suggestionList = buildSuggestionList();
        this.popup = buildPopup();
        getChildren().add(textField);
        attachListeners();
        initializeSelection(initialScripId);
    }

    public ScripSearchField(ScripRepository scripRepository, Scrip initialScrip) {
        this.scripRepository = scripRepository;
        this.textField = buildTextField();
        this.suggestionList = buildSuggestionList();
        this.popup = buildPopup();
        getChildren().add(textField);
        attachListeners();
        if (initialScrip != null) {
            applySelection(initialScrip);
        }
    }

    public ObjectProperty<Scrip> selectedScripProperty() {
        return selectedScrip;
    }

    public Scrip getSelectedScrip() {
        return selectedScrip.get();
    }

    public String getSelectedScripId() {
        Scrip scrip = selectedScrip.get();
        return scrip != null ? scrip.getId() : textField.getText();
    }

    public TextField getTextField() {
        return textField;
    }

    private void initializeSelection(String scripId) {
        if (scripId == null || scripId.isEmpty()) return;
        scripRepository.findById(scripId).ifPresentOrElse(
                this::applySelection,
                () -> textField.setText(scripId)
        );
    }

    private void applySelection(Scrip scrip) {
        suppressSearch = true;
        selectedScrip.set(scrip);
        textField.setText(scrip.getSymbol());
        textField.positionCaret(textField.getText().length());
        suppressSearch = false;
    }

    private TextField buildTextField() {
        TextField field = new TextField();
        field.getStyleClass().add(FIELD_STYLE);
        field.setPromptText(PROMPT_TEXT);
        return field;
    }

    private ListView<Scrip> buildSuggestionList() {
        ListView<Scrip> list = new ListView<>();
        list.getStyleClass().add(LIST_STYLE);
        list.setCellFactory(lv -> new ScripSuggestionCell());
        list.setOnMouseClicked(e -> confirmSelection());
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmSelection();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hidePopup();
                textField.requestFocus();
            }
        });
        return list;
    }

    private Popup buildPopup() {
        Popup p = new Popup();
        p.setAutoHide(true);
        p.getContent().add(suggestionList);
        return p;
    }

    private void attachListeners() {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressSearch) onTextChanged(newVal);
        });
        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) hidePopup();
        });
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN && popup.isShowing()) {
                suggestionList.requestFocus();
                if (!suggestionList.getItems().isEmpty()) {
                    suggestionList.getSelectionModel().selectFirst();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hidePopup();
            }
        });
    }

    private void onTextChanged(String query) {
        if (query == null || query.length() < MIN_QUERY_LENGTH) {
            hidePopup();
            return;
        }
        List<Scrip> matches = scripRepository.search(query, MAX_SUGGESTIONS);
        if (matches.isEmpty()) {
            hidePopup();
            return;
        }
        suggestionList.getItems().setAll(matches);
        int visibleRows = Math.min(matches.size(), MAX_SUGGESTIONS);
        suggestionList.setPrefHeight(visibleRows * LIST_ROW_HEIGHT + LIST_PADDING);
        showPopup();
    }

    private void confirmSelection() {
        Scrip selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        applySelection(selected);
        hidePopup();
    }

    private void showPopup() {
        if (textField.getScene() == null || textField.getScene().getWindow() == null) return;
        Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
        if (bounds == null) return;
        suggestionList.setPrefWidth(Math.max(textField.getWidth(), 250));
        popup.show(textField, bounds.getMinX(), bounds.getMaxY());
    }

    private void hidePopup() {
        popup.hide();
    }

    private static final class ScripSuggestionCell extends ListCell<Scrip> {

        private final Label symbolLabel = new Label();
        private final HBox container = new HBox(BADGE_GAP);

        ScripSuggestionCell() {
            symbolLabel.getStyleClass().add(CELL_SYMBOL_STYLE);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Scrip scrip, boolean empty) {
            super.updateItem(scrip, empty);
            if (empty || scrip == null) {
                setGraphic(null);
                return;
            }
            symbolLabel.setText(scrip.getSymbol());
            container.getChildren().setAll(ScripBadge.create(scrip.getScripType()), symbolLabel);
            setGraphic(container);
        }

    }

}
