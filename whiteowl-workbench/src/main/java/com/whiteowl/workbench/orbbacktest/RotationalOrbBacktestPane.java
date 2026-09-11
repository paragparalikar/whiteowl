package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.rotational.RotationalBacktestEngine;
import com.whiteowl.core.backtest.rotational.RotationalBacktestReport;
import com.whiteowl.core.backtest.rotational.RotationalBacktestResult;
import com.whiteowl.core.backtest.driver.RotationalOrbDriver;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left-side configuration pane for running a single Rotational ORB backtest.
 */
@Slf4j
public final class RotationalOrbBacktestPane extends VBox {

    // ── Style constants ──────────────────────────────────────────────
    private static final String PANE_STYLE = "rot-orb-pane";
    private static final String SCROLL_STYLE = "rot-orb-scroll";
    private static final String SECTION_STYLE = "rot-orb-section";
    private static final String SECTION_LABEL_STYLE = "rot-orb-section-label";
    private static final String CONFIG_LABEL_STYLE = "rot-orb-config-label";
    private static final String CONFIG_FIELD_STYLE = "rot-orb-config-field";
    private static final String CONFIG_COMBO_STYLE = "rot-orb-config-combo";
    private static final String CHECKBOX_STYLE = "rot-orb-checkbox";
    private static final String BUTTON_STYLE = "rot-orb-button";
    private static final String BUTTON_ICON_STYLE = "rot-orb-button-icon";
    private static final String STOP_BUTTON_STYLE = "rot-orb-stop-button";
    private static final String STOP_ICON_STYLE = "rot-orb-stop-icon";
    private static final String PROGRESS_CONTAINER_STYLE = "rot-orb-progress-container";
    private static final String PROGRESS_BAR_STYLE = "rot-orb-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "rot-orb-progress-label";
    private static final String MESSAGE_LABEL_STYLE = "rot-orb-message-label";

    // ── Section headers ──────────────────────────────────────────────
    private static final String UNIVERSE_HEADER = "Universe";
    private static final String OPENING_RANGE_HEADER = "Opening Range";
    private static final String ENTRY_FILTERS_HEADER = "Entry Filters";
    private static final String ENTRY_METHOD_HEADER = "Entry Method";
    private static final String PICKS_RANKING_HEADER = "Picks & Ranking";
    private static final String TIMING_HEADER = "Timing";
    private static final String STOP_LOSS_HEADER = "Stop Loss";
    private static final String TRAILING_STOP_HEADER = "Trailing Stop";
    private static final String TARGET_HEADER = "Target";
    private static final String POSITION_SIZING_HEADER = "Position Sizing";
    private static final String RUN_LABEL = "Run Backtest";
    private static final String STOP_LABEL = "Cancel";
    private static final String LOADING_MESSAGE = "Loading data...";
    private static final String RUNNING_MESSAGE = "Running backtest...";
    private static final String COMPLETED_MESSAGE = "Completed";
    private static final String CANCELLED_MESSAGE = "Cancelled";
    private static final String FAILED_MESSAGE = "Backtest failed";

    // ── Layout constants ─────────────────────────────────────────────
    private static final int SECTION_SPACING = 4;
    private static final int GRID_H_GAP = 6;
    private static final int GRID_V_GAP = 3;
    private static final int FIELD_WIDTH = 55;
    private static final int ICON_SIZE = 14;
    private static final int STOP_ICON_SIZE = 12;
    private static final int BUTTON_SPACING = 8;

    // ── Dependencies ─────────────────────────────────────────────────
    private final BarsRepository barsRepository;
    private final GroupRepository groupRepository;

    // ── Config fields ────────────────────────────────────────────────
    private final ComboBox<String> universeCombo;
    private final TextField openingRangeMinutesField;
    private final TextField barMinutesField;
    private final TextField minGapAtrField;
    private final TextField maxGapAtrField;
    private final ComboBox<RotationalBacktestConfig.Side> sideCombo;
    private final TextField minOrAtrField;
    private final TextField maxOrAtrField;
    private final TextField minOrbRvolField;
    private final TextField maxOrbRvolField;
    private final TextField minRsRankField;
    private final TextField maxRsRankField;
    private final TextField minOrbIbsField;
    private final TextField maxOrbIbsField;
    private final ComboBox<RotationalBacktestConfig.EntryMethod> entryMethodCombo;
    private final TextField maxReEntriesField;
    private final TextField picksField;
    private final ComboBox<RotationalBacktestConfig.RankerType> rankerTypeCombo;
    private final TextField entryCutoffField;
    private final TextField exitTimeField;
    private final ComboBox<RotationalBacktestConfig.StopBasis> stopBasisCombo;
    private final TextField stopMultiplierField;
    private final CheckBox trailingStopEnabledCheck;
    private final ComboBox<RotationalBacktestConfig.StopBasis> trailingStopBasisCombo;
    private final TextField trailingStopMultiplierField;
    private final CheckBox targetEnabledCheck;
    private final ComboBox<RotationalBacktestConfig.TargetBasis> targetBasisCombo;
    private final TextField targetMultiplierField;
    private final TextField initialCapitalField;
    private final TextField slippageField;
    private final CheckBox atrScalingCheck;
    // ── Controls ─────────────────────────────────────────────────────
    private final Button runButton;
    private final Button stopButton;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label messageLabel;
    private final VBox progressContainer;

    // ── State ────────────────────────────────────────────────────────
    private volatile boolean cancelRequested;
    private Consumer<RotationalBacktestReport> onBacktestComplete;

    public RotationalOrbBacktestPane(BarsRepository barsRepository, GroupRepository groupRepository) {
        this.barsRepository = barsRepository;
        this.groupRepository = groupRepository;

        this.universeCombo = buildUniverseCombo();
        this.openingRangeMinutesField = buildConfigField("5");
        this.barMinutesField = buildConfigField("5");
        this.minGapAtrField = buildConfigField("0.35");
        this.maxGapAtrField = buildConfigField("");
        this.sideCombo = buildEnumCombo(RotationalBacktestConfig.Side.values(),
                RotationalBacktestConfig.Side.LONG);
        this.minOrAtrField = buildConfigField("");
        this.maxOrAtrField = buildConfigField("");
        this.minOrbRvolField = buildConfigField("");
        this.maxOrbRvolField = buildConfigField("");
        this.minRsRankField = buildConfigField("");
        this.maxRsRankField = buildConfigField("");
        this.minOrbIbsField = buildConfigField("");
        this.maxOrbIbsField = buildConfigField("0.80");
        this.entryMethodCombo = buildEnumCombo(RotationalBacktestConfig.EntryMethod.values(),
                RotationalBacktestConfig.EntryMethod.BREAKOUT);
        this.maxReEntriesField = buildConfigField("1");
        this.picksField = buildConfigField("4");
        this.rankerTypeCombo = buildEnumCombo(RotationalBacktestConfig.RankerType.values(),
                RotationalBacktestConfig.RankerType.RS_RVOL);
        this.entryCutoffField = buildConfigField("10:30");
        this.exitTimeField = buildConfigField("15:20");
        this.stopBasisCombo = buildEnumCombo(RotationalBacktestConfig.StopBasis.values(),
                RotationalBacktestConfig.StopBasis.OR_RANGE);
        this.stopMultiplierField = buildConfigField("0.70");
        this.trailingStopEnabledCheck = buildCheckBox("Enabled", true);
        this.trailingStopBasisCombo = buildEnumCombo(RotationalBacktestConfig.StopBasis.values(),
                RotationalBacktestConfig.StopBasis.ATR);
        this.trailingStopMultiplierField = buildConfigField("1.50");
        this.targetEnabledCheck = buildCheckBox("Enabled", true);
        this.targetBasisCombo = buildEnumCombo(RotationalBacktestConfig.TargetBasis.values(),
                RotationalBacktestConfig.TargetBasis.STOP_DISTANCE);
        this.targetMultiplierField = buildConfigField("5.25");
        this.initialCapitalField = buildConfigField("1000000");
        this.slippageField = buildConfigField("0.002");
        this.atrScalingCheck = buildCheckBox("ATR Scaling", false);
        this.runButton = buildRunButton();
        this.stopButton = buildStopButton();
        this.progressBar = buildProgressBar();
        this.progressLabel = buildProgressLabel();
        this.messageLabel = buildMessageLabel();
        this.progressContainer = buildProgressContainer();

        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    public void setOnBacktestComplete(Consumer<RotationalBacktestReport> handler) {
        this.onBacktestComplete = handler;
    }

    // ── Layout ───────────────────────────────────────────────────────

    private void buildLayout() {
        VBox content = new VBox(
                buildSection(UNIVERSE_HEADER, buildUniverseSection()),
                new Separator(),
                buildSection(OPENING_RANGE_HEADER, buildOpeningRangeSection()),
                new Separator(),
                buildSection(ENTRY_FILTERS_HEADER, buildEntryFiltersSection()),
                new Separator(),
                buildSection(ENTRY_METHOD_HEADER, buildEntryMethodSection()),
                new Separator(),
                buildSection(PICKS_RANKING_HEADER, buildPicksRankingSection()),
                new Separator(),
                buildSection(TIMING_HEADER, buildTimingSection()),
                new Separator(),
                buildSection(STOP_LOSS_HEADER, buildStopLossSection()),
                new Separator(),
                buildSection(TRAILING_STOP_HEADER, buildTrailingStopSection()),
                new Separator(),
                buildSection(TARGET_HEADER, buildTargetSection()),
                new Separator(),
                buildSection(POSITION_SIZING_HEADER, buildPositionSizingSection()),
                buildButtonRow(),
                progressContainer
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add(SCROLL_STYLE);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private VBox buildSection(String header, VBox body) {
        Label label = createSectionLabel(header);
        VBox section = new VBox(SECTION_SPACING, label);
        section.getChildren().addAll(body.getChildren());
        section.getStyleClass().add(SECTION_STYLE);
        return section;
    }

    // ── Config sections ──────────────────────────────────────────────

    private VBox buildUniverseSection() {
        VBox box = new VBox(SECTION_SPACING);
        box.getChildren().addAll(createConfigLabel("Group"), universeCombo);
        return box;
    }

    private VBox buildOpeningRangeSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("OR Min"), 0, row);
        grid.add(openingRangeMinutesField, 1, row);
        grid.add(createConfigLabel("Bar Min"), 2, row);
        grid.add(barMinutesField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildEntryFiltersSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Gap/ATR"), 0, row);
        grid.add(minGapAtrField, 1, row);
        grid.add(createConfigLabel("to"), 2, row);
        grid.add(maxGapAtrField, 3, row++);
        grid.add(createConfigLabel("OR/ATR"), 0, row);
        grid.add(minOrAtrField, 1, row);
        grid.add(createConfigLabel("to"), 2, row);
        grid.add(maxOrAtrField, 3, row++);
        grid.add(createConfigLabel("RVOL"), 0, row);
        grid.add(minOrbRvolField, 1, row);
        grid.add(createConfigLabel("to"), 2, row);
        grid.add(maxOrbRvolField, 3, row++);
        grid.add(createConfigLabel("RS Rank"), 0, row);
        grid.add(minRsRankField, 1, row);
        grid.add(createConfigLabel("to"), 2, row);
        grid.add(maxRsRankField, 3, row++);
        grid.add(createConfigLabel("IBS"), 0, row);
        grid.add(minOrbIbsField, 1, row);
        grid.add(createConfigLabel("to"), 2, row);
        grid.add(maxOrbIbsField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildEntryMethodSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Method"), 0, row);
        grid.add(entryMethodCombo, 1, row);
        grid.add(createConfigLabel("Re-entries"), 2, row);
        grid.add(maxReEntriesField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildPicksRankingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Side"), 0, row);
        grid.add(sideCombo, 1, row);
        grid.add(createConfigLabel("Picks"), 2, row);
        grid.add(picksField, 3, row++);
        grid.add(createConfigLabel("Ranker"), 0, row);
        grid.add(rankerTypeCombo, 1, row, 3, 1);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTimingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Cutoff"), 0, row);
        grid.add(entryCutoffField, 1, row);
        grid.add(createConfigLabel("Exit"), 2, row);
        grid.add(exitTimeField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildStopLossSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Basis"), 0, row);
        grid.add(stopBasisCombo, 1, row);
        grid.add(createConfigLabel("Mult"), 2, row);
        grid.add(stopMultiplierField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTrailingStopSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(trailingStopEnabledCheck, 0, row);
        grid.add(trailingStopBasisCombo, 1, row);
        grid.add(createConfigLabel("Mult"), 2, row);
        grid.add(trailingStopMultiplierField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTargetSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(targetEnabledCheck, 0, row);
        grid.add(targetBasisCombo, 1, row);
        grid.add(createConfigLabel("Mult"), 2, row);
        grid.add(targetMultiplierField, 3, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildPositionSizingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Capital"), 0, row);
        grid.add(initialCapitalField, 1, row);
        grid.add(createConfigLabel("Slippage"), 2, row);
        grid.add(slippageField, 3, row++);
        grid.add(atrScalingCheck, 0, row, 4, 1);
        return new VBox(SECTION_SPACING, grid);
    }

    private HBox buildButtonRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(BUTTON_SPACING, runButton, spacer, stopButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        return row;
    }

    // ── Widget builders ──────────────────────────────────────────────

    private ComboBox<String> buildUniverseCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        try {
            List<Group> groups = groupRepository.loadAll();
            for (Group g : groups) combo.getItems().add(g.getName());
            if (!combo.getItems().isEmpty()) {
                if (combo.getItems().contains("ORB Universe")) {
                    combo.setValue("ORB Universe");
                } else {
                    combo.setValue(combo.getItems().getFirst());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load groups: {}", e.getMessage());
        }
        return combo;
    }

    private <E extends Enum<E>> ComboBox<E> buildEnumCombo(E[] values, E defaultValue) {
        ComboBox<E> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.setValue(defaultValue);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        return combo;
    }

    private TextField buildConfigField(String initial) {
        TextField field = new TextField(initial);
        field.getStyleClass().add(CONFIG_FIELD_STYLE);
        field.setPrefWidth(FIELD_WIDTH);
        return field;
    }

    private CheckBox buildCheckBox(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.getStyleClass().add(CHECKBOX_STYLE);
        return cb;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(SECTION_LABEL_STYLE);
        return label;
    }

    private Label createConfigLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(CONFIG_LABEL_STYLE);
        return label;
    }

    private GridPane createConfigGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_H_GAP);
        grid.setVgap(GRID_V_GAP);
        javafx.scene.layout.ColumnConstraints lc1 = new javafx.scene.layout.ColumnConstraints();
        lc1.setHgrow(Priority.NEVER);
        javafx.scene.layout.ColumnConstraints fc1 = new javafx.scene.layout.ColumnConstraints();
        fc1.setHgrow(Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints lc2 = new javafx.scene.layout.ColumnConstraints();
        lc2.setHgrow(Priority.NEVER);
        javafx.scene.layout.ColumnConstraints fc2 = new javafx.scene.layout.ColumnConstraints();
        fc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(lc1, fc1, lc2, fc2);
        return grid;
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

    private Button buildStopButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(STOP_ICON_SIZE);
        icon.getStyleClass().add(STOP_ICON_STYLE);
        Button button = new Button(STOP_LABEL, icon);
        button.getStyleClass().add(STOP_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> cancelBacktest());
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
        Label label = new Label();
        label.getStyleClass().add(MESSAGE_LABEL_STYLE);
        label.setWrapText(true);
        return label;
    }

    private VBox buildProgressContainer() {
        VBox container = new VBox(4, messageLabel, progressBar, progressLabel);
        container.getStyleClass().add(PROGRESS_CONTAINER_STYLE);
        container.setVisible(false);
        container.setManaged(false);
        return container;
    }

    // ── Config building ──────────────────────────────────────────────

    private RotationalBacktestConfig buildConfigFromFields() {
        RotationalBacktestConfig.RotationalBacktestConfigBuilder builder = RotationalBacktestConfig.builder();

        builder.universeGroupName(universeCombo.getValue());
        builder.openingRangeMinutes(parseInt(openingRangeMinutesField, 15));
        builder.barMinutes(parseInt(barMinutesField, 5));

        builder.minGapAtr(parseNullableDouble(minGapAtrField));
        builder.maxGapAtr(parseNullableDouble(maxGapAtrField));
        builder.minOrAtr(parseNullableDouble(minOrAtrField));
        builder.maxOrAtr(parseNullableDouble(maxOrAtrField));
        builder.minOrbRvol(parseNullableDouble(minOrbRvolField));
        builder.maxOrbRvol(parseNullableDouble(maxOrbRvolField));
        builder.minRsRank(parseNullableDouble(minRsRankField));
        builder.maxRsRank(parseNullableDouble(maxRsRankField));
        builder.minOrbIbs(parseNullableDouble(minOrbIbsField));
        builder.maxOrbIbs(parseNullableDouble(maxOrbIbsField));

        builder.entryMethod(entryMethodCombo.getValue());
        builder.maxReEntries(parseInt(maxReEntriesField, 0));
        builder.side(sideCombo.getValue());
        builder.picks(parseInt(picksField, 5));
        builder.rankerType(rankerTypeCombo.getValue());

        String cutoff = entryCutoffField.getText().trim();
        if (!cutoff.isEmpty()) builder.entryCutoffTime(LocalTime.parse(cutoff));
        String exit = exitTimeField.getText().trim();
        builder.exitTime(exit.isEmpty() ? null : LocalTime.parse(exit));

        builder.stopBasis(stopBasisCombo.getValue());
        builder.stopMultiplier(parseDouble(stopMultiplierField, 1.0));
        builder.trailingStopEnabled(trailingStopEnabledCheck.isSelected());
        builder.trailingStopBasis(trailingStopBasisCombo.getValue());
        builder.trailingStopMultiplier(parseDouble(trailingStopMultiplierField, 1.0));
        builder.targetEnabled(targetEnabledCheck.isSelected());
        builder.targetBasis(targetBasisCombo.getValue());
        builder.targetMultiplier(parseDouble(targetMultiplierField, 3.0));

        builder.initialCapital(parseDouble(initialCapitalField, 1_000_000));
        builder.slippage(parseDouble(slippageField, 0.001));
        builder.atrScaling(atrScalingCheck.isSelected());

        return builder.build();
    }

    // ── Run / Cancel ─────────────────────────────────────────────────

    private void startBacktest() {
        RotationalBacktestConfig config;
        try {
            config = buildConfigFromFields();
            config.validate();
        } catch (Exception e) {
            messageLabel.setText("Config error: " + e.getMessage());
            showProgress(true);
            return;
        }

        setRunning(true);
        showProgress(true);
        progressBar.setProgress(-1);
        progressLabel.setText("");
        messageLabel.setText(LOADING_MESSAGE);
        cancelRequested = false;

        Thread thread = new Thread(() -> runBacktest(config));
        thread.setDaemon(true);
        thread.setName("rot-orb-backtest-thread");
        thread.start();
    }

    private void runBacktest(RotationalBacktestConfig config) {
        try {
            // Load data
            String groupName = config.getUniverseGroupName();
            List<String> scripIds = RotationalOrbDriver.loadGroupScrips(groupName);

            Platform.runLater(() -> progressLabel.setText("Loading " + scripIds.size() + " scrips..."));

            Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                    barsRepository, scripIds, Timeframe.FIVE_MINUTE);
            Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                    barsRepository, scripIds, Timeframe.DAILY);

            if (intradayBars.isEmpty()) {
                Platform.runLater(() -> onFinished(FAILED_MESSAGE + ": no intraday data found"));
                return;
            }

            if (cancelRequested) {
                Platform.runLater(() -> onFinished(CANCELLED_MESSAGE));
                return;
            }

            Platform.runLater(() -> {
                messageLabel.setText(RUNNING_MESSAGE);
                progressBar.setProgress(-1);
                progressLabel.setText(intradayBars.size() + " scrips loaded");
            });

            // Run backtest
            long start = System.currentTimeMillis();
            RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
            long elapsed = System.currentTimeMillis() - start;

            if (cancelRequested) {
                Platform.runLater(() -> onFinished(CANCELLED_MESSAGE));
                return;
            }

            // Build report
            RotationalBacktestReport report = RotationalBacktestReport.fromResult(result, config);

            Platform.runLater(() -> {
                onFinished(String.format("%s in %.1fs — %d trades",
                        COMPLETED_MESSAGE, elapsed / 1000.0, result.getMetrics().getTotalTrades()));
                if (onBacktestComplete != null) {
                    onBacktestComplete.accept(report);
                }
            });

        } catch (Exception e) {
            log.error("Backtest failed", e);
            Platform.runLater(() -> onFinished(FAILED_MESSAGE + ": " + e.getMessage()));
        }
    }

    private void cancelBacktest() {
        cancelRequested = true;
    }

    private void onFinished(String status) {
        messageLabel.setText(status);
        progressBar.setProgress(1.0);
        setRunning(false);
    }

    private void setRunning(boolean running) {
        runButton.setDisable(running);
        stopButton.setDisable(!running);
        universeCombo.setDisable(running);
    }

    private void showProgress(boolean show) {
        progressContainer.setVisible(show);
        progressContainer.setManaged(show);
    }

    // ── Parse helpers ────────────────────────────────────────────────

    private static int parseInt(TextField field, int defaultValue) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static double parseDouble(TextField field, double defaultValue) {
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static Double parseNullableDouble(TextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try { return Double.parseDouble(text); }
        catch (NumberFormatException e) { return null; }
    }
}
