package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.scripting.screener.Screen;
import com.whiteowl.scripting.screener.ScreenRegistry;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.common.ScripBadge;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class AlertPane extends VBox {

    private static final String PANE_STYLE = "alert-pane";
    private static final String TOOLBAR_STYLE = "alert-toolbar";
    private static final String COMBO_STYLE = "alert-combo";
    private static final String BUTTON_STYLE = "alert-button";
    private static final String ICON_STYLE = "alert-icon";
    private static final String LIST_STYLE = "alert-list";
    private static final String CELL_SYMBOL_STYLE = "alert-cell-symbol";
    private static final String CELL_TIME_STYLE = "alert-cell-time";
    private static final String HEADER_STYLE = "alert-header";
    private static final int SECTION_SPACING = 8;
    private static final int ICON_SIZE = 14;
    private static final int BUTTON_SPACING = 4;
    private static final int FIXED_CELL_HEIGHT = 28;
    private static final int BADGE_GAP = 6;
    private static final String HEADER_FORMAT = "Matches (%d)";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AlertEngine alertEngine;
    private final GroupRepository groupRepository;
    private final ScreenRegistry screenRegistry;
    private final ComboBox<AlertDefinition> alertDefinitionCombo;
    private final Button createButton;
    private final Button editButton;
    private final Button deleteButton;
    private final ListView<AlertMatch> matchListView;
    private final ObservableList<AlertMatch> matchList;
    private final Label headerLabel;
    private Consumer<Scrip> onScripSelected;
    private Consumer<AlertMatch> onAlertChartRequested;

    public AlertPane(AlertEngine alertEngine, GroupRepository groupRepository, ScreenRegistry screenRegistry) {
        this.alertEngine = alertEngine;
        this.groupRepository = groupRepository;
        this.screenRegistry = screenRegistry;
        this.alertDefinitionCombo = buildAlertDefinitionCombo();
        this.createButton = buildCreateButton();
        this.editButton = buildEditButton();
        this.deleteButton = buildDeleteButton();
        this.matchList = FXCollections.observableArrayList();
        this.matchListView = buildMatchListView();
        this.headerLabel = buildHeaderLabel();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        setupAlertDefinitionListener();
    }

    private void buildLayout() {
        HBox toolbar = buildToolbar();
        getChildren().addAll(toolbar, new Separator(), headerLabel, matchListView);
        VBox.setVgrow(matchListView, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(BUTTON_SPACING);
        toolbar.getStyleClass().add(TOOLBAR_STYLE);
        toolbar.setPadding(new Insets(4));
        HBox.setHgrow(alertDefinitionCombo, Priority.ALWAYS);
        toolbar.getChildren().addAll(alertDefinitionCombo, createButton, editButton, deleteButton);
        return toolbar;
    }

    private ComboBox<AlertDefinition> buildAlertDefinitionCombo() {
        ComboBox<AlertDefinition> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setCellFactory(lv -> new AlertDefinitionCell());
        combo.setButtonCell(new AlertDefinitionCell());
        combo.getItems().setAll(alertEngine.getAlertDefinitions());
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        combo.setOnAction(e -> onAlertDefinitionChanged());
        return combo;
    }

    private Button buildIconButton(org.kordamp.ikonli.Ikon iconCode, Runnable onAction) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(ICON_STYLE);
        Button button = new Button(null, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(e -> onAction.run());
        return button;
    }

    private Button buildCreateButton() {
        return buildIconButton(FluentUiRegularAL.ADD_16, this::showCreateDialog);
    }

    private Button buildEditButton() {
        return buildIconButton(FluentUiRegularAL.EDIT_16, this::showEditDialog);
    }

    private Button buildDeleteButton() {
        return buildIconButton(FluentUiRegularAL.DELETE_16, this::showDeleteDialog);
    }

    private ListView<AlertMatch> buildMatchListView() {
        ListView<AlertMatch> listView = new ListView<>(matchList);
        listView.getStyleClass().add(LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.setCellFactory(lv -> new AlertMatchCell());
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onScripSelected != null) {
                onScripSelected.accept(newVal.getScrip());
                if (onAlertChartRequested != null) {
                    onAlertChartRequested.accept(newVal);
                }
            }
        });
        return listView;
    }

    private Label buildHeaderLabel() {
        Label label = new Label(String.format(HEADER_FORMAT, 0));
        label.getStyleClass().add(HEADER_STYLE);
        return label;
    }

    private void setupAlertDefinitionListener() {
        alertEngine.addAlertMatchListener(alertMatch -> {
            AlertDefinition alertDefinition = alertMatch.getAlertDefinition();
            Platform.runLater(() -> {
                if (alertDefinitionCombo.getValue() != null &&
                    alertDefinitionCombo.getValue().getId().equals(alertDefinition.getId())) {
                    matchList.add(alertMatch);
                    updateHeader();
                }
            });
        });
    }

    private void onAlertDefinitionChanged() {
        AlertDefinition selected = alertDefinitionCombo.getValue();
        if (selected == null) {
            matchList.clear();
        } else {
            matchList.setAll(alertEngine.getAlertMatches(selected.getId()));
        }
        updateHeader();
    }

    private void updateHeader() {
        headerLabel.setText(String.format(HEADER_FORMAT, matchList.size()));
    }

    private void showCreateDialog() {
        AlertDefinitionDialog dialog = new AlertDefinitionDialog(groupRepository, screenRegistry);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            AlertDefinition alertDefinition = dialog.getAlertDefinition();
            alertEngine.addAlertDefinition(alertDefinition);
            alertDefinitionCombo.getItems().add(alertDefinition);
            alertDefinitionCombo.setValue(alertDefinition);
        }
    }

    private void showEditDialog() {
        AlertDefinition selected = alertDefinitionCombo.getValue();
        if (selected == null) return;
        AlertDefinitionDialog dialog = new AlertDefinitionDialog(groupRepository, screenRegistry, selected);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            AlertDefinition updated = dialog.getAlertDefinition();
            alertEngine.updateAlertDefinition(updated);
            int index = alertDefinitionCombo.getItems().indexOf(selected);
            if (index >= 0) {
                alertDefinitionCombo.getItems().set(index, updated);
            }
            alertDefinitionCombo.setValue(updated);
        }
    }

    private void showDeleteDialog() {
        AlertDefinition selected = alertDefinitionCombo.getValue();
        if (selected == null) return;
        com.whiteowl.workbench.common.ConfirmationDialog dialog = new com.whiteowl.workbench.common.ConfirmationDialog(
                "Delete Alert",
                "Are you sure you want to delete '" + selected.getName() + "'?",
                "Delete");
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            alertEngine.deleteAlertDefinition(selected.getId());
            alertDefinitionCombo.getItems().remove(selected);
            if (!alertDefinitionCombo.getItems().isEmpty()) {
                alertDefinitionCombo.setValue(alertDefinitionCombo.getItems().get(0));
            } else {
                alertDefinitionCombo.setValue(null);
            }
        }
    }

    public void setOnScripSelected(Consumer<Scrip> handler) {
        this.onScripSelected = handler;
    }

    public void setOnAlertChartRequested(Consumer<AlertMatch> handler) {
        this.onAlertChartRequested = handler;
    }

    public void refreshAlertDefinitions() {
        alertEngine.refreshAlertDefinitions();
        AlertDefinition selected = alertDefinitionCombo.getValue();
        alertDefinitionCombo.getItems().setAll(alertEngine.getAlertDefinitions());
        if (selected != null) {
            alertDefinitionCombo.getItems().stream()
                    .filter(a -> a.getId().equals(selected.getId()))
                    .findFirst()
                    .ifPresent(alertDefinitionCombo::setValue);
        } else if (!alertDefinitionCombo.getItems().isEmpty()) {
            alertDefinitionCombo.setValue(alertDefinitionCombo.getItems().get(0));
        }
    }

    private static final class AlertDefinitionCell extends ListCell<AlertDefinition> {

        @Override
        protected void updateItem(AlertDefinition alertDefinition, boolean empty) {
            super.updateItem(alertDefinition, empty);
            if (empty || alertDefinition == null) {
                setText(null);
                return;
            }
            String status = alertDefinition.isEnabled() ? "●" : "○";
            setText(status + " " + alertDefinition.getName());
        }
    }

    private final class AlertMatchCell extends ListCell<AlertMatch> {

        private final Label symbolLabel = new Label();
        private final Label timeLabel = new Label();
        private final HBox container = new HBox(BADGE_GAP);

        AlertMatchCell() {
            symbolLabel.getStyleClass().add(CELL_SYMBOL_STYLE);
            timeLabel.getStyleClass().add(CELL_TIME_STYLE);
            container.setAlignment(Pos.CENTER_LEFT);
            setupContextMenu();
        }

        @Override
        protected void updateItem(AlertMatch alertMatch, boolean empty) {
            super.updateItem(alertMatch, empty);
            if (empty || alertMatch == null) {
                setGraphic(null);
                return;
            }
            symbolLabel.setText(alertMatch.getScrip().getSymbol());
            timeLabel.setText(LocalDateTime.ofInstant(alertMatch.getTimestamp(), ZoneId.systemDefault()).format(TIME_FORMATTER));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            container.getChildren().setAll(ScripBadge.create(alertMatch.getScrip().getScripType()),
                    symbolLabel, spacer, timeLabel);
            setGraphic(container);
        }

        private void setupContextMenu() {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem moveUpItem = new MenuItem("Move up");
            moveUpItem.setOnAction(e -> moveMatchUp());
            MenuItem moveDownItem = new MenuItem("Move down");
            moveDownItem.setOnAction(e -> moveMatchDown());
            MenuItem sendToTopItem = new MenuItem("Send to top");
            sendToTopItem.setOnAction(e -> sendMatchToTop());
            MenuItem sendToBottomItem = new MenuItem("Send to bottom");
            sendToBottomItem.setOnAction(e -> sendMatchToBottom());
            contextMenu.getItems().addAll(moveUpItem, moveDownItem, sendToTopItem, sendToBottomItem);
            setContextMenu(contextMenu);
        }

        private void moveMatchUp() {
            int index = getIndex();
            if (index > 0) {
                AlertDefinition alertDefinition = alertDefinitionCombo.getValue();
                if (alertDefinition != null) {
                    alertEngine.moveAlertMatch(alertDefinition.getId(), index, index - 1);
                    onAlertDefinitionChanged();
                }
            }
        }

        private void moveMatchDown() {
            int index = getIndex();
            if (index < matchList.size() - 1) {
                AlertDefinition alertDefinition = alertDefinitionCombo.getValue();
                if (alertDefinition != null) {
                    alertEngine.moveAlertMatch(alertDefinition.getId(), index, index + 1);
                    onAlertDefinitionChanged();
                }
            }
        }

        private void sendMatchToTop() {
            int index = getIndex();
            if (index > 0) {
                AlertDefinition alertDefinition = alertDefinitionCombo.getValue();
                if (alertDefinition != null) {
                    alertEngine.moveAlertMatchToTop(alertDefinition.getId(), index);
                    onAlertDefinitionChanged();
                }
            }
        }

        private void sendMatchToBottom() {
            int index = getIndex();
            if (index < matchList.size() - 1) {
                AlertDefinition alertDefinition = alertDefinitionCombo.getValue();
                if (alertDefinition != null) {
                    alertEngine.moveAlertMatchToBottom(alertDefinition.getId(), index);
                    onAlertDefinitionChanged();
                }
            }
        }
    }
}
