package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.scripting.script.ScriptCompilationException;
import com.whiteowl.scripting.script.ScriptCompiler;
import com.whiteowl.scripting.script.ScriptDescriptor;
import com.whiteowl.scripting.script.ScriptRepository;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.Duration;
import java.util.function.BiConsumer;

@Slf4j
public final class StrategyEditorPane extends VBox {

    private static final String PANE_STYLE = "strategy-editor-pane";
    private static final String HEADER_STYLE = "strategy-editor-header";
    private static final String TITLE_STYLE = "strategy-editor-title";
    private static final String TITLE_FIELD_STYLE = "strategy-editor-title-field";
    private static final String NAME_ERROR_STYLE = "strategy-editor-name-error";
    private static final String EDITOR_STYLE = "strategy-editor-code-area";
    private static final String SAVE_BUTTON_STYLE = "strategy-editor-save-button";
    private static final String SAVE_ICON_STYLE = "strategy-editor-save-icon";
    private static final String STATUS_STYLE = "strategy-editor-status";
    private static final String ERROR_STYLE = "strategy-editor-error";
    private static final String REFERENCE_STYLE = "strategy-editor-reference";
    private static final String REFERENCE_CONTENT_STYLE = "strategy-editor-reference-content";
    private static final String SAVE_TEXT = "Save";
    private static final String STATUS_SAVED = "Saved";
    private static final String STATUS_COMPILED = "Compiled successfully";
    private static final String STATUS_MODIFIED = "Modified";
    private static final String ERROR_SAVE_FAILED = "Save failed: %s";
    private static final String ERROR_LINE_FORMAT = "Line %d: %s";
    private static final String ERROR_DEFAULT_NAME = "Please provide a valid name for trading strategy";
    private static final String REFERENCE_TITLE_TEXT = "DSL Reference";
    private static final String DEFAULT_STRATEGY_NAME = "New Trading Strategy";
    private static final String STYLESHEET_PATH = "/css/strategy-editor.css";
    private static final int ICON_SIZE = 14;
    private static final int HEADER_SPACING = 8;
    private static final long HIGHLIGHT_DELAY_MS = 100;
    private static final String DSL_REFERENCE = """
            SmartValues: open, high, low, close, volume, timestamp
            Access: close[0]=current, close[-1]=previous

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              bollinger(source, period, stdDev) → [middle, upper, lower]
              supertrend(period, mult) → [value, direction]
              twap()                       → intraday TWAP (resets daily)
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)

            Signals:
              longEntry(qty)               longExit()
              shortEntry(qty)              shortExit()
              closePosition(id)

            Lifecycle: setup() + onBar(bar, scripId)
            Inputs: input(name, default, min?, max?, step?)""";

    private final ScriptRepository strategyRepository;
    private final InlineCssTextArea editor;
    private final Label titleLabel;
    private final Label nameErrorLabel;
    private final Label statusLabel;
    private final Label errorLabel;
    private final HBox headerRow;
    private ScriptDescriptor activeStrategy;
    private BiConsumer<ScriptDescriptor, ScriptDescriptor> onStrategyRenamed;

    public StrategyEditorPane(ScriptRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
        this.titleLabel = buildTitleLabel();
        this.nameErrorLabel = buildNameErrorLabel();
        this.headerRow = buildHeaderRow();
        this.editor = buildCodeArea();
        this.statusLabel = buildStatusLabel();
        this.errorLabel = buildErrorLabel();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    public void setOnStrategyRenamed(BiConsumer<ScriptDescriptor, ScriptDescriptor> handler) {
        this.onStrategyRenamed = handler;
    }

    public void openStrategy(ScriptDescriptor strategy) {
        this.activeStrategy = strategy;
        titleLabel.setText(strategy.getName());
        nameErrorLabel.setText("");
        try {
            String script = strategyRepository.loadScript(strategy);
            editor.replaceText(script);
            editor.moveTo(0);
            editor.requestFollowCaret();
            statusLabel.setText("");
            errorLabel.setText("");
        } catch (IOException e) {
            log.error("Failed to load strategy: {}", e.getMessage());
            editor.replaceText("");
            errorLabel.setText(String.format(ERROR_SAVE_FAILED, e.getMessage()));
        }
    }

    public ScriptDescriptor getActiveStrategy() {
        return activeStrategy;
    }

    private void buildLayout() {
        VBox headerSection = new VBox(2, headerRow, nameErrorLabel);
        TitledPane referencePane = buildReferencePane();
        VBox.setVgrow(editor, Priority.ALWAYS);
        getChildren().addAll(headerSection, editor, statusLabel, errorLabel, referencePane);
    }

    private HBox buildHeaderRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button saveButton = buildSaveButton();
        HBox header = new HBox(HEADER_SPACING, titleLabel, spacer, saveButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add(HEADER_STYLE);
        return header;
    }

    private Label buildTitleLabel() {
        Label label = new Label();
        label.getStyleClass().add(TITLE_STYLE);
        label.setCursor(javafx.scene.Cursor.HAND);
        label.setOnMouseClicked(e -> startInlineRename());
        return label;
    }

    private Label buildNameErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add(NAME_ERROR_STYLE);
        return label;
    }

    private void startInlineRename() {
        if (activeStrategy == null) return;
        TextField field = new TextField(activeStrategy.getName());
        field.getStyleClass().add(TITLE_FIELD_STYLE);
        field.selectAll();
        int titleIndex = headerRow.getChildren().indexOf(titleLabel);
        headerRow.getChildren().set(titleIndex, field);
        field.requestFocus();
        field.setOnAction(e -> commitRename(field));
        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) cancelRename(field);
        });
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) commitRename(field);
        });
    }

    private void commitRename(TextField field) {
        if (headerRow.getChildren().contains(titleLabel)) return;
        String newName = field.getText().trim();
        headerRow.getChildren().set(headerRow.getChildren().indexOf(field), titleLabel);
        if (newName.isEmpty() || newName.equals(activeStrategy.getName())) return;
        try {
            ScriptDescriptor oldStrategy = activeStrategy;
            activeStrategy = strategyRepository.rename(activeStrategy, newName);
            titleLabel.setText(newName);
            nameErrorLabel.setText("");
            if (onStrategyRenamed != null) {
                onStrategyRenamed.accept(oldStrategy, activeStrategy);
            }
        } catch (IOException ex) {
            log.error("Failed to rename strategy: {}", ex.getMessage());
        }
    }

    private void cancelRename(TextField field) {
        headerRow.getChildren().set(headerRow.getChildren().indexOf(field), titleLabel);
    }

    private InlineCssTextArea buildCodeArea() {
        InlineCssTextArea area = new InlineCssTextArea();
        area.getStyleClass().add(EDITOR_STYLE);
        String stylesheet = getClass().getResource(STYLESHEET_PATH).toExternalForm();
        area.getStylesheets().add(stylesheet);
        area.setWrapText(false);
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.multiPlainChanges()
                .successionEnds(Duration.ofMillis(HIGHLIGHT_DELAY_MS))
                .subscribe(changes -> applyHighlighting());
        area.textProperty().addListener((obs, oldVal, newVal) -> onTextChanged());
        return area;
    }

    private void applyHighlighting() {
        String text = editor.getText();
        if (!text.isEmpty()) {
            editor.setStyleSpans(0, GroovySyntaxHighlighter.computeHighlighting(text));
        }
    }

    private Button buildSaveButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.SAVE_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(SAVE_ICON_STYLE);
        Button button = new Button(SAVE_TEXT, icon);
        button.getStyleClass().add(SAVE_BUTTON_STYLE);
        button.setOnAction(e -> saveAndCompile());
        return button;
    }

    private Label buildStatusLabel() {
        Label label = new Label();
        label.getStyleClass().add(STATUS_STYLE);
        return label;
    }

    private Label buildErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add(ERROR_STYLE);
        label.setWrapText(true);
        return label;
    }

    private TitledPane buildReferencePane() {
        Label content = new Label(DSL_REFERENCE);
        content.getStyleClass().add(REFERENCE_CONTENT_STYLE);
        content.setWrapText(true);
        TitledPane pane = new TitledPane(REFERENCE_TITLE_TEXT, content);
        pane.setExpanded(false);
        pane.setAnimated(true);
        pane.getStyleClass().add(REFERENCE_STYLE);
        return pane;
    }

    private void onTextChanged() {
        errorLabel.setText("");
        statusLabel.setText(STATUS_MODIFIED);
    }

    private void saveAndCompile() {
        if (activeStrategy == null) return;
        if (isDefaultName(activeStrategy.getName())) {
            nameErrorLabel.setText(ERROR_DEFAULT_NAME);
            return;
        }
        String content = editor.getText();
        applyHighlighting();
        try {
            strategyRepository.saveScript(activeStrategy, content);
            statusLabel.setText(STATUS_SAVED);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText(String.format(ERROR_SAVE_FAILED, e.getMessage()));
            return;
        }
        try {
            ScriptCompiler.compile(content, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
            statusLabel.setText(STATUS_COMPILED);
            errorLabel.setText("");
        } catch (ScriptCompilationException e) {
            errorLabel.setText(String.format(ERROR_LINE_FORMAT, e.getLine(), e.getDetail()));
        }
    }

    private boolean isDefaultName(String name) {
        return name == null || name.isBlank() || DEFAULT_STRATEGY_NAME.equals(name);
    }

}
