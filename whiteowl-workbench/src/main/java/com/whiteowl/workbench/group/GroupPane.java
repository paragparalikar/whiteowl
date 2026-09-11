package com.whiteowl.workbench.group;

import com.whiteowl.core.breadth.BreadthComputer;
import com.whiteowl.core.rs.RSComputer;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.AddToCollectionMenuBuilder;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.common.NameInputDialog;
import com.whiteowl.workbench.common.ScripCellGraphic;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.watchlist.WatchlistPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class GroupPane extends VBox implements ScripNavigable {

    private static final String PANE_STYLE = "group-pane";
    private static final String TOOLBAR_STYLE = "group-toolbar";
    private static final String ADD_BUTTON_STYLE = "group-add-button";
    private static final String LIST_STYLE = "group-list";

    private static final String SECTION_STYLE = "group-section";
    private static final String SECTION_HEADER_STYLE = "group-section-header";
    private static final String SECTION_CHEVRON_STYLE = "group-section-chevron";
    private static final String SECTION_NAME_STYLE = "group-section-name";
    private static final String SECTION_DELETE_STYLE = "group-section-delete";
    private static final String SECTION_TRUNCATE_STYLE = "group-section-truncate";
    private static final String SECTION_EDIT_FIELD_STYLE = "group-section-edit-field";
    private static final String SEARCH_FIELD_STYLE = "group-search-field";
    private static final String ICON_STYLE = "group-icon";
    private static final String ADD_PROMPT = "Enter group name";
    private static final String DIALOG_TITLE = "New Group";
    private static final String CSV_DELIMITER = ",";
    private static final int CSV_SYMBOL_INDEX = 2;
    private static final int CSV_MIN_COLUMNS = 3;
    private static final String SEARCH_PROMPT = "Search scrips...";
    private static final String REMOVE_LABEL = "Remove";
    private static final String DELETE_CONFIRM_TITLE = "Delete Group";
    private static final String DELETE_CONFIRM_FORMAT = "Are you sure you want to delete '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String TRUNCATE_CONFIRM_TITLE = "Clear Group";
    private static final String TRUNCATE_CONFIRM_FORMAT = "Are you sure you want to remove all scrips from '%s'?";
    private static final String TRUNCATE_CONFIRM_BUTTON = "Clear";
    private static final String ADD_TO_WATCHLIST_LABEL = "Add to Watchlist";
    private static final int CONTEXT_ICON_SIZE = 16;
    private static final String CONTEXT_DELETE_ICON_STYLE = "context-delete-icon";
    private static final String DRAG_OVER_STYLE = "group-cell-drag-over";
    private static final DataFormat SCRIP_DRAG_FORMAT = new DataFormat("application/x-whiteowl-group-scrip-drag");
    private static final int FIXED_CELL_HEIGHT = 24;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int ICON_SIZE = 14;
    private static final int HEADER_ICON_SIZE = 12;
    private static final int CHEVRON_SIZE = 10;
    private static final String CELL_DELETE_STYLE = "group-cell-delete";
    private static final int CELL_DELETE_ICON_SIZE = 12;

    private final GroupRepository repository;
    private final ScripRepository scripRepository;
    private final List<Group> groups;
    private BreadthComputer breadthComputer;
    private RSComputer rsComputer;
    private final VBox sectionsContainer;
    private final Set<String> expandedGroups = new HashSet<>();
    private String searchText = "";
    private Consumer<Scrip> onScripSelected;
    private WatchlistPane watchlistPane;
    private ListView<String> lastActiveListView;

    public GroupPane(GroupRepository repository, ScripRepository scripRepository) {
        this.repository = repository;
        this.scripRepository = scripRepository;
        this.groups = new ArrayList<>(repository.loadAll());
        this.sectionsContainer = new VBox();
        getStyleClass().add(PANE_STYLE);
        getChildren().addAll(buildToolbar(), sectionsContainer);
        VBox.setVgrow(sectionsContainer, Priority.ALWAYS);
        rebuildSections();
    }

    public void setOnScripSelected(Consumer<Scrip> handler) {
        this.onScripSelected = handler;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setWatchlistPane(WatchlistPane watchlistPane) {
        this.watchlistPane = watchlistPane;
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

    public void addScripToGroup(Group group, String scripId) {
        group.addScrip(scripId);
        persist();
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
        Button importButton = new Button();
        FontIcon importIcon = new FontIcon(FluentUiRegularAL.ARROW_IMPORT_20);
        importIcon.setIconSize(ICON_SIZE);
        importIcon.getStyleClass().add(ICON_STYLE);
        importButton.setGraphic(importIcon);
        importButton.getStyleClass().add(ADD_BUTTON_STYLE);
        importButton.setFocusTraversable(false);
        importButton.setOnAction(e -> showImportDialog());
        bar.getChildren().addAll(searchField, importButton, addButton);
        return bar;
    }

    private void showCreateDialog() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        NameInputDialog dialog = new NameInputDialog(DIALOG_TITLE, ADD_PROMPT);
        dialog.show(owner);
        String name = dialog.getEnteredName();
        if (name != null) {
            groups.add(new Group(name));
            persist();
            rebuildSections();
        }
    }

    private void showImportDialog() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        GroupImportDialog dialog = new GroupImportDialog();
        dialog.show(owner);
        String name = dialog.getEnteredName();
        File file = dialog.getSelectedFile();
        if (name != null && file != null) {
            importGroupFromCsv(name, file);
        }
    }

    private void importGroupFromCsv(String groupName, File csvFile) {
        List<String> scripIds = parseCsvSymbols(csvFile);
        if (scripIds.isEmpty()) return;
        Group existing = groups.stream()
                .filter(g -> g.getName().equalsIgnoreCase(groupName))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.getScripIds().clear();
            scripIds.forEach(existing::addScrip);
        } else {
            groups.add(new Group(groupName, scripIds));
        }
        persist();
        rebuildSections();
    }

    private List<String> parseCsvSymbols(File csvFile) {
        List<String> scripIds = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(csvFile.toPath());
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] tokens = line.split(CSV_DELIMITER);
                if (tokens.length < CSV_MIN_COLUMNS) continue;
                String symbol = tokens[CSV_SYMBOL_INDEX].trim();
                if (symbol.isEmpty()) continue;
                scripRepository.findBySymbolAndExchange(symbol, Exchange.NSE)
                        .ifPresent(scrip -> scripIds.add(scrip.getId()));
            }
        } catch (IOException ignored) {
        }
        return scripIds;
    }

    private void truncateGroup(Group group) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                TRUNCATE_CONFIRM_TITLE,
                String.format(TRUNCATE_CONFIRM_FORMAT, group.getName()),
                TRUNCATE_CONFIRM_BUTTON);
        dialog.show(owner);
        if (!dialog.isConfirmed()) return;
        group.getScripIds().clear();
        persist();
        rebuildSections();
    }

    private void deleteGroup(Group group) {
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

    private void removeScripFromGroup(Group group, String scripId) {
        group.removeScrip(scripId);
        persist();
        rebuildSections();
    }

    public void setBreadthComputer(BreadthComputer breadthComputer) {
        this.breadthComputer = breadthComputer;
    }

    public void setRsComputer(RSComputer rsComputer) {
        this.rsComputer = rsComputer;
    }

    public void persist() {
        repository.saveAll(groups);
        if (breadthComputer != null) {
            breadthComputer.invalidateAll();
        }
        if (rsComputer != null) {
            rsComputer.invalidateAll();
        }
    }

    public void refresh() {
        rebuildSections();
    }

    private void rebuildSections() {
        sectionsContainer.getChildren().clear();
        for (Group g : groups) {
            sectionsContainer.getChildren().add(buildGroupSection(g));
        }
    }

    private VBox buildGroupSection(Group group) {
        VBox section = new VBox();
        section.getStyleClass().add(SECTION_STYLE);
        boolean expanded = expandedGroups.contains(group.getName());
        ListView<String> listView = buildScripListView(group);
        listView.setVisible(expanded);
        listView.setManaged(expanded);
        HBox header = buildSectionHeader(group, listView);
        section.getChildren().addAll(header, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private void toggleSection(Group group, ListView<String> listView, FontIcon chevron) {
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

    private HBox buildSectionHeader(Group group, ListView<String> listView) {
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

    private void startNameEdit(Group group, StackPane container, Label nameLabel) {
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
                persist();
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

    private ListView<String> buildScripListView(Group group) {
        ObservableList<String> items = FXCollections.observableArrayList(group.getScripIds());
        FilteredList<String> filtered = new FilteredList<>(items, this::matchesSearch);
        ListView<String> listView = new ListView<>(filtered);
        listView.getStyleClass().add(LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.setPrefHeight(filtered.size() * FIXED_CELL_HEIGHT + 2);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(lv -> new ScripIdCell(group, listView));
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lastActiveListView = listView;
                if (onScripSelected != null) {
                    Scrip scrip = scripRepository.findById(newVal).orElse(null);
                    if (scrip != null) onScripSelected.accept(scrip);
                }
            }
        });
        return listView;
    }

    private boolean matchesSearch(String scripId) {
        if (searchText == null || searchText.isEmpty()) return true;
        Scrip scrip = scripRepository.findById(scripId).orElse(null);
        if (scrip == null) return scripId.toLowerCase().contains(searchText);
        return scripRepository.matches(scrip, searchText);
    }

    private final class ScripIdCell extends ListCell<String> {

        private final ScripCellGraphic scripGraphic = new ScripCellGraphic();
        private final HBox container = new HBox(scripGraphic);
        private final Group group;
        private final ListView<String> ownerListView;

        ScripIdCell(Group group, ListView<String> ownerListView) {
            this.group = group;
            this.ownerListView = ownerListView;
            container.setAlignment(Pos.CENTER_LEFT);
            setupDragHandlers();
        }

        private void setupDragHandlers() {
            setOnDragDetected(e -> {
                if (getItem() == null) return;
                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(SCRIP_DRAG_FORMAT, getItem());
                db.setContent(content);
                e.consume();
            });
            setOnDragOver(e -> {
                if (e.getGestureSource() != this && e.getDragboard().hasContent(SCRIP_DRAG_FORMAT)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });
            setOnDragEntered(e -> {
                if (e.getGestureSource() != this && e.getDragboard().hasContent(SCRIP_DRAG_FORMAT)) {
                    getStyleClass().add(DRAG_OVER_STYLE);
                }
                e.consume();
            });
            setOnDragExited(e -> {
                getStyleClass().remove(DRAG_OVER_STYLE);
                e.consume();
            });
            setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                if (!db.hasContent(SCRIP_DRAG_FORMAT)) return;
                String draggedId = (String) db.getContent(SCRIP_DRAG_FORMAT);
                String targetId = getItem();
                if (draggedId != null && targetId != null && !draggedId.equals(targetId)) {
                    int fromIndex = group.getScripIds().indexOf(draggedId);
                    int toIndex = group.getScripIds().indexOf(targetId);
                    if (fromIndex >= 0 && toIndex >= 0) {
                        group.moveScrip(fromIndex, toIndex);
                        persist();
                        rebuildSections();
                    }
                }
                e.setDropCompleted(true);
                e.consume();
            });
            setOnDragDone(e -> {
                getStyleClass().remove(DRAG_OVER_STYLE);
                e.consume();
            });
        }

        @Override
        protected void updateItem(String scripId, boolean empty) {
            super.updateItem(scripId, empty);
            if (empty || scripId == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            Scrip scrip = scripRepository.findById(scripId).orElse(null);
            scripGraphic.update(
                    scrip != null ? scrip.getScripType() : null,
                    scrip != null ? scrip.getSymbol() : scripId,
                    scripId);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button cellDeleteBtn = new Button();
            FontIcon cellDeleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            cellDeleteIcon.setIconSize(CELL_DELETE_ICON_SIZE);
            cellDeleteIcon.getStyleClass().add(ICON_STYLE);
            cellDeleteBtn.setGraphic(cellDeleteIcon);
            cellDeleteBtn.getStyleClass().add(CELL_DELETE_STYLE);
            cellDeleteBtn.setFocusTraversable(false);
            cellDeleteBtn.setOnAction(ev -> {
                List.copyOf(ownerListView.getSelectionModel().getSelectedItems())
                        .forEach(group::removeScrip);
                persist();
                rebuildSections();
            });
            container.getChildren().setAll(scripGraphic, spacer,
                    scripGraphic.getPortfolioQtyLabel(), cellDeleteBtn);
            setGraphic(container);
            ContextMenu ctx = new ContextMenu();
            if (watchlistPane != null) {
                ctx.getItems().add(AddToCollectionMenuBuilder.build(
                        ADD_TO_WATCHLIST_LABEL,
                        FluentUiRegularMZ.STAR_16,
                        () -> watchlistPane.getWatchlists(),
                        () -> List.copyOf(ownerListView.getSelectionModel().getSelectedItems()),
                        () -> { watchlistPane.syncAllItems(); watchlistPane.persist(); }
                ));
            }
            FontIcon removeIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            removeIcon.setIconSize(CONTEXT_ICON_SIZE);
            removeIcon.getStyleClass().add(CONTEXT_DELETE_ICON_STYLE);
            MenuItem removeItem = new MenuItem(REMOVE_LABEL, removeIcon);
            removeItem.setOnAction(e -> {
                List.copyOf(ownerListView.getSelectionModel().getSelectedItems())
                        .forEach(group::removeScrip);
                persist();
                rebuildSections();
            });
            ctx.getItems().add(removeItem);
            setContextMenu(ctx);
        }

    }

}
