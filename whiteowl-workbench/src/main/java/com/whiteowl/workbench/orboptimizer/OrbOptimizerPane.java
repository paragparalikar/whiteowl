package com.whiteowl.workbench.orboptimizer;

import com.whiteowl.core.backtest.rotational.RotationalBacktestConfig;
import com.whiteowl.core.backtest.driver.RotationalOrbDriver;
import com.whiteowl.core.backtest.rotational.optimizer.OptimizableParameter;
import com.whiteowl.core.backtest.rotational.optimizer.OptimizationResult;
import com.whiteowl.core.backtest.rotational.optimizer.ParameterOptimizationEngine;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left-side configuration pane for Rotational ORB Parameter Optimization.
 *
 * <p>Allows the user to:
 * <ul>
 *   <li>Specify all configurable inputs for the rotational ORB backtest</li>
 *   <li>Select up to 2 parameters to optimize with min/max/step</li>
 *   <li>Run the optimization and track progress</li>
 * </ul>
 */
@Slf4j
public final class OrbOptimizerPane extends VBox {

    // ── Style constants ──────────────────────────────────────────────
    private static final String PANE_STYLE = "orb-optimizer-pane";
    private static final String SCROLL_STYLE = "orb-optimizer-scroll";
    private static final String SECTION_STYLE = "orb-optimizer-section";
    private static final String SECTION_LABEL_STYLE = "orb-optimizer-section-label";
    private static final String CONFIG_LABEL_STYLE = "orb-optimizer-config-label";
    private static final String CONFIG_FIELD_STYLE = "orb-optimizer-config-field";
    private static final String CONFIG_COMBO_STYLE = "orb-optimizer-config-combo";
    private static final String CHECKBOX_STYLE = "orb-optimizer-checkbox";
    private static final String OPT_CHECKBOX_STYLE = "orb-optimizer-opt-checkbox";
    private static final String BUTTON_STYLE = "orb-optimizer-button";
    private static final String BUTTON_ICON_STYLE = "orb-optimizer-button-icon";
    private static final String STOP_BUTTON_STYLE = "orb-optimizer-stop-button";
    private static final String STOP_ICON_STYLE = "orb-optimizer-stop-icon";
    private static final String PROGRESS_CONTAINER_STYLE = "orb-optimizer-progress-container";
    private static final String PROGRESS_BAR_STYLE = "orb-optimizer-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "orb-optimizer-progress-label";
    private static final String MESSAGE_LABEL_STYLE = "orb-optimizer-message-label";
    private static final String OPT_GRID_STYLE = "orb-optimizer-opt-grid";
    private static final String OPT_HEADER_STYLE = "orb-optimizer-opt-header";

    // ── Label constants ──────────────────────────────────────────────
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
    private static final String OPTIMIZATION_HEADER = "Parameter Optimization";
    private static final String RUN_LABEL = "Optimize";
    private static final String STOP_LABEL = "Cancel";
    private static final String READY_MESSAGE = "Ready";
    private static final String PROGRESS_FORMAT = "%d / %d configs";
    private static final String LOADING_MESSAGE = "Loading data...";
    private static final String RUNNING_MESSAGE = "Running optimization...";
    private static final String COMPLETED_MESSAGE = "Completed";
    private static final String CANCELLED_MESSAGE = "Cancelled";
    private static final String FAILED_MESSAGE = "Optimization failed";
    private static final String MAX_TWO_PARAMS_MESSAGE = "Select at most 2 parameters to optimize";

    // ── Layout constants ─────────────────────────────────────────────
    private static final int SECTION_SPACING = 6;
    private static final int GRID_H_GAP = 8;
    private static final int GRID_V_GAP = 4;
    private static final int FIELD_WIDTH = 70;
    private static final int OPT_FIELD_WIDTH = 50;
    private static final int ICON_SIZE = 14;
    private static final int STOP_ICON_SIZE = 12;
    private static final int BUTTON_SPACING = 8;

    // ── Dependencies ─────────────────────────────────────────────────
    private final BarsRepository barsRepository;
    private final GroupRepository groupRepository;

    // ── Config fields ────────────────────────────────────────────────
    private final ComboBox<String> universeCombo;

    // Opening Range
    private final TextField openingRangeMinutesField;
    private final TextField barMinutesField;

    // Entry Filters
    private final TextField minGapAtrField;
    private final TextField maxGapAtrField;
    private final TextField minOrAtrField;
    private final TextField maxOrAtrField;
    private final TextField minOrbRvolField;
    private final TextField maxOrbRvolField;
    private final TextField minRsRankField;
    private final TextField maxRsRankField;
    private final TextField minOrbIbsField;
    private final TextField maxOrbIbsField;

    // Entry Method
    private final ComboBox<RotationalBacktestConfig.EntryMethod> entryMethodCombo;
    private final TextField maxReEntriesField;

    // Picks & Ranking
    private final ComboBox<RotationalBacktestConfig.Side> sideCombo;
    private final TextField picksField;
    private final ComboBox<RotationalBacktestConfig.RankerType> rankerTypeCombo;

    // Timing
    private final TextField entryCutoffField;
    private final TextField exitTimeField;

    // Stop Loss
    private final ComboBox<RotationalBacktestConfig.StopBasis> stopBasisCombo;
    private final TextField stopMultiplierField;

    // Trailing Stop
    private final CheckBox trailingStopEnabledCheck;
    private final ComboBox<RotationalBacktestConfig.StopBasis> trailingStopBasisCombo;
    private final TextField trailingStopMultiplierField;

    // Target
    private final CheckBox targetEnabledCheck;
    private final ComboBox<RotationalBacktestConfig.TargetBasis> targetBasisCombo;
    private final TextField targetMultiplierField;

    // Position Sizing
    private final TextField initialCapitalField;
    private final TextField slippageField;
    private final CheckBox atrScalingCheck;


    // ── Optimizable parameter rows ───────────────────────────────────
    private final Map<String, OptParamRow> optParamRows = new LinkedHashMap<>();

    // ── Progress & Controls ──────────────────────────────────────────
    private final Button runButton;
    private final Button stopButton;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label messageLabel;
    private final VBox progressContainer;

    // ── State ────────────────────────────────────────────────────────
    private ParameterOptimizationEngine currentEngine;
    private Consumer<OptimizationRunResult> onOptimizationComplete;

    /**
     * Holds optimization checkbox + min/max/step fields for a single parameter.
     */
    private record OptParamRow(
            CheckBox optimizeCheck,
            TextField minField,
            TextField maxField,
            TextField stepField,
            String fieldName
    ) {}

    public OrbOptimizerPane(BarsRepository barsRepository, GroupRepository groupRepository) {
        this.barsRepository = barsRepository;
        this.groupRepository = groupRepository;

        // Initialize all config fields
        this.universeCombo = buildUniverseCombo();
        this.openingRangeMinutesField = buildConfigField("5");
        this.barMinutesField = buildConfigField("5");
        this.minGapAtrField = buildConfigField("0.50");
        this.maxGapAtrField = buildConfigField("");
        this.minOrAtrField = buildConfigField("");
        this.maxOrAtrField = buildConfigField("");
        this.minOrbRvolField = buildConfigField("");
        this.maxOrbRvolField = buildConfigField("");
        this.minRsRankField = buildConfigField("");
        this.maxRsRankField = buildConfigField("");
        this.minOrbIbsField = buildConfigField("");
        this.maxOrbIbsField = buildConfigField("0.90");
        this.entryMethodCombo = buildEnumCombo(RotationalBacktestConfig.EntryMethod.values(),
                RotationalBacktestConfig.EntryMethod.BREAKOUT);
        this.maxReEntriesField = buildConfigField("0");
        this.sideCombo = buildEnumCombo(RotationalBacktestConfig.Side.values(),
                RotationalBacktestConfig.Side.LONG);
        this.picksField = buildConfigField("5");
        this.rankerTypeCombo = buildEnumCombo(RotationalBacktestConfig.RankerType.values(),
                RotationalBacktestConfig.RankerType.RS_RVOL);
        this.entryCutoffField = buildConfigField("11:00");
        this.exitTimeField = buildConfigField("15:25");
        this.stopBasisCombo = buildEnumCombo(RotationalBacktestConfig.StopBasis.values(),
                RotationalBacktestConfig.StopBasis.OR_RANGE);
        this.stopMultiplierField = buildConfigField("0.8");
        this.trailingStopEnabledCheck = buildCheckBox("Enabled", true);
        this.trailingStopBasisCombo = buildEnumCombo(RotationalBacktestConfig.StopBasis.values(),
                RotationalBacktestConfig.StopBasis.ATR);
        this.trailingStopMultiplierField = buildConfigField("1.25");
        this.targetEnabledCheck = buildCheckBox("Enabled", true);
        this.targetBasisCombo = buildEnumCombo(RotationalBacktestConfig.TargetBasis.values(),
                RotationalBacktestConfig.TargetBasis.STOP_DISTANCE);
        this.targetMultiplierField = buildConfigField("1.5");
        this.initialCapitalField = buildConfigField("1000000");
        this.slippageField = buildConfigField("0.001");
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

    public void setOnOptimizationComplete(Consumer<OptimizationRunResult> handler) {
        this.onOptimizationComplete = handler;
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
                new Separator(),
                buildSection(OPTIMIZATION_HEADER, buildOptimizationSection()),
                buildButtonRow(),
                progressContainer
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add(SCROLL_STYLE);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private VBox buildSection(String header, VBox content) {
        Label label = createSectionLabel(header);
        VBox section = new VBox(SECTION_SPACING, label);
        section.getChildren().addAll(content.getChildren());
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
        grid.add(createConfigLabel("OR Minutes"), 0, row);
        grid.add(openingRangeMinutesField, 1, row++);
        grid.add(createConfigLabel("Bar Minutes"), 0, row);
        grid.add(barMinutesField, 1, row);
        addOptParamRow("openingRangeMinutes", "OR Minutes", "5", "1", "60", "1");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildEntryFiltersSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Min Gap/ATR"), 0, row);
        grid.add(minGapAtrField, 1, row++);
        grid.add(createConfigLabel("Max Gap/ATR"), 0, row);
        grid.add(maxGapAtrField, 1, row++);
        grid.add(createConfigLabel("Min OR/ATR"), 0, row);
        grid.add(minOrAtrField, 1, row++);
        grid.add(createConfigLabel("Max OR/ATR"), 0, row);
        grid.add(maxOrAtrField, 1, row++);
        grid.add(createConfigLabel("Min RVOL"), 0, row);
        grid.add(minOrbRvolField, 1, row++);
        grid.add(createConfigLabel("Max RVOL"), 0, row);
        grid.add(maxOrbRvolField, 1, row++);
        grid.add(createConfigLabel("Min RS Rank"), 0, row);
        grid.add(minRsRankField, 1, row++);
        grid.add(createConfigLabel("Max RS Rank"), 0, row);
        grid.add(maxRsRankField, 1, row++);
        grid.add(createConfigLabel("Min IBS"), 0, row);
        grid.add(minOrbIbsField, 1, row++);
        grid.add(createConfigLabel("Max IBS"), 0, row);
        grid.add(maxOrbIbsField, 1, row);
        addOptParamRow("minGapAtr", "Min Gap/ATR", "0.5", "0.0", "3.0", "0.1");
        addOptParamRow("minOrbRvol", "Min RVOL", "1.0", "0.5", "5.0", "0.25");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildEntryMethodSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Method"), 0, row);
        grid.add(entryMethodCombo, 1, row++);
        grid.add(createConfigLabel("Max Re-entries"), 0, row);
        grid.add(maxReEntriesField, 1, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildPicksRankingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Side"), 0, row);
        grid.add(sideCombo, 1, row++);
        grid.add(createConfigLabel("Picks"), 0, row);
        grid.add(picksField, 1, row++);
        grid.add(createConfigLabel("Ranker"), 0, row);
        grid.add(rankerTypeCombo, 1, row);
        addOptParamRow("picks", "Picks", "5", "1", "20", "1");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTimingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Entry Cutoff"), 0, row);
        grid.add(entryCutoffField, 1, row++);
        grid.add(createConfigLabel("Exit Time"), 0, row);
        grid.add(exitTimeField, 1, row);
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildStopLossSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Basis"), 0, row);
        grid.add(stopBasisCombo, 1, row++);
        grid.add(createConfigLabel("Multiplier"), 0, row);
        grid.add(stopMultiplierField, 1, row);
        addOptParamRow("stopMultiplier", "Stop Multiplier", "0.8", "0.5", "2.0", "0.1");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTrailingStopSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(trailingStopEnabledCheck, 0, row, 2, 1);
        row++;
        grid.add(createConfigLabel("Basis"), 0, row);
        grid.add(trailingStopBasisCombo, 1, row++);
        grid.add(createConfigLabel("Multiplier"), 0, row);
        grid.add(trailingStopMultiplierField, 1, row);
        addOptParamRow("trailingStopMultiplier", "Trail Stop Mult", "1.25", "0.5", "2.0", "0.25");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildTargetSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(targetEnabledCheck, 0, row, 2, 1);
        row++;
        grid.add(createConfigLabel("Basis"), 0, row);
        grid.add(targetBasisCombo, 1, row++);
        grid.add(createConfigLabel("Multiplier"), 0, row);
        grid.add(targetMultiplierField, 1, row);
        addOptParamRow("targetMultiplier", "Target Multiplier", "1.5", "0.5", "5.0", "0.25");
        return new VBox(SECTION_SPACING, grid);
    }

    private VBox buildPositionSizingSection() {
        GridPane grid = createConfigGrid();
        int row = 0;
        grid.add(createConfigLabel("Initial Capital"), 0, row);
        grid.add(initialCapitalField, 1, row++);
        grid.add(createConfigLabel("Slippage"), 0, row);
        grid.add(slippageField, 1, row++);
        grid.add(atrScalingCheck, 0, row, 2, 1);
        return new VBox(SECTION_SPACING, grid);
    }



    private VBox buildOptimizationSection() {
        VBox section = new VBox(SECTION_SPACING);

        // Header row: Optimize | Min | Max | Step
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(4);
        headerGrid.getStyleClass().add(OPT_GRID_STYLE);
        ColumnConstraints checkCol = new ColumnConstraints();
        checkCol.setPrefWidth(120);
        ColumnConstraints valCol = new ColumnConstraints();
        valCol.setPrefWidth(OPT_FIELD_WIDTH);
        headerGrid.getColumnConstraints().addAll(checkCol, valCol, valCol, valCol);
        Label hParam = createOptHeader("Parameter");
        Label hMin = createOptHeader("Min");
        Label hMax = createOptHeader("Max");
        Label hStep = createOptHeader("Step");
        headerGrid.add(hParam, 0, 0);
        headerGrid.add(hMin, 1, 0);
        headerGrid.add(hMax, 2, 0);
        headerGrid.add(hStep, 3, 0);
        section.getChildren().add(headerGrid);

        // Add rows for each optimizable parameter
        GridPane rowsGrid = new GridPane();
        rowsGrid.setHgap(4);
        rowsGrid.setVgap(2);
        rowsGrid.getStyleClass().add(OPT_GRID_STYLE);
        rowsGrid.getColumnConstraints().addAll(checkCol, valCol, valCol, valCol);

        int row = 0;
        for (Map.Entry<String, OptParamRow> entry : optParamRows.entrySet()) {
            OptParamRow opr = entry.getValue();
            rowsGrid.add(opr.optimizeCheck, 0, row);
            rowsGrid.add(opr.minField, 1, row);
            rowsGrid.add(opr.maxField, 2, row);
            rowsGrid.add(opr.stepField, 3, row);
            // Disable min/max/step when not optimizing
            opr.minField.disableProperty().bind(opr.optimizeCheck.selectedProperty().not());
            opr.maxField.disableProperty().bind(opr.optimizeCheck.selectedProperty().not());
            opr.stepField.disableProperty().bind(opr.optimizeCheck.selectedProperty().not());
            row++;
        }
        section.getChildren().add(rowsGrid);

        return section;
    }

    private HBox buildButtonRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(BUTTON_SPACING, runButton, spacer, stopButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        return row;
    }

    // ── Helper: add an optimizable parameter row ─────────────────────

    private void addOptParamRow(String fieldName, String label,
                                String defaultVal, String min, String max, String step) {
        CheckBox check = new CheckBox(label);
        check.getStyleClass().add(OPT_CHECKBOX_STYLE);
        check.setSelected(false);
        // Enforce max 2 selected
        check.selectedProperty().addListener((obs, wasSelected, isNow) -> {
            if (isNow) {
                long selected = optParamRows.values().stream()
                        .filter(r -> r.optimizeCheck.isSelected()).count();
                if (selected > 2) {
                    check.setSelected(false);
                    messageLabel.setText(MAX_TWO_PARAMS_MESSAGE);
                }
            }
        });
        TextField minField = buildOptField(min);
        TextField maxField = buildOptField(max);
        TextField stepField = buildOptField(step);
        optParamRows.put(fieldName, new OptParamRow(check, minField, maxField, stepField, fieldName));
    }

    // ── Widget builders ──────────────────────────────────────────────

    private ComboBox<String> buildUniverseCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(CONFIG_COMBO_STYLE);
        // Populate with group names
        try {
            List<Group> groups = groupRepository.loadAll();
            for (Group g : groups) combo.getItems().add(g.getName());
            if (!combo.getItems().isEmpty()) {
                // Try to select "ORB Universe" if present
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

    @SuppressWarnings("unchecked")
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

    private TextField buildOptField(String initial) {
        TextField field = new TextField(initial);
        field.getStyleClass().add(CONFIG_FIELD_STYLE);
        field.setPrefWidth(OPT_FIELD_WIDTH);
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

    private Label createOptHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(OPT_HEADER_STYLE);
        return label;
    }

    private GridPane createConfigGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_H_GAP);
        grid.setVgap(GRID_V_GAP);
        return grid;
    }

    private Button buildRunButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.PLAY_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(BUTTON_ICON_STYLE);
        Button button = new Button(RUN_LABEL, icon);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> startOptimization());
        return button;
    }

    private Button buildStopButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.STOP_16);
        icon.setIconSize(STOP_ICON_SIZE);
        icon.getStyleClass().add(STOP_ICON_STYLE);
        Button button = new Button(STOP_LABEL, icon);
        button.getStyleClass().add(STOP_BUTTON_STYLE);
        button.setDisable(true);
        button.setOnAction(e -> cancelOptimization());
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

        // Entry Filters (null if blank)
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

    private List<OptimizableParameter> buildSelectedParams() {
        List<OptimizableParameter> params = new ArrayList<>();
        for (Map.Entry<String, OptParamRow> entry : optParamRows.entrySet()) {
            OptParamRow row = entry.getValue();
            if (row.optimizeCheck.isSelected()) {
                params.add(new OptimizableParameter(
                        row.optimizeCheck.getText(),
                        entry.getKey(),
                        parseDouble(row.minField, 0),
                        parseDouble(row.maxField, 1),
                        parseDouble(row.stepField, 0.1)
                ));
            }
        }
        return params;
    }

    // ── Run / Cancel ─────────────────────────────────────────────────

    private void startOptimization() {
        List<OptimizableParameter> params = buildSelectedParams();
        if (params.isEmpty()) {
            messageLabel.setText("Select at least 1 parameter to optimize");
            showProgressContainer(true);
            return;
        }
        if (params.size() > 2) {
            messageLabel.setText(MAX_TWO_PARAMS_MESSAGE);
            showProgressContainer(true);
            return;
        }

        RotationalBacktestConfig baseConfig;
        try {
            baseConfig = buildConfigFromFields();
            baseConfig.validate();
        } catch (Exception e) {
            messageLabel.setText("Config error: " + e.getMessage());
            showProgressContainer(true);
            return;
        }

        setAllDisabled(true);
        showProgressContainer(true);
        progressBar.setProgress(-1);
        progressLabel.setText("");
        messageLabel.setText(LOADING_MESSAGE);

        int totalConfigs = ParameterOptimizationEngine.gridSize(params);
        ParameterOptimizationEngine engine = new ParameterOptimizationEngine();
        currentEngine = engine;

        Thread thread = new Thread(() -> runOptimization(engine, baseConfig, params, totalConfigs));
        thread.setDaemon(true);
        thread.setName("orb-optimizer-thread");
        thread.start();
    }

    private void runOptimization(ParameterOptimizationEngine engine,
                                 RotationalBacktestConfig baseConfig,
                                 List<OptimizableParameter> params,
                                 int totalConfigs) {
        try {
            // Load data
            String groupName = baseConfig.getUniverseGroupName();
            List<String> scripIds = RotationalOrbDriver.loadGroupScrips(groupName);
            Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                    barsRepository, scripIds, Timeframe.FIVE_MINUTE);
            Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                    barsRepository, scripIds, Timeframe.DAILY);

            if (intradayBars.isEmpty()) {
                Platform.runLater(() -> onFinished(FAILED_MESSAGE + ": no data"));
                return;
            }

            Platform.runLater(() -> {
                messageLabel.setText(RUNNING_MESSAGE);
                progressBar.setProgress(0);
            });

            // Run optimization
            List<OptimizationResult> results = engine.optimize(
                    baseConfig, params, intradayBars, dailyBars,
                    (completed, total, latest) -> Platform.runLater(() -> {
                        double fraction = total > 0 ? (double) completed / total : 0;
                        progressBar.setProgress(fraction);
                        progressLabel.setText(String.format(PROGRESS_FORMAT, completed, total));
                    })
            );

            Platform.runLater(() -> {
                onFinished(engine.isCancelled() ? CANCELLED_MESSAGE : COMPLETED_MESSAGE);
                if (!engine.isCancelled() && onOptimizationComplete != null) {
                    onOptimizationComplete.accept(
                            new OptimizationRunResult(baseConfig, params, results));
                }
            });

        } catch (Exception e) {
            log.error("Optimization failed", e);
            Platform.runLater(() -> onFinished(FAILED_MESSAGE + ": " + e.getMessage()));
        }
    }

    private void cancelOptimization() {
        if (currentEngine != null) {
            currentEngine.cancel();
        }
    }

    private void onFinished(String status) {
        messageLabel.setText(status);
        progressBar.setProgress(1.0);
        setAllDisabled(false);
        stopButton.setDisable(true);
        currentEngine = null;
    }

    private void setAllDisabled(boolean disabled) {
        runButton.setDisable(disabled);
        stopButton.setDisable(!disabled);
        universeCombo.setDisable(disabled);
        openingRangeMinutesField.setDisable(disabled);
        barMinutesField.setDisable(disabled);
        entryMethodCombo.setDisable(disabled);
        rankerTypeCombo.setDisable(disabled);
        stopBasisCombo.setDisable(disabled);
        trailingStopBasisCombo.setDisable(disabled);
        targetBasisCombo.setDisable(disabled);
    }

    private void showProgressContainer(boolean show) {
        progressContainer.setVisible(show);
        progressContainer.setManaged(show);
    }

    // ── Parse helpers ────────────────────────────────────────────────

    private static int parseInt(TextField field, int defaultValue) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(TextField field, double defaultValue) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Double parseNullableDouble(TextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Result data passed to the output pane via the callback.
     */
    public record OptimizationRunResult(
            RotationalBacktestConfig baseConfig,
            List<OptimizableParameter> params,
            List<OptimizationResult> results
    ) {}
}
