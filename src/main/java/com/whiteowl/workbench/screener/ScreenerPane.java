package com.whiteowl.workbench.screener;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.examplegroup.repository.ExampleGroupRepository;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenRegistry;
import com.whiteowl.core.collection.CollectionResolver;
import com.whiteowl.core.screener.ScreenerListener;
import com.whiteowl.core.screener.ScreenerService;
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

import java.util.Set;
import java.util.function.Consumer;

public final class ScreenerPane extends VBox implements ScripNavigable {

    private static final String PANE_STYLE = "screener-pane";
    private static final String SECTION_STYLE = "screener-section";
    private static final String COMBO_STYLE = "screener-combo";
    private static final String BUTTON_STYLE = "screener-button";
    private static final String BUTTON_ICON_STYLE = "screener-button-icon";
    private static final String EDIT_BUTTON_STYLE = "screener-edit-button";
    private static final String EDIT_ICON_STYLE = "screener-edit-icon";
    private static final String CANCEL_BUTTON_STYLE = "screener-cancel-button";
    private static final String CANCEL_ICON_STYLE = "screener-cancel-icon";
    private static final String SCREEN_ROW_STYLE = "screener-screen-row";
    private static final String LIST_STYLE = "screener-list";
    private static final String CELL_SYMBOL_STYLE = "screener-cell-symbol";
    private static final String PROGRESS_CONTAINER_STYLE = "screener-progress-container";
    private static final String PROGRESS_BAR_STYLE = "screener-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "screener-progress-label";
    private static final String RESULT_HEADER_STYLE = "screener-result-header";
    private static final String RUN_LABEL = "Run";
    private static final String CANCEL_LABEL = "Cancel";
    private static final String RUNNING_FORMAT = "Scanning %d / %d scrips...";
    private static final String RESULTS_HEADER_FORMAT = "Results (%d)";
    private static final int SECTION_SPACING = 8;
    private static final int ICON_SIZE = 14;
    private static final int EDIT_ICON_SIZE = 12;
    private static final int CANCEL_ICON_SIZE = 12;
    private static final int FIXED_CELL_HEIGHT = 24;
    private static final int BADGE_GAP = 6;
    private static final int BUTTON_SPACING = 8;
    private static final String OFFSET_LABEL_STYLE = "screener-offset-label";
    private static final String OFFSET_SPINNER_STYLE = "screener-offset-spinner";
    private static final String OFFSET_ROW_STYLE = "screener-offset-row";
    private static final String OFFSET_LABEL_TEXT = "Offset";
    private static final String SCRIP_TYPE_COMBO_STYLE = "screener-combo";
    private static final int SCREEN_ROW_GAP = 4;
    private static final int DEFAULT_OFFSET = 0;
    private static final int MIN_OFFSET = 0;
    private static final int MAX_OFFSET = Integer.MAX_VALUE;
    private final ScreenerService screenerService;
    private final ScreenRegistry screenRegistry;
    private final ComboBox<Screen> screenCombo;
    private final Button editButton;
    private final ScripTypeFilterCombo scripTypeCombo;
    private final ExchangeFilterCombo exchangeCombo;
    private final Spinner<Integer> offsetSpinner;
    private final CollectionPickerPane collectionPicker;
    private final Button runButton;
    private final Button cancelButton;
    private final ListView<Scrip> resultListView;
    private final ObservableList<Scrip> resultList;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label resultHeader;
    private final VBox progressContainer;
    private Consumer<Scrip> onScripSelected;
    private Timeframe selectedTimeframe = Timeframe.DAILY;

    public ScreenerPane(ScripRepository scripRepository, BarsRepository barsRepository,
                        ExampleGroupRepository exampleGroupRepository,
                        CollectionResolver collectionResolver,
                        com.whiteowl.core.script.ScriptRepository screenerScriptRepo) {
        this.screenerService = new ScreenerService(scripRepository, barsRepository);
        this.screenRegistry = new ScreenRegistry(barsRepository, exampleGroupRepository, screenerScriptRepo);
        this.screenCombo = buildScreenCombo();
        this.editButton = buildEditButton();
        this.scripTypeCombo = buildScripTypeCombo();
        this.exchangeCombo = buildExchangeCombo();
        this.offsetSpinner = buildOffsetSpinner();
        this.collectionPicker = new CollectionPickerPane(collectionResolver);
        this.runButton = buildRunButton();
        this.cancelButton = buildCancelButton();
        this.resultList = FXCollections.observableArrayList();
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
        HBox screenRow = new HBox(SCREEN_ROW_GAP, screenCombo, editButton);
        screenRow.setAlignment(Pos.CENTER_LEFT);
        screenRow.getStyleClass().add(SCREEN_ROW_STYLE);
        HBox.setHgrow(screenCombo, Priority.ALWAYS);
        Label offsetLabel = new Label(OFFSET_LABEL_TEXT);
        offsetLabel.getStyleClass().add(OFFSET_LABEL_STYLE);
        Region offsetSpacer = new Region();
        HBox.setHgrow(offsetSpacer, Priority.ALWAYS);
        HBox offsetRow = new HBox(BUTTON_SPACING, offsetLabel, offsetSpacer, offsetSpinner);
        offsetRow.setAlignment(Pos.CENTER_LEFT);
        offsetRow.getStyleClass().add(OFFSET_ROW_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(BUTTON_SPACING, runButton, spacer, cancelButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(SECTION_SPACING, screenRow, exchangeCombo, scripTypeCombo, offsetRow, collectionPicker, buttonRow);
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    private ComboBox<Screen> buildScreenCombo() {
        ComboBox<Screen> combo = new ComboBox<>();
        combo.getItems().addAll(screenRegistry.getBuiltInScreens());
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setCellFactory(lv -> new ScreenCell());
        combo.setButtonCell(new ScreenCell());
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
        button.setOnAction(e -> startScreen());
        return button;
    }

    private Button buildCancelButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(CANCEL_ICON_SIZE);
        icon.getStyleClass().add(CANCEL_ICON_STYLE);
        Button button = new Button(CANCEL_LABEL, icon);
        button.getStyleClass().add(CANCEL_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> cancelScreen());
        return button;
    }

    private ListView<Scrip> buildResultListView() {
        ListView<Scrip> list = new ListView<>(resultList);
        list.getStyleClass().add(LIST_STYLE);
        list.setFixedCellSize(FIXED_CELL_HEIGHT);
        list.setCellFactory(lv -> new ResultCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onScripSelected != null) {
                onScripSelected.accept(newVal);
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
        Screen screen = screenCombo.getValue();
        if (screen == null) return;
        ScreenConfigDialog dialog = new ScreenConfigDialog(screen, scripTypeCombo.getValue(), exchangeCombo.getValue(), selectedTimeframe, offsetSpinner.getValue());
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            scripTypeCombo.setValue(dialog.getScripType());
            exchangeCombo.setValue(dialog.getExchange());
            selectedTimeframe = dialog.getTimeframe();
            offsetSpinner.getValueFactory().setValue(dialog.getOffset());
            startScreen();
        }
    }

    private void startScreen() {
        Screen screen = screenCombo.getValue();
        if (screen == null) return;
        resultList.clear();
        updateResultHeader();
        setRunning(true);
        showProgressContainer(true);
        progressBar.setProgress(0);
        progressLabel.setText("");
        ScripType scripType = scripTypeCombo.getValue();
        Exchange exchange = exchangeCombo.getValue();
        int offset = offsetSpinner.getValue();
        Set<String> scripIdFilter = collectionPicker.resolveScripIds();
        Thread thread = new Thread(() -> runScreen(screen, scripType, exchange, selectedTimeframe, offset, scripIdFilter));
        thread.setDaemon(true);
        thread.setName("screener-thread");
        thread.start();
    }

    private void runScreen(Screen screen, ScripType scripType, Exchange exchange, Timeframe timeframe, int offset,
                           Set<String> scripIdFilter) {
        screenerService.run(screen, scripType, exchange, timeframe, offset, new ScreenerListener() {
            @Override
            public void onProgress(int completed, int total) {
                Platform.runLater(() -> {
                    double fraction = total > 0 ? (double) completed / total : 0;
                    progressBar.setProgress(fraction);
                    progressLabel.setText(String.format(RUNNING_FORMAT, completed, total));
                });
            }

            @Override
            public void onMatch(Scrip scrip) {
                Platform.runLater(() -> {
                    resultList.add(scrip);
                    updateResultHeader();
                });
            }

            @Override
            public void onError(Scrip scrip, String error) {
            }
        }, scripIdFilter);
        Platform.runLater(() -> onFinished());
    }

    private void cancelScreen() {
        screenerService.cancel();
    }

    private void onFinished() {
        showProgressContainer(false);
        setRunning(false);
    }

    private void setRunning(boolean running) {
        screenCombo.setDisable(running);
        editButton.setDisable(running);
        scripTypeCombo.setDisable(running);
        offsetSpinner.setDisable(running);
        collectionPicker.setPickerDisabled(running);
        runButton.setDisable(running);
        cancelButton.setDisable(!running);
    }

    private void showProgressContainer(boolean show) {
        progressContainer.setVisible(show);
        progressContainer.setManaged(show);
    }

    private void updateResultHeader() {
        resultHeader.setText(String.format(RESULTS_HEADER_FORMAT, resultList.size()));
    }

    private static final class ScreenCell extends ListCell<Screen> {

        @Override
        protected void updateItem(Screen screen, boolean empty) {
            super.updateItem(screen, empty);
            if (empty || screen == null) {
                setText(null);
                return;
            }
            setText(screen.getName());
        }

    }

    private final class ResultCell extends ListCell<Scrip> {

        private final Label symbolLabel = new Label();
        private final HBox container = new HBox(BADGE_GAP);

        ResultCell() {
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
