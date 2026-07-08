package com.whiteowl.workbench.examplegroup;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.examplegroup.model.Example;
import com.whiteowl.core.examplegroup.model.ExampleGroup;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.common.NameInputDialog;
import com.whiteowl.workbench.common.PortfolioQuantityLabel;
import com.whiteowl.workbench.common.ScripBadge;
import com.whiteowl.workbench.common.ScripNavigable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ExampleGroupPane extends VBox implements ScripNavigable {

    private static final String PANE_STYLE = "example-group-pane";
    private static final String TOOLBAR_STYLE = "example-group-toolbar";
    private static final String ADD_BUTTON_STYLE = "example-group-add-button";
    private static final String LIST_STYLE = "example-group-list";
    private static final String CELL_SYMBOL_STYLE = "example-group-cell-symbol";
    private static final String CELL_DETAIL_STYLE = "example-group-cell-detail";
    private static final String SECTION_STYLE = "example-group-section";
    private static final String SECTION_HEADER_STYLE = "example-group-section-header";
    private static final String SECTION_CHEVRON_STYLE = "example-group-section-chevron";
    private static final String SECTION_NAME_STYLE = "example-group-section-name";
    private static final String SECTION_DELETE_STYLE = "example-group-section-delete";
    private static final String SECTION_TRUNCATE_STYLE = "example-group-section-truncate";
    private static final String SECTION_EDIT_FIELD_STYLE = "example-group-section-edit-field";
    private static final String SEARCH_FIELD_STYLE = "example-group-search-field";
    private static final String ICON_STYLE = "example-group-icon";
    private static final String ADD_PROMPT = "Enter example group name";
    private static final String DIALOG_TITLE = "New Example Group";
    private static final String SEARCH_PROMPT = "Search examples...";
    private static final String REMOVE_LABEL = "Remove";
    private static final String DELETE_CONFIRM_TITLE = "Delete Example Group";
    private static final String DELETE_CONFIRM_FORMAT = "Are you sure you want to delete '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String TRUNCATE_CONFIRM_TITLE = "Clear Example Group";
    private static final String TRUNCATE_CONFIRM_FORMAT = "Are you sure you want to remove all examples from '%s'?";
    private static final String TRUNCATE_CONFIRM_BUTTON = "Clear";
    private static final String CONTEXT_DELETE_ICON_STYLE = "context-delete-icon";
    private static final String SEPARATOR = " \u00B7 ";
    private static final String ARROW = " \u2192 ";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yy HH:mm");
    private static final int FIXED_CELL_HEIGHT = 36;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int ICON_SIZE = 14;
    private static final int HEADER_ICON_SIZE = 12;
    private static final int CHEVRON_SIZE = 10;
    private static final int CONTEXT_ICON_SIZE = 16;
    private static final String CELL_DELETE_STYLE = "example-group-cell-delete";
    private static final int CELL_DELETE_ICON_SIZE = 12;

    private final ExampleGroupRepository repository;
    private final ScripRepository scripRepository;
    private final List<ExampleGroup> groups;
    private final VBox sectionsContainer;
    private final Set<String> expandedGroups = new HashSet<>();
    private String searchText = "";
    private Consumer<Example> onExampleSelected;
    private ListView<Integer> lastActiveListView;
    private ExampleGroup lastActiveGroup;

    public ExampleGroupPane(ExampleGroupRepository repository, ScripRepository scripRepository) {
        this.repository = repository;
        this.scripRepository = scripRepository;
        this.groups = new ArrayList<>(repository.loadAll());
        sortGroups();
        this.sectionsContainer = new VBox();
        getStyleClass().add(PANE_STYLE);
        getChildren().addAll(buildToolbar(), sectionsContainer);
        VBox.setVgrow(sectionsContainer, Priority.ALWAYS);
        rebuildSections();
    }

    public void setOnExampleSelected(Consumer<Example> handler) {
        this.onExampleSelected = handler;
    }

    @Override
    public void selectNext() {
        if (lastActiveListView == null || !lastActiveListView.isVisible()) return;
        int size = lastActiveListView.getItems().size();
        if (size == 0) return;
        int current = lastActiveListView.getSelectionModel().getSelectedIndex();
        if (current < size - 1) {
            lastActiveListView.getSelectionModel().clearAndSelect(current + 1);
            lastActiveListView.scrollTo(current + 1);
        }
    }

    @Override
    public void selectPrevious() {
        if (lastActiveListView == null || !lastActiveListView.isVisible()) return;
        int size = lastActiveListView.getItems().size();
        if (size == 0) return;
        int current = lastActiveListView.getSelectionModel().getSelectedIndex();
        if (current > 0) {
            lastActiveListView.getSelectionModel().clearAndSelect(current - 1);
            lastActiveListView.scrollTo(current - 1);
        }
    }

    public List<ExampleGroup> getGroups() {
        return groups;
    }

    public void addExampleToGroup(ExampleGroup group, Example example) {
        group.addExample(example);
        persist();
        rebuildSections();
    }

    public void persist() {
        repository.saveAll(groups);
    }

    public void refresh() {
        rebuildSections();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.getStyleClass().add(TOOLBAR_STYLE);
        bar.setPadding(new Insets(4));
        TextField searchField = new TextField();
        searchField.setPromptText(SEARCH_PROMPT);
        searchField.getStyleClass().add(SEARCH_FIELD_STYLE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText = newVal == null ? "" : newVal.toLowerCase();
            rebuildSections();
        });
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button addButton = new Button();
        FontIcon icon = new FontIcon(FluentUiRegularAL.ADD_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(ICON_STYLE);
        addButton.setGraphic(icon);
        addButton.getStyleClass().add(ADD_BUTTON_STYLE);
        addButton.setFocusTraversable(false);
        addButton.setOnAction(e -> showCreateDialog());
        bar.getChildren().addAll(searchField, addButton);
        return bar;
    }

    private void showCreateDialog() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        NameInputDialog dialog = new NameInputDialog(DIALOG_TITLE, ADD_PROMPT);
        dialog.show(owner);
        String name = dialog.getEnteredName();
        if (name != null) {
            groups.add(new ExampleGroup(name));
            sortGroups();
            persist();
            rebuildSections();
        }
    }

    private void truncateGroup(ExampleGroup group) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                TRUNCATE_CONFIRM_TITLE,
                String.format(TRUNCATE_CONFIRM_FORMAT, group.getName()),
                TRUNCATE_CONFIRM_BUTTON);
        dialog.show(owner);
        if (!dialog.isConfirmed()) return;
        group.getExamples().clear();
        persist();
        rebuildSections();
    }

    private void deleteGroup(ExampleGroup group) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, group.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(owner);
        if (!dialog.isConfirmed()) return;
        groups.remove(group);
        persist();
        rebuildSections();
    }

    private void removeExample(ExampleGroup group, int index) {
        group.removeExample(index);
        persist();
        rebuildSections();
    }

    private void sortGroups() {
        groups.sort(Comparator.comparing(ExampleGroup::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private void rebuildSections() {
        lastActiveListView = null;
        lastActiveGroup = null;
        sectionsContainer.getChildren().clear();
        for (ExampleGroup g : groups) {
            sectionsContainer.getChildren().add(buildGroupSection(g));
        }
    }

    private VBox buildGroupSection(ExampleGroup group) {
        VBox section = new VBox();
        section.getStyleClass().add(SECTION_STYLE);
        boolean expanded = expandedGroups.contains(group.getName());
        ListView<Integer> listView = buildExampleListView(group);
        listView.setVisible(expanded);
        listView.setManaged(expanded);
        HBox header = buildSectionHeader(group, listView);
        section.getChildren().addAll(header, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private void toggleSection(ExampleGroup group, ListView<Integer> listView, FontIcon chevron) {
        boolean expanded = !listView.isVisible();
        listView.setVisible(expanded);
        listView.setManaged(expanded);
        chevron.setIconCode(expanded ? FluentUiRegularAL.CHEVRON_DOWN_12 : FluentUiRegularAL.CHEVRON_RIGHT_12);
        if (expanded) {
            expandedGroups.add(group.getName());
        } else {
            expandedGroups.remove(group.getName());
        }
    }

    private HBox buildSectionHeader(ExampleGroup group, ListView<Integer> listView) {
        HBox header = new HBox(4);
        header.getStyleClass().add(SECTION_HEADER_STYLE);
        header.setPadding(new Insets(2, 4, 2, 4));
        boolean expanded = expandedGroups.contains(group.getName());
        FontIcon chevron = new FontIcon(expanded ? FluentUiRegularAL.CHEVRON_DOWN_12 : FluentUiRegularAL.CHEVRON_RIGHT_12);
        chevron.setIconSize(CHEVRON_SIZE);
        chevron.getStyleClass().add(SECTION_CHEVRON_STYLE);
        Button chevronBtn = new Button();
        chevronBtn.setGraphic(chevron);
        chevronBtn.getStyleClass().add(SECTION_DELETE_STYLE);
        chevronBtn.setFocusTraversable(false);
        chevronBtn.setOnAction(e -> toggleSection(group, listView, chevron));
        StackPane nameContainer = new StackPane();
        Label nameLabel = new Label(group.getName());
        nameLabel.getStyleClass().add(SECTION_NAME_STYLE);
        nameContainer.getChildren().add(nameLabel);
        HBox.setHgrow(nameContainer, Priority.ALWAYS);
        nameContainer.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                startNameEdit(group, nameContainer, nameLabel);
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                toggleSection(group, listView, chevron);
            }
        });
        Button truncateButton = new Button();
        FontIcon truncateIcon = new FontIcon(FluentUiRegularAL.ERASER_20);
        truncateIcon.setIconSize(HEADER_ICON_SIZE);
        truncateIcon.getStyleClass().add(ICON_STYLE);
        truncateButton.setGraphic(truncateIcon);
        truncateButton.getStyleClass().add(SECTION_TRUNCATE_STYLE);
        truncateButton.setFocusTraversable(false);
        truncateButton.setOnAction(e -> truncateGroup(group));
        Button deleteButton = new Button();
        FontIcon deleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
        deleteIcon.setIconSize(HEADER_ICON_SIZE);
        deleteIcon.getStyleClass().add(ICON_STYLE);
        deleteButton.setGraphic(deleteIcon);
        deleteButton.getStyleClass().add(SECTION_DELETE_STYLE);
        deleteButton.setFocusTraversable(false);
        deleteButton.setOnAction(e -> deleteGroup(group));
        header.getChildren().addAll(chevronBtn, nameContainer, truncateButton, deleteButton);
        return header;
    }

    private void startNameEdit(ExampleGroup group, StackPane container, Label nameLabel) {
        TextField editField = new TextField(group.getName());
        editField.getStyleClass().add(SECTION_EDIT_FIELD_STYLE);
        editField.selectAll();
        container.getChildren().setAll(editField);
        editField.requestFocus();
        Runnable commitEdit = () -> {
            String newName = editField.getText();
            if (newName != null && !newName.isBlank()
                    && newName.trim().length() >= MIN_NAME_LENGTH
                    && newName.trim().length() <= MAX_NAME_LENGTH) {
                group.setName(newName.trim());
                sortGroups();
                persist();
                rebuildSections();
                return;
            }
            nameLabel.setText(group.getName());
            container.getChildren().setAll(nameLabel);
        };
        editField.setOnAction(e -> commitEdit.run());
        editField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                container.getChildren().setAll(nameLabel);
            }
        });
        editField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) commitEdit.run();
        });
    }

    private ListView<Integer> buildExampleListView(ExampleGroup group) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < group.getExamples().size(); i++) {
            if (matchesSearch(group.getExamples().get(i))) {
                indices.add(i);
            }
        }
        ListView<Integer> listView = new ListView<>();
        listView.getItems().addAll(indices);
        listView.getStyleClass().add(LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.setPrefHeight(indices.size() * FIXED_CELL_HEIGHT + 2);
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setCellFactory(lv -> new ExampleCell(group, listView));
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onExampleSelected != null) {
                lastActiveListView = listView;
                lastActiveGroup = group;
                if (newVal < group.getExamples().size()) {
                    onExampleSelected.accept(group.getExamples().get(newVal));
                }
            }
        });
        return listView;
    }

    private boolean matchesSearch(Example example) {
        if (searchText == null || searchText.isEmpty()) return true;
        Scrip scrip = scripRepository.findById(example.getScripId()).orElse(null);
        if (scrip != null && scripRepository.matches(scrip, searchText)) return true;
        if (example.getScripId().toLowerCase().contains(searchText)) return true;
        return example.getTimeframe().getCode().toLowerCase().contains(searchText);
    }

    private static String formatTimestamp(long timestamp) {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return ldt.format(DATE_FORMAT);
    }

    private final class ExampleCell extends ListCell<Integer> {

        private static final int BADGE_GAP = 6;

        private final Label symbolLabel = new Label();
        private final Label detailLabel = new Label();
        private final PortfolioQuantityLabel portfolioQtyLabel = new PortfolioQuantityLabel();
        private final HBox container = new HBox(BADGE_GAP);
        private final VBox textContainer = new VBox(1);
        private final HBox topRow = new HBox(BADGE_GAP);
        private final ExampleGroup group;

        private final ListView<Integer> ownerListView;

        ExampleCell(ExampleGroup group, ListView<Integer> ownerListView) {
            this.group = group;
            this.ownerListView = ownerListView;
            symbolLabel.getStyleClass().add(CELL_SYMBOL_STYLE);
            detailLabel.getStyleClass().add(CELL_DETAIL_STYLE);
            topRow.setAlignment(Pos.CENTER_LEFT);
            container.setAlignment(Pos.CENTER_LEFT);
            setOnMousePressed(e -> ownerListView.requestFocus());
        }

        @Override
        protected void updateItem(Integer exampleIndex, boolean empty) {
            super.updateItem(exampleIndex, empty);
            if (empty || exampleIndex == null || exampleIndex >= group.getExamples().size()) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            Example example = group.getExamples().get(exampleIndex);
            Scrip scrip = scripRepository.findById(example.getScripId()).orElse(null);
            String displayName = scrip != null ? scrip.getSymbol() : example.getScripId();
            symbolLabel.setText(displayName);
            String detail = example.getTimeframe().getCode()
                    + SEPARATOR + formatTimestamp(example.getStartTimestamp())
                    + ARROW + formatTimestamp(example.getEndTimestamp());
            detailLabel.setText(detail);
            topRow.getChildren().setAll(
                    ScripBadge.create(scrip != null ? scrip.getScripType() : null),
                    symbolLabel);
            textContainer.getChildren().setAll(topRow, detailLabel);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button cellDeleteBtn = new Button();
            FontIcon cellDeleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            cellDeleteIcon.setIconSize(CELL_DELETE_ICON_SIZE);
            cellDeleteIcon.getStyleClass().add(ICON_STYLE);
            cellDeleteBtn.setGraphic(cellDeleteIcon);
            cellDeleteBtn.getStyleClass().add(CELL_DELETE_STYLE);
            cellDeleteBtn.setFocusTraversable(false);
            final int idx = exampleIndex;
            cellDeleteBtn.setOnAction(ev -> removeExample(group, idx));
            portfolioQtyLabel.updateQuantity(example.getScripId());
            container.getChildren().setAll(textContainer, spacer, portfolioQtyLabel, cellDeleteBtn);
            setGraphic(container);
            ContextMenu ctx = new ContextMenu();
            FontIcon removeIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            removeIcon.setIconSize(CONTEXT_ICON_SIZE);
            removeIcon.getStyleClass().add(CONTEXT_DELETE_ICON_STYLE);
            MenuItem removeItem = new MenuItem(REMOVE_LABEL, removeIcon);
            removeItem.setOnAction(e -> removeExample(group, idx));
            ctx.getItems().add(removeItem);
            setContextMenu(ctx);
        }

    }

}
