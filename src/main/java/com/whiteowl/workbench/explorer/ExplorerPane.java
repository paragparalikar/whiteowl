package com.whiteowl.workbench.explorer;

import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripFilter;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.AddToCollectionMenuBuilder;
import com.whiteowl.workbench.common.ExchangeFilterCombo;
import com.whiteowl.workbench.common.ScripBadge;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.common.ScripTypeFilterCombo;
import com.whiteowl.workbench.group.GroupPane;
import com.whiteowl.workbench.watchlist.WatchlistPane;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ExplorerPane extends VBox implements ScripNavigable {

    private static final String EXPLORER_PANE_STYLE = "explorer-pane";
    private static final String EXPLORER_SEARCH_STYLE = "explorer-search";
    private static final String EXPLORER_FILTER_STYLE = "explorer-filter";
    private static final String EXPLORER_LIST_STYLE = "explorer-list";
    private static final String EXPLORER_CELL_SYMBOL_STYLE = "explorer-cell-symbol";
    private static final String SEARCH_PROMPT = "Search scrips...";
    private static final String ADD_TO_WATCHLIST_LABEL = "Add to Watchlist";
    private static final String ADD_TO_GROUP_LABEL = "Add to Group";
    private static final int FIXED_CELL_HEIGHT = 24;

    private final ScripRepository scripRepository;
    private final ObservableList<Scrip> scripList;
    private final FilteredList<Scrip> filteredScrips;
    private final ListView<Scrip> listView;
    private final ScripTypeFilterCombo typeFilter;
    private final ExchangeFilterCombo exchangeFilter;
    private final TextField searchField;
    private Consumer<Scrip> onScripSelected;
    private WatchlistPane watchlistPane;
    private GroupPane groupPane;

    public ExplorerPane(ScripRepository scripRepository) {
        this.scripRepository = scripRepository;
        getStyleClass().add(EXPLORER_PANE_STYLE);
        this.scripList = FXCollections.observableArrayList(loadScrips());
        this.filteredScrips = new FilteredList<>(scripList, s -> true);
        this.listView = buildListView();
        this.searchField = buildSearchField();
        this.typeFilter = buildTypeFilter();
        this.exchangeFilter = buildExchangeFilter();
        exchangeFilter.setMaxWidth(Double.MAX_VALUE);
        typeFilter.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(searchField, exchangeFilter, typeFilter, listView);
        applyFilter();
    }

    public void refresh() {
        scripList.setAll(loadScrips());
    }

    public void setOnScripSelected(Consumer<Scrip> handler) {
        this.onScripSelected = handler;
    }

    public void setWatchlistPane(WatchlistPane watchlistPane) {
        this.watchlistPane = watchlistPane;
    }

    public void setGroupPane(GroupPane groupPane) {
        this.groupPane = groupPane;
    }

    @Override
    public void selectNext() {
        int size = filteredScrips.size();
        if (size == 0) return;
        int current = listView.getSelectionModel().getSelectedIndex();
        if (current < size - 1) {
            listView.getSelectionModel().clearAndSelect(current + 1);
            listView.scrollTo(current + 1);
        }
    }

    @Override
    public void selectPrevious() {
        int size = filteredScrips.size();
        if (size == 0) return;
        int current = listView.getSelectionModel().getSelectedIndex();
        if (current > 0) {
            listView.getSelectionModel().clearAndSelect(current - 1);
            listView.scrollTo(current - 1);
        }
    }

    private List<Scrip> loadScrips() {
        return scripRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Scrip::getSymbol))
                .toList();
    }

    private TextField buildSearchField() {
        TextField field = new TextField();
        field.setPromptText(SEARCH_PROMPT);
        field.getStyleClass().add(EXPLORER_SEARCH_STYLE);
        field.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        return field;
    }

    private ScripTypeFilterCombo buildTypeFilter() {
        ScripTypeFilterCombo combo = new ScripTypeFilterCombo(true);
        combo.getStyleClass().add(EXPLORER_FILTER_STYLE);
        combo.setOnAction(e -> applyFilter());
        return combo;
    }

    private ExchangeFilterCombo buildExchangeFilter() {
        ExchangeFilterCombo combo = new ExchangeFilterCombo(true);
        combo.getStyleClass().add(EXPLORER_FILTER_STYLE);
        combo.setOnAction(e -> applyFilter());
        return combo;
    }

    private ListView<Scrip> buildListView() {
        ListView<Scrip> listView = new ListView<>(filteredScrips);
        listView.getStyleClass().add(EXPLORER_LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(lv -> new ScripCell());
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldScrip, newScrip) -> {
            if (newScrip != null && onScripSelected != null) {
                onScripSelected.accept(newScrip);
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
        return listView;
    }

    private void applyFilter() {
        String text = searchField.getText();
        ScripType selectedType = typeFilter.getValue();
        Exchange selectedExchange = exchangeFilter.getValue();
        String query = (text == null || text.isBlank()) ? null : text;
        filteredScrips.setPredicate(s -> {
            if (selectedType != null && s.getScripType() != selectedType) return false;
            if (selectedType == ScripType.EQUITY && !ScripFilter.isTradable(s)) return false;
            if (selectedExchange != null && s.getExchange() != selectedExchange) return false;
            if (query == null) return true;
            return scripRepository.matches(s, query);
        });
    }

    private List<String> getSelectedScripIds() {
        return listView.getSelectionModel().getSelectedItems()
                .stream()
                .map(Scrip::getId)
                .collect(Collectors.toList());
    }

    private final class ScripCell extends ListCell<Scrip> {

        private static final int BADGE_GAP = 6;

        private final Label symbolLabel = new Label();
        private final HBox container = new HBox(BADGE_GAP);

        ScripCell() {
            symbolLabel.getStyleClass().add(EXPLORER_CELL_SYMBOL_STYLE);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Scrip scrip, boolean empty) {
            super.updateItem(scrip, empty);
            if (empty || scrip == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            symbolLabel.setText(scrip.getSymbol());
            container.getChildren().setAll(ScripBadge.create(scrip.getScripType()), symbolLabel);
            setGraphic(container);
            ContextMenu ctx = new ContextMenu();
            if (watchlistPane != null) {
                ctx.getItems().add(AddToCollectionMenuBuilder.build(
                        ADD_TO_WATCHLIST_LABEL,
                        FluentUiRegularMZ.STAR_16,
                        () -> watchlistPane.getWatchlists(),
                        ExplorerPane.this::getSelectedScripIds,
                        () -> { watchlistPane.refresh(); watchlistPane.persist(); }
                ));
            }
            if (groupPane != null) {
                ctx.getItems().add(AddToCollectionMenuBuilder.build(
                        ADD_TO_GROUP_LABEL,
                        FluentUiRegularMZ.TAG_16,
                        () -> groupPane.getGroups(),
                        ExplorerPane.this::getSelectedScripIds,
                        () -> { groupPane.persist(); groupPane.refresh(); }
                ));
            }
            if (!ctx.getItems().isEmpty()) {
                setContextMenu(ctx);
            }
        }

    }

}
