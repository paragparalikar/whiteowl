package com.whiteowl.workbench.ranker;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.collection.CollectionResolver;
import com.whiteowl.core.ranker.RankResult;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerListener;
import com.whiteowl.core.ranker.RankerRegistry;
import com.whiteowl.core.ranker.RankerService;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.CollectionPickerPane;
import com.whiteowl.workbench.common.ExchangeFilterCombo;
import com.whiteowl.workbench.common.ScripBadge;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.common.ScripTypeFilterCombo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RankerPane extends VBox implements ScripNavigable {

    private static final String PANE_STYLE = "ranker-pane";
    private static final String SECTION_STYLE = "ranker-section";
    private static final String COMBO_STYLE = "ranker-combo";
    private static final String BUTTON_STYLE = "ranker-button";
    private static final String BUTTON_ICON_STYLE = "ranker-button-icon";
    private static final String EDIT_BUTTON_STYLE = "ranker-edit-button";
    private static final String EDIT_ICON_STYLE = "ranker-edit-icon";
    private static final String CANCEL_BUTTON_STYLE = "ranker-cancel-button";
    private static final String CANCEL_ICON_STYLE = "ranker-cancel-icon";
    private static final String SORT_BUTTON_STYLE = "ranker-sort-button";
    private static final String SORT_ICON_STYLE = "ranker-sort-icon";
    private static final String RANKER_ROW_STYLE = "ranker-row";
    private static final String LIST_STYLE = "ranker-list";
    private static final String CELL_SYMBOL_STYLE = "ranker-cell-symbol";
    private static final String PROGRESS_CONTAINER_STYLE = "ranker-progress-container";
    private static final String PROGRESS_BAR_STYLE = "ranker-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "ranker-progress-label";
    private static final String RESULT_HEADER_STYLE = "ranker-result-header";
    private static final String RUN_LABEL = "Run";
    private static final String CANCEL_LABEL = "Cancel";
    private static final String RUNNING_FORMAT = "Ranking %d / %d scrips...";
    private static final String RESULTS_HEADER_FORMAT = "Results (%d)";
    private static final int SECTION_SPACING = 8;
    private static final int ICON_SIZE = 14;
    private static final int EDIT_ICON_SIZE = 12;
    private static final int CANCEL_ICON_SIZE = 12;
    private static final int SORT_ICON_SIZE = 14;
    private static final int FIXED_CELL_HEIGHT = 24;
    private static final int BADGE_GAP = 6;
    private static final int BUTTON_SPACING = 8;
    private static final String OFFSET_LABEL_STYLE = "ranker-offset-label";
    private static final String OFFSET_SPINNER_STYLE = "ranker-offset-spinner";
    private static final String OFFSET_ROW_STYLE = "ranker-offset-row";
    private static final String OFFSET_LABEL_TEXT = "Offset";
    private static final String SCRIP_TYPE_COMBO_STYLE = "ranker-combo";
    private static final int RANKER_ROW_GAP = 4;
    private static final int DEFAULT_OFFSET = 0;
    private static final int MIN_OFFSET = 0;
    private static final int MAX_OFFSET = Integer.MAX_VALUE;

    private final RankerService rankerService;
    private final RankerRegistry rankerRegistry;
    private final ScripRepository scripRepository;
    private final ComboBox<Ranker> rankerCombo;
    private final Button editButton;
    private final ScripTypeFilterCombo scripTypeCombo;
    private final ExchangeFilterCombo exchangeCombo;
    private final Spinner<Integer> offsetSpinner;
    private final CollectionPickerPane collectionPicker;
    private final Button runButton;
    private final Button cancelButton;
    private final Button sortButton;
    private final ListView<RankResult> resultListView;
    private final ObservableList<RankResult> resultList;
    private final List<RankResult> unsortedResults;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label resultHeader;
    private final VBox progressContainer;
    private Consumer<Scrip> onScripSelected;
    private Timeframe selectedTimeframe = Timeframe.DAILY;
    private boolean ascending = true;

    public RankerPane(ScripRepository scripRepository, BarsRepository barsRepository,
                      CollectionResolver collectionResolver) {
        this.scripRepository = scripRepository;
        this.rankerService = new RankerService(scripRepository, barsRepository);
        this.rankerRegistry = new RankerRegistry(barsRepository);
        this.rankerCombo = buildRankerCombo();
        this.editButton = buildEditButton();
        this.scripTypeCombo = buildScripTypeCombo();
        this.exchangeCombo = buildExchangeCombo();
        this.offsetSpinner = buildOffsetSpinner();
        this.collectionPicker = new CollectionPickerPane(collectionResolver);
        this.runButton = buildRunButton();
        this.cancelButton = buildCancelButton();
        this.sortButton = buildSortButton();
        this.resultList = FXCollections.observableArrayList();
        this.unsortedResults = new ArrayList<>();
        this.resultListView = buildResultListView();
        this.progressBar = buildProgressBar();
        this.progressLabel = buildProgressLabel();
        this.resultHeader = buildResultHeader();
        this.progressContainer = buildProgressContainer();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    public void setOnScripSelected(Consumer<Scrip> handler) {
        this.onScripSelected = handler;
    }

    @Override
    public void selectNext() {
        int size = resultList.size();
        if (size == 0) return;
        int current = resultListView.getSelectionModel().getSelectedIndex();
        if (current < size - 1) {
            resultListView.getSelectionModel().clearAndSelect(current + 1);
            resultListView.scrollTo(current + 1);
        }
    }

    @Override
    public void selectPrevious() {
        int size = resultList.size();
        if (size == 0) return;
        int current = resultListView.getSelectionModel().getSelectedIndex();
        if (current > 0) {
            resultListView.getSelectionModel().clearAndSelect(current - 1);
            resultListView.scrollTo(current - 1);
        }
    }

    private void buildLayout() {
        VBox configSection = buildConfigSection();
        getChildren().addAll(configSection, new Separator(), resultHeader,
                resultListView, progressContainer);
    }

    private VBox buildConfigSection() {
        HBox rankerRow = new HBox(RANKER_ROW_GAP, rankerCombo, editButton);
        rankerRow.setAlignment(Pos.CENTER_LEFT);
        rankerRow.getStyleClass().add(RANKER_ROW_STYLE);
        HBox.setHgrow(rankerCombo, Priority.ALWAYS);
        Label offsetLabel = new Label(OFFSET_LABEL_TEXT);
        offsetLabel.getStyleClass().add(OFFSET_LABEL_STYLE);
        Region offsetSpacer = new Region();
        HBox.setHgrow(offsetSpacer, Priority.ALWAYS);
        HBox offsetRow = new HBox(BUTTON_SPACING, offsetLabel, offsetSpacer, offsetSpinner);
        offsetRow.setAlignment(Pos.CENTER_LEFT);
        offsetRow.getStyleClass().add(OFFSET_ROW_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(BUTTON_SPACING, runButton, sortButton, spacer, cancelButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(SECTION_SPACING, rankerRow, exchangeCombo, scripTypeCombo, offsetRow, collectionPicker, buttonRow);
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    private ComboBox<Ranker> buildRankerCombo() {
        ComboBox<Ranker> combo = new ComboBox<>();
        combo.getItems().addAll(rankerRegistry.getRankers());
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setCellFactory(lv -> new RankerCell());
        combo.setButtonCell(new RankerCell());
        return combo;
    }

    private Button buildEditButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.EDIT_16);
        icon.setIconSize(EDIT_ICON_SIZE);
        icon.getStyleClass().add(EDIT_ICON_STYLE);
        Button button = new Button(null, icon);
        button.getStyleClass().add(EDIT_BUTTON_STYLE);
        button.setOnAction(e -> openConfigDialog());
        return button;
    }

    private ScripTypeFilterCombo buildScripTypeCombo() {
        ScripTypeFilterCombo combo = new ScripTypeFilterCombo(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(SCRIP_TYPE_COMBO_STYLE);
        return combo;
    }

    private ExchangeFilterCombo buildExchangeCombo() {
        ExchangeFilterCombo combo = new ExchangeFilterCombo(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(SCRIP_TYPE_COMBO_STYLE);
        return combo;
    }

    private Spinner<Integer> buildOffsetSpinner() {
        Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_OFFSET, MAX_OFFSET, DEFAULT_OFFSET));
        spinner.setEditable(true);
        spinner.setPrefWidth(90);
        spinner.getStyleClass().add(OFFSET_SPINNER_STYLE);
        return spinner;
    }

    private Button buildRunButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.PLAY_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(BUTTON_ICON_STYLE);
        Button button = new Button(RUN_LABEL, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> startRanker());
        return button;
    }

    private Button buildCancelButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(CANCEL_ICON_SIZE);
        icon.getStyleClass().add(CANCEL_ICON_STYLE);
        Button button = new Button(CANCEL_LABEL, icon);
        button.getStyleClass().add(CANCEL_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> cancelRanker());
        return button;
    }

    private Button buildSortButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ARROW_UP_20);
        icon.setIconSize(SORT_ICON_SIZE);
        icon.getStyleClass().add(SORT_ICON_STYLE);
        Button button = new Button(null, icon);
        button.getStyleClass().add(SORT_BUTTON_STYLE);
        button.setOnAction(e -> toggleSortOrder());
        return button;
    }

    private ListView<RankResult> buildResultListView() {
        ListView<RankResult> list = new ListView<>(resultList);
        list.getStyleClass().add(LIST_STYLE);
        list.setFixedCellSize(FIXED_CELL_HEIGHT);
        list.setCellFactory(lv -> new ResultCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onScripSelected != null) {
                onScripSelected.accept(newVal.getScrip());
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        return list;
    }

    private ProgressBar buildProgressBar() {
        ProgressBar bar = new ProgressBar(0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add(PROGRESS_BAR_STYLE);
        return bar;
    }

    private Label buildProgressLabel() {
        Label label = new Label();
        label.getStyleClass().add(PROGRESS_LABEL_STYLE);
        return label;
    }

    private Label buildResultHeader() {
        Label label = new Label(String.format(RESULTS_HEADER_FORMAT, 0));
        label.getStyleClass().add(RESULT_HEADER_STYLE);
        return label;
    }

    private VBox buildProgressContainer() {
        VBox container = new VBox(SECTION_SPACING, progressBar, progressLabel);
        container.getStyleClass().add(PROGRESS_CONTAINER_STYLE);
        container.setVisible(false);
        container.setManaged(false);
        return container;
    }

    private void openConfigDialog() {
        Ranker ranker = rankerCombo.getValue();
        if (ranker == null) return;
        RankerConfigDialog dialog = new RankerConfigDialog(ranker, scripTypeCombo.getValue(), exchangeCombo.getValue(), selectedTimeframe, offsetSpinner.getValue(), scripRepository);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            scripTypeCombo.setValue(dialog.getScripType());
            exchangeCombo.setValue(dialog.getExchange());
            selectedTimeframe = dialog.getTimeframe();
            offsetSpinner.getValueFactory().setValue(dialog.getOffset());
            startRanker();
        }
    }

    private void startRanker() {
        Ranker ranker = rankerCombo.getValue();
        if (ranker == null) return;
        resultList.clear();
        unsortedResults.clear();
        updateResultHeader();
        setRunning(true);
        showProgressContainer(true);
        progressBar.setProgress(0);
        progressLabel.setText("");
        ScripType scripType = scripTypeCombo.getValue();
        Exchange exchange = exchangeCombo.getValue();
        int offset = offsetSpinner.getValue();
        Set<String> scripIdFilter = collectionPicker.resolveScripIds();
        Thread thread = new Thread(() -> runRanker(ranker, scripType, exchange, selectedTimeframe, offset, scripIdFilter));
        thread.setDaemon(true);
        thread.setName("ranker-thread");
        thread.start();
    }

    private void runRanker(Ranker ranker, ScripType scripType, Exchange exchange, Timeframe timeframe, int offset,
                            Set<String> scripIdFilter) {
        rankerService.run(ranker, scripType, exchange, timeframe, offset, new RankerListener() {
            @Override
            public void onProgress(int completed, int total) {
                Platform.runLater(() -> {
                    double fraction = total > 0 ? (double) completed / total : 0;
                    progressBar.setProgress(fraction);
                    progressLabel.setText(String.format(RUNNING_FORMAT, completed, total));
                });
            }

            @Override
            public void onRanked(Scrip scrip, Double value) {
                Platform.runLater(() -> {
                    unsortedResults.add(new RankResult(scrip, value));
                    updateResultHeader();
                });
            }

            @Override
            public void onError(Scrip scrip, String error) {
            }
        }, scripIdFilter);
        Platform.runLater(this::onFinished);
    }

    private void cancelRanker() {
        rankerService.cancel();
    }

    private void onFinished() {
        sortAndDisplay();
        showProgressContainer(false);
        setRunning(false);
    }

    private void setRunning(boolean running) {
        rankerCombo.setDisable(running);
        editButton.setDisable(running);
        scripTypeCombo.setDisable(running);
        offsetSpinner.setDisable(running);
        collectionPicker.setPickerDisabled(running);
        runButton.setDisable(running);
        sortButton.setDisable(running);
        cancelButton.setDisable(!running);
    }

    private void showProgressContainer(boolean show) {
        progressContainer.setVisible(show);
        progressContainer.setManaged(show);
    }

    private void updateResultHeader() {
        resultHeader.setText(String.format(RESULTS_HEADER_FORMAT, unsortedResults.size()));
    }

    private void toggleSortOrder() {
        ascending = !ascending;
        FontIcon icon = ascending
                ? new FontIcon(FluentUiRegularAL.ARROW_UP_20)
                : new FontIcon(FluentUiRegularAL.ARROW_DOWN_20);
        icon.setIconSize(SORT_ICON_SIZE);
        icon.getStyleClass().add(SORT_ICON_STYLE);
        sortButton.setGraphic(icon);
        sortAndDisplay();
    }

    private void sortAndDisplay() {
        List<RankResult> sorted = new ArrayList<>(unsortedResults);
        Comparator<RankResult> comparator = Comparator.comparingDouble(RankResult::getValue);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        resultList.setAll(sorted);
    }

    private static final class RankerCell extends ListCell<Ranker> {

        @Override
        protected void updateItem(Ranker ranker, boolean empty) {
            super.updateItem(ranker, empty);
            if (empty || ranker == null) {
                setText(null);
                return;
            }
            setText(ranker.getName());
        }

    }

    private final class ResultCell extends ListCell<RankResult> {

        private final Label symbolLabel = new Label();
        private final HBox container = new HBox(BADGE_GAP);

        ResultCell() {
            symbolLabel.getStyleClass().add(CELL_SYMBOL_STYLE);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(RankResult result, boolean empty) {
            super.updateItem(result, empty);
            if (empty || result == null) {
                setGraphic(null);
                return;
            }
            symbolLabel.setText(result.getScrip().getSymbol());
            container.getChildren().setAll(ScripBadge.create(result.getScrip().getScripType()), symbolLabel);
            setGraphic(container);
        }

    }

}
