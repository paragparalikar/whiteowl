package com.whiteowl.workbench.downloader;

import com.whiteowl.core.bar.download.BarDownloadListener;
import com.whiteowl.core.bar.download.CompositeBarDataDownloader;
import com.whiteowl.core.bar.download.MarketHours;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.collection.CollectionResolver;
import com.whiteowl.core.scrip.download.ScripDataDownloader;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.workbench.common.CollectionPickerPane;
import com.whiteowl.workbench.common.ExchangeFilterCombo;
import com.whiteowl.workbench.common.ScripBadge;
import com.whiteowl.workbench.explorer.ExplorerPane;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class DownloaderPane extends VBox {

    private static final String PANE_STYLE = "downloader-pane";
    private static final String SCROLL_STYLE = "downloader-scroll";
    private static final String COMBO_STYLE = "downloader-combo";
    private static final String BUTTON_STYLE = "downloader-button";
    private static final String BUTTON_ICON_STYLE = "downloader-button-icon";
    private static final String PROGRESS_CONTAINER_STYLE = "downloader-progress-container";
    private static final String PROGRESS_BAR_STYLE = "downloader-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "downloader-progress-label";
    private static final String MESSAGE_LABEL_STYLE = "downloader-message-label";
    private static final String STOP_BUTTON_STYLE = "downloader-stop-button";
    private static final String STOP_ICON_STYLE = "downloader-stop-icon";
    private static final String SECTION_STYLE = "downloader-section";
    private static final String CHECKBOX_GRID_STYLE = "downloader-checkbox-grid";
    private static final String SECTION_LABEL_STYLE = "downloader-section-label";
    private static final String CHECKBOX_STYLE = "downloader-checkbox";
    private static final String SCRIP_DOWNLOAD_HEADER = "Scrip Data";
    private static final String BAR_DOWNLOAD_HEADER = "Bar Data";
    private static final String TIMEFRAME_HEADER = "Timeframes";
    private static final String SCRIP_TYPE_HEADER = "Scrip Types";
    private static final String FORCE_DOWNLOAD_LABEL = "Force download";
    private static final String BACKWARD_DOWNLOAD_LABEL = "Backward download";
    private static final String DOWNLOAD_SCRIPS_LABEL = "Download Scrips";
    private static final String START_LABEL = "Download";
    private static final String STOP_LABEL = "Stop";
    private static final String READY_MESSAGE = "Ready";
    private static final String SCRIP_DOWNLOADING_FORMAT = "Downloading scrips for %s...";
    private static final String SCRIP_DONE_FORMAT = "Downloaded %d scrips for %s";
    private static final String BAR_DOWNLOADING_FORMAT = "Downloading %s [%s]";
    private static final String FORWARD_FORMAT = "  ↑ Forward: %d bars (%s → %s)";
    private static final String BACKWARD_FORMAT = "  ↓ Backward: %d bars (%s → %s)";
    private static final String CANCELLED_MESSAGE = "Cancelled";
    private static final String COMPLETED_MESSAGE = "Completed";
    private static final String FAILED_MESSAGE = "Download failed";
    private static final String PROGRESS_FORMAT = "%d / %d scrips";
    private static final String EXCHANGE_CELL_STYLE = "downloader-exchange-cell";
    private static final String EM_DASH = " — ";
    private static final int ICON_SIZE = 14;
    private static final int STOP_ICON_SIZE = 12;
    private static final int SECTION_SPACING = 8;
    private static final int GRID_GAP = 6;
    private static final int GRID_COLUMNS = 3;
    private static final int EXCHANGE_CELL_GAP = 8;
    private static final int INNER_SPACING = 4;

    private final ScripDataDownloader scripDownloader;
    private final CompositeBarDataDownloader barDownloader;
    private final ExchangeFilterCombo exchangeCombo;
    private final CheckBox forceDownloadCheck;
    private final Button scripDownloadButton;
    private final List<CheckBox> timeframeChecks;
    private final List<CheckBox> scripTypeChecks;
    private final CheckBox backwardCheck;
    private final CollectionPickerPane collectionPicker;
    private final Button startButton;
    private final Button stopButton;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label messageLabel;
    private final VBox progressContainer;
    private ExplorerPane explorerPane;

    public DownloaderPane(ScripDataDownloader scripDataDownloader,
                          CompositeBarDataDownloader compositeBarDataDownloader,
                          CollectionResolver collectionResolver) {
        this.scripDownloader = scripDataDownloader;
        this.barDownloader = compositeBarDataDownloader;
        this.exchangeCombo = buildExchangeCombo();
        this.forceDownloadCheck = buildCheckBox(FORCE_DOWNLOAD_LABEL, false);
        this.scripDownloadButton = buildScripDownloadButton();
        this.timeframeChecks = buildTimeframeChecks();
        this.scripTypeChecks = buildScripTypeChecks();
        this.backwardCheck = buildCheckBox(BACKWARD_DOWNLOAD_LABEL, true);
        this.collectionPicker = new CollectionPickerPane(collectionResolver);
        this.startButton = buildStartButton();
        this.stopButton = buildStopButton();
        this.progressBar = buildProgressBar();
        this.progressLabel = buildProgressLabel();
        this.messageLabel = buildMessageLabel();
        this.progressContainer = buildProgressContainer();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    public void setExplorerPane(ExplorerPane explorerPane) {
        this.explorerPane = explorerPane;
    }

    private void buildLayout() {
        VBox scripSection = buildScripSection();
        VBox barSection = buildBarSection();
        VBox content = new VBox(scripSection, new Separator(), barSection, progressContainer);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add(SCROLL_STYLE);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private VBox buildScripSection() {
        Label header = createSectionLabel(SCRIP_DOWNLOAD_HEADER);
        HBox buttonRow = new HBox(SECTION_SPACING, scripDownloadButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(SECTION_SPACING, header, exchangeCombo,
                forceDownloadCheck, buttonRow);
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    private VBox buildBarSection() {
        Label header = createSectionLabel(BAR_DOWNLOAD_HEADER);
        VBox timeframes = buildCheckboxGrid(TIMEFRAME_HEADER, timeframeChecks);
        VBox scripTypes = buildCheckboxGrid(SCRIP_TYPE_HEADER, scripTypeChecks);
        HBox buttonRow = new HBox(SECTION_SPACING, startButton, stopButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(SECTION_SPACING, header, timeframes, scripTypes,
                backwardCheck, collectionPicker, buttonRow);
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    private VBox buildCheckboxGrid(String title, List<CheckBox> checks) {
        Label label = createSectionLabel(title);
        GridPane grid = new GridPane();
        grid.setHgap(GRID_GAP);
        grid.setVgap(GRID_GAP);
        grid.getStyleClass().add(CHECKBOX_GRID_STYLE);
        for (int i = 0; i < GRID_COLUMNS; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / GRID_COLUMNS);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < checks.size(); i++) {
            grid.add(checks.get(i), i % GRID_COLUMNS, i / GRID_COLUMNS);
        }
        return new VBox(INNER_SPACING, label, grid);
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(SECTION_LABEL_STYLE);
        return label;
    }

    private ExchangeFilterCombo buildExchangeCombo() {
        ExchangeFilterCombo combo = new ExchangeFilterCombo(false);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        return combo;
    }

    private List<CheckBox> buildTimeframeChecks() {
        List<CheckBox> checks = new ArrayList<>();
        for (Timeframe tf : Timeframe.values()) {
            CheckBox cb = new CheckBox(tf.getDisplayLabel());
            cb.setSelected(tf == Timeframe.DAILY);
            cb.getStyleClass().add(CHECKBOX_STYLE);
            cb.setUserData(tf);
            checks.add(cb);
        }
        return checks;
    }

    private List<CheckBox> buildScripTypeChecks() {
        List<CheckBox> checks = new ArrayList<>();
        for (ScripType type : ScripType.values()) {
            CheckBox cb = new CheckBox();
            cb.setGraphic(buildScripTypeGraphic(type));
            cb.setSelected(type == ScripType.EQUITY || type == ScripType.INDEX);
            cb.getStyleClass().add(CHECKBOX_STYLE);
            cb.setUserData(type);
            checks.add(cb);
        }
        return checks;
    }

    private HBox buildScripTypeGraphic(ScripType type) {
        HBox graphic = new HBox(INNER_SPACING);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setPadding(new Insets(0, 0, 0, INNER_SPACING));
        graphic.getChildren().addAll(ScripBadge.create(type), new Label(type.getDisplayLabel()));
        return graphic;
    }

    private CheckBox buildCheckBox(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.getStyleClass().add(CHECKBOX_STYLE);
        return cb;
    }

    private List<Timeframe> getSelectedTimeframes() {
        List<Timeframe> selected = new ArrayList<>();
        for (CheckBox cb : timeframeChecks) {
            if (cb.isSelected()) selected.add((Timeframe) cb.getUserData());
        }
        return selected;
    }

    private Set<ScripType> getSelectedScripTypes() {
        EnumSet<ScripType> selected = EnumSet.noneOf(ScripType.class);
        for (CheckBox cb : scripTypeChecks) {
            if (cb.isSelected()) selected.add((ScripType) cb.getUserData());
        }
        return selected;
    }

    private Button buildScripDownloadButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ARROW_DOWNLOAD_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(BUTTON_ICON_STYLE);
        Button button = new Button(DOWNLOAD_SCRIPS_LABEL, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> startScripDownload());
        return button;
    }

    private Button buildStartButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ARROW_DOWNLOAD_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(BUTTON_ICON_STYLE);
        Button button = new Button(START_LABEL, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> startBarDownload());
        return button;
    }

    private Button buildStopButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(STOP_ICON_SIZE);
        icon.getStyleClass().add(STOP_ICON_STYLE);
        Button button = new Button(STOP_LABEL, icon);
        button.getStyleClass().add(STOP_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> stopDownload());
        return button;
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

    private Label buildMessageLabel() {
        Label label = new Label(READY_MESSAGE);
        label.getStyleClass().add(MESSAGE_LABEL_STYLE);
        label.setWrapText(true);
        return label;
    }

    private VBox buildProgressContainer() {
        VBox container = new VBox(GRID_GAP, messageLabel, progressBar, progressLabel);
        container.getStyleClass().add(PROGRESS_CONTAINER_STYLE);
        container.setVisible(false);
        container.setManaged(false);
        return container;
    }

    private void startScripDownload() {
        Exchange exchange = exchangeCombo.getValue();
        if (exchange == null) return;
        boolean force = forceDownloadCheck.isSelected();
        setAllDisabled(true);
        showProgressContainer(true);
        progressBar.setProgress(-1);
        progressLabel.setText("");
        messageLabel.setText(String.format(SCRIP_DOWNLOADING_FORMAT, exchange.getCode()));
        Thread thread = new Thread(() -> runScripDownload(exchange, force));
        thread.setDaemon(true);
        thread.setName("scrip-download-thread");
        thread.start();
    }

    private void runScripDownload(Exchange exchange, boolean force) {
        try {
            int count = scripDownloader.download(exchange, force);
            Platform.runLater(() -> {
                messageLabel.setText(String.format(SCRIP_DONE_FORMAT, count, exchange.getCode()));
                if (explorerPane != null) explorerPane.refresh();
                onFinished(String.format(SCRIP_DONE_FORMAT, count, exchange.getCode()));
            });
        } catch (Throwable ex) {
            Platform.runLater(() -> onFinished(FAILED_MESSAGE));
        }
    }

    private void startBarDownload() {
        Exchange exchange = exchangeCombo.getValue();
        List<Timeframe> timeframes = getSelectedTimeframes();
        Set<ScripType> scripTypes = getSelectedScripTypes();
        if (exchange == null || timeframes.isEmpty() || scripTypes.isEmpty()) return;
        boolean backward = backwardCheck.isSelected();
        setAllDisabled(true);
        showProgressContainer(true);
        progressBar.setProgress(0);
        progressLabel.setText("");
        messageLabel.setText(String.format(SCRIP_DOWNLOADING_FORMAT, exchange.getCode()));
        Set<String> scripIdFilter = collectionPicker.resolveScripIds();
        Thread thread = new Thread(() -> runBarDownload(exchange, timeframes, scripTypes, backward, scripIdFilter));
        thread.setDaemon(true);
        thread.setName("bar-download-thread");
        thread.start();
    }

    private void runBarDownload(Exchange exchange, List<Timeframe> timeframes,
                                Set<ScripType> scripTypes, boolean backward,
                                Set<String> scripIdFilter) {
        try {
            int scripCount = scripDownloader.download(exchange);
            Platform.runLater(() -> {
                messageLabel.setText(String.format(SCRIP_DONE_FORMAT, scripCount, exchange.getCode()));
                if (explorerPane != null) explorerPane.refresh();
                stopButton.setDisable(false);
            });
            barDownloader.download(List.of(exchange), timeframes,
                    createBarListener(), scripTypes, backward, scripIdFilter);
            Platform.runLater(() -> onFinished(
                    barDownloader.isCancelled() ? CANCELLED_MESSAGE : COMPLETED_MESSAGE));
        } catch (Throwable ex) {
            Platform.runLater(() -> onFinished(
                    barDownloader.isCancelled() ? CANCELLED_MESSAGE : FAILED_MESSAGE));
        } finally {
            Thread.interrupted();
        }
    }

    private void stopDownload() {
        barDownloader.cancel();
    }

    private void onFinished(String status) {
        messageLabel.setText(status);
        progressBar.setProgress(1.0);
        setAllDisabled(false);
        stopButton.setDisable(true);
    }

    private void setAllDisabled(boolean disabled) {
        scripDownloadButton.setDisable(disabled);
        startButton.setDisable(disabled);
        exchangeCombo.setDisable(disabled);
        forceDownloadCheck.setDisable(disabled);
        backwardCheck.setDisable(disabled);
        collectionPicker.setPickerDisabled(disabled);
        timeframeChecks.forEach(cb -> cb.setDisable(disabled));
        scripTypeChecks.forEach(cb -> cb.setDisable(disabled));
    }

    private void showProgressContainer(boolean show) {
        progressContainer.setVisible(show);
        progressContainer.setManaged(show);
    }

    private BarDownloadListener createBarListener() {
        return new BarDownloadListener() {

            @Override
            public void onScripStart(Scrip scrip, Timeframe timeframe, int completed, int total) {
                Platform.runLater(() -> {
                    messageLabel.setText(String.format(BAR_DOWNLOADING_FORMAT,
                            scrip.getSymbol(), timeframe.getDisplayLabel()));
                    updateBar(completed, total);
                });
            }

            @Override
            public void onForwardComplete(Scrip scrip, Timeframe timeframe, int barCount,
                                          ZonedDateTime from, ZonedDateTime to) {
                Platform.runLater(() -> messageLabel.setText(String.format(FORWARD_FORMAT,
                        barCount, formatDate(from), formatDate(to))));
            }

            @Override
            public void onBackwardComplete(Scrip scrip, Timeframe timeframe, int barCount,
                                           ZonedDateTime from, ZonedDateTime to) {
                Platform.runLater(() -> messageLabel.setText(String.format(BACKWARD_FORMAT,
                        barCount, formatDate(from), formatDate(to))));
            }

            @Override
            public void onScripComplete(Scrip scrip, Timeframe timeframe, int completed, int total) {
                Platform.runLater(() -> updateBar(completed, total));
            }

            @Override
            public void onScripError(Scrip scrip, Timeframe timeframe, String error) {
                Platform.runLater(() -> messageLabel.setText(scrip.getSymbol() + EM_DASH + error));
            }
        };
    }

    private void updateBar(int completed, int total) {
        double fraction = total > 0 ? (double) completed / total : 0;
        progressBar.setProgress(fraction);
        progressLabel.setText(String.format(PROGRESS_FORMAT, completed, total));
    }

    private static String formatDate(ZonedDateTime dateTime) {
        return dateTime != null ? MarketHours.DATE_TIME_FORMAT.format(dateTime) : "";
    }

}
