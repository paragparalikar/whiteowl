package com.whiteowl.workbench.watchlist;

import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.watchlist.model.Watchlist;
import com.whiteowl.workbench.watchlist.repository.WatchlistRepository;
import com.whiteowl.workbench.common.AddToCollectionMenuBuilder;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.common.NameInputDialog;
import com.whiteowl.workbench.common.ScripCellGraphic;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.group.GroupPane;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class WatchlistPane extends VBox implements ScripNavigable {

    private static final String PANE_STYLE = "watchlist-pane";
    private static final String TOOLBAR_STYLE = "watchlist-toolbar";
    private static final String ADD_BUTTON_STYLE = "watchlist-add-button";
    private static final String LIST_STYLE = "watchlist-list";

    private static final String SECTION_STYLE = "watchlist-section";
    private static final String SECTION_HEADER_STYLE = "watchlist-section-header";
    private static final String SECTION_CHEVRON_STYLE = "watchlist-section-chevron";
    private static final String SECTION_NAME_STYLE = "watchlist-section-name";
    private static final String SECTION_DELETE_STYLE = "watchlist-section-delete";
    private static final String SECTION_TRUNCATE_STYLE = "watchlist-section-truncate";
    private static final int CHEVRON_SIZE = 10;
    private static final String SECTION_EDIT_FIELD_STYLE = "watchlist-section-edit-field";
    private static final String SEARCH_FIELD_STYLE = "watchlist-search-field";
    private static final String ADD_PROMPT = "Enter watchlist name";
    private static final String DIALOG_TITLE = "New Watchlist";
    private static final String SEARCH_PROMPT = "Search scrips...";
    private static final String REMOVE_LABEL = "Remove";
    private static final String DELETE_CONFIRM_TITLE = "Delete Watchlist";
    private static final String DELETE_CONFIRM_FORMAT = "Are you sure you want to delete '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String TRUNCATE_CONFIRM_TITLE = "Clear Watchlist";
    private static final String TRUNCATE_CONFIRM_FORMAT = "Are you sure you want to remove all scrips from '%s'?";
    private static final String TRUNCATE_CONFIRM_BUTTON = "Clear";
    private static final String ADD_TO_GROUP_LABEL = "Add to Group";
    private static final int CONTEXT_ICON_SIZE = 16;
    private static final String CONTEXT_DELETE_ICON_STYLE = "context-delete-icon";
    private static final String DRAG_OVER_STYLE = "watchlist-cell-drag-over";
    private static final DataFormat SCRIP_DRAG_FORMAT = new DataFormat("application/x-whiteowl-scrip-drag");
    private static final int FIXED_CELL_HEIGHT = 24;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int ICON_SIZE = 14;
    private static final int HEADER_ICON_SIZE = 12;
    private static final String CELL_DELETE_STYLE = "watchlist-cell-delete";
    private static final int CELL_DELETE_ICON_SIZE = 12;
    private static final String MOVE_TO_TOP_LABEL = "Move To Top";
    private static final String MOVE_TO_BOTTOM_LABEL = "Move To Bottom";

    private final WatchlistRepository repository;
    private final ScripRepository scripRepository;
    private final List<Watchlist> watchlists;
    private final VBox sectionsContainer;
    private final Set<String> expandedWatchlists = new HashSet<>();
    private final Map<Watchlist, ObservableList<String>> itemsByWatchlist = new HashMap<>();
    private final Map<Watchlist, ListView<String>> listViewByWatchlist = new HashMap<>();
    private String searchText = "";
    private Consumer<Scrip> onScripSelected;
    private Runnable onWatchlistsChanged;
    private GroupPane groupPane;
    private ListView<String> lastActiveListView;

    public WatchlistPane(WatchlistRepository repository, ScripRepository scripRepository) {
        this.repository = repository;
        this.scripRepository = scripRepository;
        this.watchlists = new ArrayList<>(repository.loadAll());
        this.sectionsContainer = new VBox();
        getStyleClass().add(PANE_STYLE);
        getChildren().addAll(buildToolbar(), sectionsContainer);
        VBox.setVgrow(sectionsContainer, Priority.ALWAYS);
        rebuildSections();
    }

    public void setOnScripSelected(Consumer<Scrip> handler) {
        this.onScripSelected = handler;
    }

    public void setOnWatchlistsChanged(Runnable handler) {
        this.onWatchlistsChanged = handler;
    }

    public List<Watchlist> getWatchlists() {
        return watchlists;
    }

    public void setGroupPane(GroupPane groupPane) {
        this.groupPane = groupPane;
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

    public void addScripToWatchlist(Watchlist watchlist, String scripId) {
        watchlist.addScrip(scripId);
        ObservableList<String> items = itemsByWatchlist.get(watchlist);
        if (items != null) {
            items.add(scripId);
            updateListViewHeight(watchlist);
        }
        persist();
    }

    public void refresh() {
        rebuildSections();
    }

    public void syncAllItems() {
        for (Watchlist wl : watchlists) {
            syncItems(wl);
        }
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
        icon.getStyleClass().add("watchlist-icon");
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
            watchlists.add(new Watchlist(name));
            persist();
            rebuildSections();
        }
    }

    private void truncateWatchlist(Watchlist watchlist) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                TRUNCATE_CONFIRM_TITLE,
                String.format(TRUNCATE_CONFIRM_FORMAT, watchlist.getName()),
                TRUNCATE_CONFIRM_BUTTON);
        dialog.show(owner);
        if (!dialog.isConfirmed()) return;
        watchlist.getScripIds().clear();
        persist();
        rebuildSections();
    }

    private void deleteWatchlist(Watchlist watchlist) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, watchlist.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(owner);
        if (!dialog.isConfirmed()) return;
        watchlists.remove(watchlist);
        persist();
        rebuildSections();
    }

    public void persist() {
        repository.saveAll(watchlists);
        if (onWatchlistsChanged != null) onWatchlistsChanged.run();
    }

    private void rebuildSections() {
        itemsByWatchlist.clear();
        listViewByWatchlist.clear();
        lastActiveListView = null;
        sectionsContainer.getChildren().clear();
        for (Watchlist wl : watchlists) {
            sectionsContainer.getChildren().add(buildWatchlistSection(wl));
        }
    }

    private void syncItems(Watchlist watchlist) {
        ObservableList<String> items = itemsByWatchlist.get(watchlist);
        if (items != null) {
            items.setAll(watchlist.getScripIds());
            updateListViewHeight(watchlist);
        }
    }

    private void selectAfterRemove(Watchlist watchlist, int removedIndex) {
        ListView<String> lv = listViewByWatchlist.get(watchlist);
        if (lv == null || lv.getItems().isEmpty()) return;
        int selectIndex = Math.min(removedIndex, lv.getItems().size() - 1);
        lv.getSelectionModel().clearAndSelect(selectIndex);
        lv.scrollTo(selectIndex);
    }

    private void selectAfterMove(Watchlist watchlist, int originalIndex) {
        ListView<String> lv = listViewByWatchlist.get(watchlist);
        if (lv == null || lv.getItems().isEmpty()) return;
        int selectIndex = Math.min(originalIndex, lv.getItems().size() - 1);
        lv.getSelectionModel().clearAndSelect(selectIndex);
        lv.scrollTo(selectIndex);
    }

    private void updateListViewHeight(Watchlist watchlist) {
        ListView<String> lv = listViewByWatchlist.get(watchlist);
        ObservableList<String> items = itemsByWatchlist.get(watchlist);
        if (lv != null && items != null) {
            lv.setPrefHeight(items.size() * FIXED_CELL_HEIGHT + 2);
        }
    }

    private VBox buildWatchlistSection(Watchlist watchlist) {
        VBox section = new VBox();
        section.getStyleClass().add(SECTION_STYLE);
        boolean expanded = expandedWatchlists.contains(watchlist.getName());
        ListView<String> listView = buildScripListView(watchlist);
        listView.setVisible(expanded);
        listView.setManaged(expanded);
        HBox header = buildSectionHeader(watchlist, listView);
        section.getChildren().addAll(header, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return section;
    }

    private void toggleSection(Watchlist watchlist, ListView<String> listView, FontIcon chevron) {
        boolean expanded = !listView.isVisible();
        listView.setVisible(expanded);
        listView.setManaged(expanded);
        chevron.setIconCode(expanded ? FluentUiRegularAL.CHEVRON_DOWN_12 : FluentUiRegularAL.CHEVRON_RIGHT_12);
        if (expanded) {
            expandedWatchlists.add(watchlist.getName());
        } else {
            expandedWatchlists.remove(watchlist.getName());
        }
    }

    private HBox buildSectionHeader(Watchlist watchlist, ListView<String> listView) {
        HBox header = new HBox(4);
        header.getStyleClass().add(SECTION_HEADER_STYLE);
        header.setPadding(new Insets(2, 4, 2, 4));
        boolean expanded = expandedWatchlists.contains(watchlist.getName());
        FontIcon chevron = new FontIcon(expanded ? FluentUiRegularAL.CHEVRON_DOWN_12 : FluentUiRegularAL.CHEVRON_RIGHT_12);
        chevron.setIconSize(CHEVRON_SIZE);
        chevron.getStyleClass().add(SECTION_CHEVRON_STYLE);
        Button chevronBtn = new Button();
        chevronBtn.setGraphic(chevron);
        chevronBtn.getStyleClass().add(SECTION_DELETE_STYLE);
        chevronBtn.setFocusTraversable(false);
        chevronBtn.setOnAction(e -> toggleSection(watchlist, listView, chevron));
        StackPane nameContainer = new StackPane();
        Label nameLabel = new Label(watchlist.getName());
        nameLabel.getStyleClass().add(SECTION_NAME_STYLE);
        nameContainer.getChildren().add(nameLabel);
        HBox.setHgrow(nameContainer, Priority.ALWAYS);
        nameContainer.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                startNameEdit(watchlist, nameContainer, nameLabel);
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                toggleSection(watchlist, listView, chevron);
            }
        });
        Button truncateButton = new Button();
        FontIcon truncateIcon = new FontIcon(FluentUiRegularAL.ERASER_20);
        truncateIcon.setIconSize(HEADER_ICON_SIZE);
        truncateIcon.getStyleClass().add("watchlist-icon");
        truncateButton.setGraphic(truncateIcon);
        truncateButton.getStyleClass().add(SECTION_TRUNCATE_STYLE);
        truncateButton.setFocusTraversable(false);
        truncateButton.setOnAction(e -> truncateWatchlist(watchlist));
        Button deleteButton = new Button();
        FontIcon deleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
        deleteIcon.setIconSize(HEADER_ICON_SIZE);
        deleteIcon.getStyleClass().add("watchlist-icon");
        deleteButton.setGraphic(deleteIcon);
        deleteButton.getStyleClass().add(SECTION_DELETE_STYLE);
        deleteButton.setFocusTraversable(false);
        deleteButton.setOnAction(e -> deleteWatchlist(watchlist));
        header.getChildren().addAll(chevronBtn, nameContainer, truncateButton, deleteButton);
        return header;
    }

    private void startNameEdit(Watchlist watchlist, StackPane container, Label nameLabel) {
        TextField editField = new TextField(watchlist.getName());
        editField.getStyleClass().add(SECTION_EDIT_FIELD_STYLE);
        editField.selectAll();
        container.getChildren().setAll(editField);
        editField.requestFocus();
        Runnable commitEdit = () -> {
            String newName = editField.getText();
            if (newName != null && !newName.isBlank()
                    && newName.trim().length() >= MIN_NAME_LENGTH
                    && newName.trim().length() <= MAX_NAME_LENGTH) {
                watchlist.setName(newName.trim());
                persist();
            }
            nameLabel.setText(watchlist.getName());
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

    private ListView<String> buildScripListView(Watchlist watchlist) {
        ObservableList<String> items = FXCollections.observableArrayList(watchlist.getScripIds());
        itemsByWatchlist.put(watchlist, items);
        FilteredList<String> filtered = new FilteredList<>(items, this::matchesSearch);
        ListView<String> listView = new ListView<>(filtered);
        listViewByWatchlist.put(watchlist, listView);
        listView.getStyleClass().add(LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.setPrefHeight(filtered.size() * FIXED_CELL_HEIGHT + 2);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(lv -> new ScripIdCell(watchlist, listView));
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
        private final Watchlist watchlist;
        private final ListView<String> ownerListView;

        ScripIdCell(Watchlist watchlist, ListView<String> ownerListView) {
            this.watchlist = watchlist;
            this.ownerListView = ownerListView;
            container.setAlignment(Pos.CENTER_LEFT);
            setOnMousePressed(e -> ownerListView.requestFocus());
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
                    int fromIndex = watchlist.getScripIds().indexOf(draggedId);
                    int toIndex = watchlist.getScripIds().indexOf(targetId);
                    if (fromIndex >= 0 && toIndex >= 0) {
                        watchlist.moveScrip(fromIndex, toIndex);
                        persist();
                        syncItems(watchlist);
                        selectAfterMove(watchlist, fromIndex);
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
            cellDeleteIcon.getStyleClass().add("watchlist-icon");
            cellDeleteBtn.setGraphic(cellDeleteIcon);
            cellDeleteBtn.getStyleClass().add(CELL_DELETE_STYLE);
            cellDeleteBtn.setFocusTraversable(false);
            cellDeleteBtn.setOnAction(ev -> {
                int idx = watchlist.getScripIds().indexOf(scripId);
                List.copyOf(ownerListView.getSelectionModel().getSelectedItems())
                        .forEach(watchlist::removeScrip);
                persist();
                syncItems(watchlist);
                selectAfterRemove(watchlist, idx);
            });
            container.getChildren().setAll(scripGraphic, spacer,
                    scripGraphic.getPortfolioQtyLabel(), cellDeleteBtn);
            setGraphic(container);
            ContextMenu ctx = new ContextMenu();
            if (groupPane != null) {
                ctx.getItems().add(AddToCollectionMenuBuilder.build(
                        ADD_TO_GROUP_LABEL,
                        FluentUiRegularMZ.TAG_16,
                        () -> groupPane.getGroups(),
                        () -> List.copyOf(ownerListView.getSelectionModel().getSelectedItems()),
                        () -> { groupPane.persist(); groupPane.refresh(); }
                ));
            }
            FontIcon removeIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            removeIcon.setIconSize(CONTEXT_ICON_SIZE);
            removeIcon.getStyleClass().add(CONTEXT_DELETE_ICON_STYLE);
            MenuItem removeItem = new MenuItem(REMOVE_LABEL, removeIcon);
            removeItem.setOnAction(e -> {
                int idx = watchlist.getScripIds().indexOf(scripId);
                List.copyOf(ownerListView.getSelectionModel().getSelectedItems())
                        .forEach(watchlist::removeScrip);
                persist();
                syncItems(watchlist);
                selectAfterRemove(watchlist, idx);
            });
            ctx.getItems().add(removeItem);
            FontIcon moveTopIcon = new FontIcon(FluentUiRegularAL.ARROW_UP_20);
            moveTopIcon.setIconSize(CONTEXT_ICON_SIZE);
            MenuItem moveTopItem = new MenuItem(MOVE_TO_TOP_LABEL, moveTopIcon);
            moveTopItem.setOnAction(e -> {
                int fromIndex = watchlist.getScripIds().indexOf(scripId);
                if (fromIndex > 0) {
                    watchlist.moveScrip(fromIndex, 0);
                    persist();
                    syncItems(watchlist);
                    selectAfterMove(watchlist, fromIndex);
                }
            });
            ctx.getItems().add(moveTopItem);
            FontIcon moveBottomIcon = new FontIcon(FluentUiRegularAL.ARROW_DOWN_20);
            moveBottomIcon.setIconSize(CONTEXT_ICON_SIZE);
            MenuItem moveBottomItem = new MenuItem(MOVE_TO_BOTTOM_LABEL, moveBottomIcon);
            moveBottomItem.setOnAction(e -> {
                int fromIndex = watchlist.getScripIds().indexOf(scripId);
                int lastIndex = watchlist.getScripIds().size() - 1;
                if (fromIndex < lastIndex) {
                    watchlist.moveScrip(fromIndex, lastIndex);
                    persist();
                    syncItems(watchlist);
                    selectAfterMove(watchlist, fromIndex);
                }
            });
            ctx.getItems().add(moveBottomItem);
            setContextMenu(ctx);
        }

    }

}
