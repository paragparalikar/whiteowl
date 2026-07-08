package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.dsl.StrategyInputDiscoverer;
import com.whiteowl.core.backtest.v2.engine.BacktestListener;
import com.whiteowl.core.backtest.v2.feature.CsvFeatureConsumer;
import com.whiteowl.core.backtest.v2.feature.FeatureCollector;
import com.whiteowl.core.backtest.v2.metrics.MetricsCalculator;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.service.BacktestService;
import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.common.ExchangeFilterCombo;
import com.whiteowl.workbench.common.ScripTypeFilterCombo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.whiteowl.core.backtest.v2.model.BacktestConfig.DEFAULT_COST_PERCENT;
import static com.whiteowl.core.backtest.v2.model.BacktestConfig.DEFAULT_INITIAL_CAPITAL;
import static com.whiteowl.core.backtest.v2.model.BacktestConfig.DEFAULT_OFFSET;
import static com.whiteowl.core.backtest.v2.model.BacktestConfig.DEFAULT_VOLUME_PARTICIPATION_PERCENT;
import static com.whiteowl.core.backtest.v2.model.BacktestConfig.DEFAULT_SLIPPAGE_PERCENT;

@Slf4j
public final class BacktestPane extends VBox {

    private static final String PANE_STYLE = "backtest-pane";
    private static final String SECTION_STYLE = "backtest-section";
    private static final String COMBO_STYLE = "backtest-combo";
    private static final String BUTTON_STYLE = "backtest-button";
    private static final String BUTTON_ICON_STYLE = "backtest-button-icon";
    private static final String TOOLBAR_BUTTON_STYLE = "backtest-toolbar-button";
    private static final String TOOLBAR_ICON_STYLE = "backtest-toolbar-icon";
    private static final String CANCEL_BUTTON_STYLE = "backtest-cancel-button";
    private static final String CANCEL_ICON_STYLE = "backtest-cancel-icon";
    private static final String TAB_PANE_STYLE = "backtest-config-tab-pane";
    private static final String CONFIG_LABEL_STYLE = "backtest-config-label";
    private static final String CONFIG_FIELD_STYLE = "backtest-config-field";
    private static final String CONFIG_COMBO_STYLE = "backtest-config-combo";
    private static final String CONFIG_SPINNER_STYLE = "backtest-config-spinner";
    private static final String RANGE_HINT_STYLE = "backtest-config-range-hint";
    private static final String RENAME_STRATEGY_TITLE = "Rename Strategy";
    private static final String RENAME_MENU_TEXT = "Rename";
    private static final String DELETE_CONFIRM_TITLE = "Delete Strategy";
    private static final String DELETE_CONFIRM_FORMAT = "Are you sure you want to delete strategy '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String STRATEGY_ROW_STYLE = "backtest-strategy-row";
    private static final String RUN_LABEL = "Run";
    private static final String CANCEL_LABEL = "Cancel";
    private static final String TAB_CONFIG = "Backtest";
    private static final String TAB_INPUTS = "Strategy Inputs";
    private static final String NO_INPUTS_TEXT = "No configurable inputs.";
    private static final String SCRIP_TYPE_LABEL = "Scrip Type";
    private static final String EXCHANGE_LABEL = "Exchange";
    private static final String TIMEFRAME_LABEL = "Timeframe";
    private static final String OFFSET_LABEL = "Offset";
    private static final String CAPITAL_LABEL = "Initial Capital";
    private static final String COST_LABEL = "Trading Cost %";
    private static final String SLIPPAGE_LABEL = "Slippage %";
    private static final String VOLUME_PARTICIPATION_LABEL = "Volume Participation %";
    private static final String GENERATE_FEATURES_LABEL = "Generate Features";
    private static final String CHECKBOX_STYLE = "backtest-config-checkbox";
    private static final String FEATURES_DIR = "features";
    private static final String FEATURES_FILE_EXTENSION = ".csv";
    private static final String RANGE_FORMAT_MIN_MAX = "(%s – %s)";
    private static final String RANGE_FORMAT_MIN_MAX_STEP = "(%s – %s, step %s)";
    private static final String RANGE_FORMAT_MIN = "(min %s)";
    private static final String RANGE_FORMAT_MAX = "(max %s)";
    private static final String RANGE_FORMAT_STEP = "(step %s)";
    private static final int DEFAULT_STEP_VALUE = 1;
    private static final int MIN_OFFSET = 0;
    private static final int MAX_OFFSET = Integer.MAX_VALUE;
    private static final int SECTION_SPACING = 8;
    private static final int ICON_SIZE = 14;
    private static final int TOOLBAR_ICON_SIZE = 12;
    private static final int CANCEL_ICON_SIZE = 12;
    private static final int BUTTON_SPACING = 8;
    private static final int STRATEGY_ROW_GAP = 4;
    private static final int TOOLBAR_GAP = 2;
    private static final int GRID_H_GAP = 12;
    private static final int GRID_V_GAP = 8;
    private static final int FIELD_WIDTH = 140;

    private final BacktestService backtestService;
    private final ScriptRepository strategyRepository;
    private final ScripRepository scripRepository;
    private final ComboBox<ScriptDescriptor> strategyCombo;
    private final Button deleteButton;
    private final Button runButton;
    private final Button cancelButton;
    private ScripTypeFilterCombo scripTypeCombo;
    private ExchangeFilterCombo exchangeCombo;
    private ComboBox<Timeframe> timeframeCombo;
    private Spinner<Integer> offsetSpinner;
    private TextField capitalField;
    private TextField costField;
    private TextField slippageField;
    private TextField volumeParticipationField;
    private CheckBox generateFeaturesCheckBox;
    private final List<TextField> inputFields = new ArrayList<>();
    private List<StrategyInput> discoveredInputs = List.of();
    private GridPane inputsGrid;
    private VBox inputsWrapper;
    private BiConsumer<ScriptDescriptor, BacktestResultPane> onBacktestStarted;
    private BacktestResultPane activeResultPane;

    public BacktestPane(ScripRepository scripRepository, BarsRepository barsRepository,
                        ScriptRepository strategyRepository) {
        this.backtestService = new BacktestService(scripRepository, barsRepository);
        this.strategyRepository = strategyRepository;
        this.scripRepository = scripRepository;
        this.strategyCombo = buildStrategyCombo();
        this.deleteButton = buildDeleteButton();
        this.runButton = buildRunButton();
        this.cancelButton = buildCancelButton();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        refreshStrategyInputs();
    }

    public void setOnBacktestStarted(BiConsumer<ScriptDescriptor, BacktestResultPane> handler) {
        this.onBacktestStarted = handler;
    }

    public void refreshStrategies() {
        ScriptDescriptor selected = strategyCombo.getValue();
        String selectedName = selected != null ? selected.getName() : null;
        strategyCombo.getItems().setAll(strategyRepository.findAll());
        if (selectedName != null) {
            strategyCombo.getItems().stream()
                    .filter(s -> s.getName().equals(selectedName))
                    .findFirst()
                    .ifPresent(strategyCombo::setValue);
        }
        if (strategyCombo.getValue() == null && !strategyCombo.getItems().isEmpty()) {
            strategyCombo.setValue(strategyCombo.getItems().getFirst());
        }
        refreshStrategyInputs();
    }

    public void updateRenamedStrategy(ScriptDescriptor oldStrategy, ScriptDescriptor newStrategy) {
        int index = strategyCombo.getItems().indexOf(oldStrategy);
        if (index >= 0) {
            strategyCombo.getItems().set(index, newStrategy);
            if (oldStrategy.equals(strategyCombo.getValue())) {
                strategyCombo.setValue(newStrategy);
            }
        }
    }

    private void buildLayout() {
        VBox topSection = buildTopSection();
        TabPane configTabs = buildConfigTabs();
        VBox.setVgrow(configTabs, Priority.ALWAYS);
        HBox buttonRow = buildButtonRow();
        getChildren().addAll(topSection, configTabs, buttonRow);
    }

    private VBox buildTopSection() {
        HBox strategyRow = new HBox(STRATEGY_ROW_GAP, strategyCombo, deleteButton);
        strategyRow.setAlignment(Pos.CENTER);
        strategyRow.getStyleClass().add(STRATEGY_ROW_STYLE);
        HBox.setHgrow(strategyCombo, Priority.ALWAYS);
        VBox section = new VBox(SECTION_SPACING, strategyRow);
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    private HBox buildButtonRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(BUTTON_SPACING, runButton, spacer, cancelButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 10, 10, 10));
        return row;
    }

    private TabPane buildConfigTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add(TAB_PANE_STYLE);
        Tab backtestTab = new Tab(TAB_CONFIG, buildBacktestConfigContent());
        Tab inputsTab = new Tab(TAB_INPUTS, buildStrategyInputsContent());
        tabPane.getTabs().addAll(backtestTab, inputsTab);
        return tabPane;
    }

    private ScrollPane buildBacktestConfigContent() {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_H_GAP);
        grid.setVgap(GRID_V_GAP);
        grid.setPadding(new Insets(GRID_V_GAP, 10, 0, 10));
        int row = 0;
        grid.add(createConfigLabel(EXCHANGE_LABEL), 0, row);
        exchangeCombo = buildExchangeCombo();
        grid.add(exchangeCombo, 1, row++);
        grid.add(createConfigLabel(SCRIP_TYPE_LABEL), 0, row);
        scripTypeCombo = buildScripTypeCombo();
        grid.add(scripTypeCombo, 1, row++);
        grid.add(createConfigLabel(TIMEFRAME_LABEL), 0, row);
        timeframeCombo = buildTimeframeCombo();
        grid.add(timeframeCombo, 1, row++);
        grid.add(createConfigLabel(OFFSET_LABEL), 0, row);
        offsetSpinner = buildOffsetSpinner();
        grid.add(offsetSpinner, 1, row++);
        grid.add(createConfigLabel(CAPITAL_LABEL), 0, row);
        capitalField = buildConfigField(String.valueOf(DEFAULT_INITIAL_CAPITAL));
        grid.add(capitalField, 1, row++);
        grid.add(createConfigLabel(COST_LABEL), 0, row);
        costField = buildConfigField(String.valueOf(DEFAULT_COST_PERCENT));
        grid.add(costField, 1, row++);
        grid.add(createConfigLabel(SLIPPAGE_LABEL), 0, row);
        slippageField = buildConfigField(String.valueOf(DEFAULT_SLIPPAGE_PERCENT));
        grid.add(slippageField, 1, row++);
        grid.add(createConfigLabel(VOLUME_PARTICIPATION_LABEL), 0, row);
        volumeParticipationField = buildConfigField(String.valueOf(DEFAULT_VOLUME_PARTICIPATION_PERCENT));
        grid.add(volumeParticipationField, 1, row++);
        generateFeaturesCheckBox = new CheckBox(GENERATE_FEATURES_LABEL);
        generateFeaturesCheckBox.getStyleClass().add(CHECKBOX_STYLE);
        grid.add(generateFeaturesCheckBox, 0, row, 2, 1);
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("backtest-config-scroll");
        return scroll;
    }

    private ScrollPane buildStrategyInputsContent() {
        inputsWrapper = new VBox();
        inputsWrapper.setPadding(new Insets(GRID_V_GAP, 10, 0, 10));
        ScrollPane scroll = new ScrollPane(inputsWrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("backtest-config-scroll");
        return scroll;
    }

    private void refreshStrategyInputs() {
        inputFields.clear();
        discoveredInputs = discoverInputs();
        inputsWrapper.getChildren().clear();
        if (discoveredInputs.isEmpty()) {
            Label noInputs = new Label(NO_INPUTS_TEXT);
            noInputs.getStyleClass().add(CONFIG_LABEL_STYLE);
            inputsWrapper.getChildren().add(noInputs);
            return;
        }
        inputsGrid = new GridPane();
        inputsGrid.setHgap(GRID_H_GAP);
        inputsGrid.setVgap(GRID_V_GAP);
        for (int i = 0; i < discoveredInputs.size(); i++) {
            StrategyInput input = discoveredInputs.get(i);
            VBox labelBox = buildInputLabel(input);
            inputsGrid.add(labelBox, 0, i);
            Number currentValue = input.getDefaultValue();
            TextField field = buildConfigField(formatInputValue(currentValue));
            inputFields.add(field);
            inputsGrid.add(field, 1, i);
        }
        inputsWrapper.getChildren().add(inputsGrid);
    }

    private VBox buildInputLabel(StrategyInput input) {
        Label nameLabel = createConfigLabel(input.getName());
        VBox box = new VBox(nameLabel);
        String hint = buildRangeHint(input);
        if (hint != null) {
            Label rangeLabel = new Label(hint);
            rangeLabel.getStyleClass().add(RANGE_HINT_STYLE);
            box.getChildren().add(rangeLabel);
        }
        return box;
    }

    private String buildRangeHint(StrategyInput input) {
        boolean hasStep = input.getStep().doubleValue() != DEFAULT_STEP_VALUE;
        if (input.hasMin() && input.hasMax() && hasStep) {
            return String.format(RANGE_FORMAT_MIN_MAX_STEP,
                    formatInputValue(input.getMinValue()), formatInputValue(input.getMaxValue()),
                    formatInputValue(input.getStep()));
        }
        if (input.hasMin() && input.hasMax()) {
            return String.format(RANGE_FORMAT_MIN_MAX,
                    formatInputValue(input.getMinValue()), formatInputValue(input.getMaxValue()));
        }
        if (input.hasMin()) {
            return String.format(RANGE_FORMAT_MIN, formatInputValue(input.getMinValue()));
        }
        if (input.hasMax()) {
            return String.format(RANGE_FORMAT_MAX, formatInputValue(input.getMaxValue()));
        }
        if (hasStep) {
            return String.format(RANGE_FORMAT_STEP, formatInputValue(input.getStep()));
        }
        return null;
    }

    private ComboBox<ScriptDescriptor> buildStrategyCombo() {
        ComboBox<ScriptDescriptor> combo = new ComboBox<>();
        combo.getItems().addAll(strategyRepository.findAll());
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setCellFactory(lv -> new ScriptDescriptorCell());
        combo.setButtonCell(new ScriptDescriptorCell());
        combo.valueProperty().addListener((obs, oldVal, newVal) -> refreshStrategyInputs());
        MenuItem renameItem = new MenuItem(RENAME_MENU_TEXT);
        renameItem.setOnAction(e -> renameStrategy());
        combo.setContextMenu(new ContextMenu(renameItem));
        return combo;
    }

    private Button buildDeleteButton() {
        return buildToolbarButton(FluentUiRegularAL.DELETE_16, e -> deleteStrategy());
    }

    private Button buildToolbarButton(Ikon ikon, EventHandler<ActionEvent> handler) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TOOLBAR_ICON_SIZE);
        icon.getStyleClass().add(TOOLBAR_ICON_STYLE);
        Button button = new Button(null, icon);
        button.getStyleClass().add(TOOLBAR_BUTTON_STYLE);
        button.setOnAction(handler);
        return button;
    }

    private Button buildRunButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.PLAY_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(BUTTON_ICON_STYLE);
        Button button = new Button(RUN_LABEL, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> startBacktest());
        return button;
    }

    private Button buildCancelButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(CANCEL_ICON_SIZE);
        icon.getStyleClass().add(CANCEL_ICON_STYLE);
        Button button = new Button(CANCEL_LABEL, icon);
        button.getStyleClass().add(CANCEL_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> cancelBacktest());
        return button;
    }

    private Label createConfigLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(CONFIG_LABEL_STYLE);
        return label;
    }

    private TextField buildConfigField(String initial) {
        TextField field = new TextField(initial);
        field.getStyleClass().add(CONFIG_FIELD_STYLE);
        field.setPrefWidth(FIELD_WIDTH);
        return field;
    }

    private Spinner<Integer> buildOffsetSpinner() {
        Spinner<Integer> spinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_OFFSET, MAX_OFFSET, DEFAULT_OFFSET));
        spinner.setEditable(true);
        spinner.getStyleClass().add(CONFIG_SPINNER_STYLE);
        spinner.setPrefWidth(FIELD_WIDTH);
        return spinner;
    }

    private ScripTypeFilterCombo buildScripTypeCombo() {
        ScripTypeFilterCombo combo = new ScripTypeFilterCombo(false);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        combo.setPrefWidth(FIELD_WIDTH);
        return combo;
    }

    private ExchangeFilterCombo buildExchangeCombo() {
        ExchangeFilterCombo combo = new ExchangeFilterCombo(true);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        combo.setPrefWidth(FIELD_WIDTH);
        return combo;
    }

    private ComboBox<Timeframe> buildTimeframeCombo() {
        ComboBox<Timeframe> combo = new ComboBox<>();
        combo.getItems().addAll(Timeframe.values());
        combo.setValue(Timeframe.DAILY);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        combo.setPrefWidth(FIELD_WIDTH);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Timeframe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayLabel());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Timeframe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayLabel());
            }
        });
        return combo;
    }

    private void deleteStrategy() {
        ScriptDescriptor strategy = strategyCombo.getValue();
        if (strategy == null) return;
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, strategy.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            try {
                strategyRepository.delete(strategy);
                strategyCombo.getItems().remove(strategy);
                if (!strategyCombo.getItems().isEmpty()) {
                    strategyCombo.setValue(strategyCombo.getItems().getFirst());
                } else {
                    strategyCombo.setValue(null);
                }
            } catch (IOException ex) {
                log.error("Failed to delete strategy: {}", ex.getMessage());
            }
        }
    }

    private void renameStrategy() {
        ScriptDescriptor strategy = strategyCombo.getValue();
        if (strategy == null) return;
        StrategyNameDialog dialog = new StrategyNameDialog(RENAME_STRATEGY_TITLE, strategy.getName());
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed() && !dialog.getName().equals(strategy.getName())) {
            try {
                ScriptDescriptor renamed = strategyRepository.rename(strategy, dialog.getName());
                int index = strategyCombo.getItems().indexOf(strategy);
                strategyCombo.getItems().set(index, renamed);
                strategyCombo.setValue(renamed);
            } catch (IOException ex) {
                log.error("Failed to rename strategy: {}", ex.getMessage());
            }
        }
    }

    private List<StrategyInput> discoverInputs() {
        ScriptDescriptor strategy = strategyCombo.getValue();
        if (strategy == null) return List.of();
        try {
            String script = strategyRepository.loadScript(strategy);
            return StrategyInputDiscoverer.discover(script);
        } catch (Exception e) {
            log.debug("Failed to discover strategy inputs: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Number> collectStrategyInputs() {
        Map<String, Number> result = new LinkedHashMap<>();
        for (int i = 0; i < discoveredInputs.size(); i++) {
            StrategyInput input = discoveredInputs.get(i);
            Number value = parseNumber(inputFields.get(i), input.getDefaultValue());
            value = clampToRange(value, input);
            result.put(input.getName(), value);
        }
        return result;
    }

    private void startBacktest() {
        ScriptDescriptor strategy = strategyCombo.getValue();
        if (strategy == null) return;
        Map<String, Number> strategyInputs = collectStrategyInputs();
        BacktestConfig config = BacktestConfig.builder()
                .scripType(scripTypeCombo.getValue())
                .exchange(exchangeCombo.getValue())
                .timeframe(timeframeCombo.getValue())
                .offset(offsetSpinner.getValue())
                .initialCapital(parseFloat(capitalField, DEFAULT_INITIAL_CAPITAL))
                .costPercent(parseFloat(costField, DEFAULT_COST_PERCENT))
                .slippagePercent(parseFloat(slippageField, DEFAULT_SLIPPAGE_PERCENT))
                .volumeParticipationPercent(parseFloat(volumeParticipationField, DEFAULT_VOLUME_PARTICIPATION_PERCENT))
                .strategyInputs(Map.copyOf(strategyInputs))
                .build();
        activeResultPane = new BacktestResultPane(scripRepository, config);
        if (onBacktestStarted != null) {
            onBacktestStarted.accept(strategy, activeResultPane);
        }
        boolean generateFeatures = generateFeaturesCheckBox.isSelected();
        setRunning(true);
        Thread thread = new Thread(() -> runBacktest(strategy, config, generateFeatures));
        thread.setDaemon(true);
        thread.setName("backtest-thread");
        thread.start();
    }

    private void runBacktest(ScriptDescriptor strategy, BacktestConfig config, boolean generateFeatures) {
        FeatureCollector collector = null;
        try {
            String scriptSource = strategyRepository.loadScript(strategy);
            if (generateFeatures) {
                collector = createFeatureCollector(strategy);
            }
            List<ScripResult> results = new ArrayList<>();
            backtestService.run(scriptSource, config, new BacktestListener() {
                @Override
                public void onProgress(int completed, int total) {
                    Platform.runLater(() -> activeResultPane.updateProgress(completed, total));
                }

                @Override
                public void onScripCompleted(ScripResult result) {
                    results.add(result);
                }

                @Override
                public void onError(Scrip scrip, String error) {
                    log.debug("Backtest error for {}: {}", scrip.getSymbol(), error);
                }
            }, collector);
            BacktestReport report = MetricsCalculator.compute(results, config);
            log.info("{}", report);
            Platform.runLater(() -> activeResultPane.displayReport(report));
        } catch (ScriptCompilationException e) {
            log.error("Strategy compilation failed: {}", e.getDetail());
            Platform.runLater(() -> activeResultPane.displayError(e.getDetail()));
        } catch (IOException e) {
            log.error("Failed to load strategy script: {}", e.getMessage());
            Platform.runLater(() -> activeResultPane.displayError(e.getMessage()));
        } finally {
            if (collector != null) {
                collector.close();
            }
            Platform.runLater(this::onFinished);
        }
    }

    private FeatureCollector createFeatureCollector(ScriptDescriptor strategy) throws IOException {
        String sanitizedName = strategy.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        long timestamp = System.currentTimeMillis();
        String fileName = sanitizedName + "_" + timestamp + FEATURES_FILE_EXTENSION;
        Path outputPath = Path.of(FEATURES_DIR, fileName);
        CsvFeatureConsumer csvConsumer = new CsvFeatureConsumer(outputPath);
        log.info("Feature output: {}", outputPath.toAbsolutePath());
        return new FeatureCollector(csvConsumer);
    }

    public BacktestResultPane getActiveResultPane() {
        return activeResultPane;
    }

    public Timeframe getActiveTimeframe() {
        return timeframeCombo.getValue();
    }

    private void cancelBacktest() {
        backtestService.cancel();
    }

    private void onFinished() {
        setRunning(false);
    }

    private void setRunning(boolean running) {
        strategyCombo.setDisable(running);
        deleteButton.setDisable(running);
        runButton.setDisable(running);
        cancelButton.setDisable(!running);
        generateFeaturesCheckBox.setDisable(running);
    }

    private static float parseFloat(TextField field, float defaultValue) {
        try {
            return Float.parseFloat(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Number parseNumber(TextField field, Number defaultValue) {
        try {
            String text = field.getText().trim();
            if (text.contains(".")) return Double.parseDouble(text);
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Number clampToRange(Number value, StrategyInput input) {
        double v = value.doubleValue();
        double step = input.getStep().doubleValue();
        if (input.hasMin()) {
            double base = input.getMinValue().doubleValue();
            v = base + Math.round((v - base) / step) * step;
        }
        if (input.hasMin() && v < input.getMinValue().doubleValue()) v = input.getMinValue().doubleValue();
        if (input.hasMax() && v > input.getMaxValue().doubleValue()) v = input.getMaxValue().doubleValue();
        if (value instanceof Integer) return (int) v;
        return v;
    }

    private static String formatInputValue(Number value) {
        if (value instanceof Integer) return String.valueOf(value.intValue());
        if (value instanceof Long) return String.valueOf(value.longValue());
        double d = value.doubleValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private static final class ScriptDescriptorCell extends ListCell<ScriptDescriptor> {

        @Override
        protected void updateItem(ScriptDescriptor descriptor, boolean empty) {
            super.updateItem(descriptor, empty);
            setText(empty || descriptor == null ? null : descriptor.getName());
        }

    }

}
