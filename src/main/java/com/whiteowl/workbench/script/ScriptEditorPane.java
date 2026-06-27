package com.whiteowl.workbench.script;

import com.whiteowl.core.script.ScriptCompilationException;
import com.whiteowl.core.script.ScriptCompiler;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.workbench.backtest.GroovySyntaxHighlighter;
import groovy.lang.Script;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public final class ScriptEditorPane extends VBox {

    private static final String PANE_STYLE = "script-editor-pane";
    private static final String HEADER_STYLE = "script-editor-header";
    private static final String TITLE_STYLE = "script-editor-title";
    private static final String TITLE_FIELD_STYLE = "script-editor-title-field";
    private static final String NAME_ERROR_STYLE = "script-editor-name-error";
    private static final String EDITOR_STYLE = "script-editor-code-area";
    private static final String SAVE_BUTTON_STYLE = "script-editor-save-button";
    private static final String SAVE_ICON_STYLE = "script-editor-save-icon";
    private static final String STATUS_STYLE = "script-editor-status";
    private static final String ERROR_STYLE = "script-editor-error";
    private static final String REFERENCE_STYLE = "script-editor-reference";
    private static final String REFERENCE_CONTENT_STYLE = "script-editor-reference-content";
    private static final String SAVE_TEXT = "Save";
    private static final String STATUS_SAVED = "Saved";
    private static final String STATUS_COMPILED = "Compiled successfully";
    private static final String STATUS_MODIFIED = "Modified";
    private static final String ERROR_SAVE_FAILED = "Save failed: %s";
    private static final String ERROR_LINE_FORMAT = "Line %d: %s";
    private static final String ERROR_DEFAULT_NAME = "Please provide a valid name for this script";
    private static final String REFERENCE_TITLE_TEXT = "DSL Reference";
    private static final String STYLESHEET_PATH = "/css/script-editor.css";
    private static final int ICON_SIZE = 14;
    private static final int HEADER_SPACING = 8;
    private static final long HIGHLIGHT_DELAY_MS = 100;

    private final ScriptRepository repository;
    private final Class<? extends Script> baseClass;
    private final List<String> additionalImports;
    private final String defaultName;
    private final InlineCssTextArea editor;
    private final Label titleLabel;
    private final Label nameErrorLabel;
    private final Label statusLabel;
    private final Label errorLabel;
    private final HBox headerRow;
    private static final KeyCodeCombination SAVE_KEY_COMBO = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);

    private ScriptDescriptor activeScript;
    private BiConsumer<ScriptDescriptor, ScriptDescriptor> onScriptRenamed;
    private Consumer<ScriptDescriptor> onScriptSaved;

    public ScriptEditorPane(ScriptRepository repository, Class<? extends Script> baseClass,
                            List<String> additionalImports, String defaultName,
                            String dslReference) {
        this.repository = repository;
        this.baseClass = baseClass;
        this.additionalImports = additionalImports;
        this.defaultName = defaultName;
        this.titleLabel = buildTitleLabel();
        this.nameErrorLabel = buildNameErrorLabel();
        this.headerRow = buildHeaderRow();
        this.editor = buildCodeArea();
        this.statusLabel = buildStatusLabel();
        this.errorLabel = buildErrorLabel();
        getStyleClass().add(PANE_STYLE);
        buildLayout(dslReference);
        setOnKeyPressed(event -> {
            if (SAVE_KEY_COMBO.match(event)) {
                saveAndCompile();
                event.consume();
            }
        });
    }

    public void setOnScriptRenamed(BiConsumer<ScriptDescriptor, ScriptDescriptor> handler) {
        this.onScriptRenamed = handler;
    }

    public void setOnScriptSaved(Consumer<ScriptDescriptor> handler) {
        this.onScriptSaved = handler;
    }

    public void openScript(ScriptDescriptor descriptor) {
        this.activeScript = descriptor;
        titleLabel.setText(descriptor.getName());
        nameErrorLabel.setText("");
        try {
            String script = repository.loadScript(descriptor);
            editor.replaceText(script);
            editor.moveTo(0);
            editor.requestFollowCaret();
            statusLabel.setText("");
            errorLabel.setText("");
        } catch (IOException e) {
            log.error("Failed to load script: {}", e.getMessage());
            editor.replaceText("");
            errorLabel.setText(String.format(ERROR_SAVE_FAILED, e.getMessage()));
        }
    }

    public ScriptDescriptor getActiveScript() {
        return activeScript;
    }

    private void buildLayout(String dslReference) {
        VBox headerSection = new VBox(2, headerRow, nameErrorLabel);
        TitledPane referencePane = buildReferencePane(dslReference);
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
        if (activeScript == null) return;
        TextField field = new TextField(activeScript.getName());
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
        if (newName.isEmpty() || newName.equals(activeScript.getName())) return;
        try {
            ScriptDescriptor oldScript = activeScript;
            activeScript = repository.rename(activeScript, newName);
            titleLabel.setText(newName);
            nameErrorLabel.setText("");
            if (onScriptRenamed != null) {
                onScriptRenamed.accept(oldScript, activeScript);
            }
        } catch (IOException ex) {
            log.error("Failed to rename script: {}", ex.getMessage());
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

    private TitledPane buildReferencePane(String dslReference) {
        Label content = new Label(dslReference);
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
        if (activeScript == null) return;
        if (isDefaultName(activeScript.getName())) {
            nameErrorLabel.setText(ERROR_DEFAULT_NAME);
            return;
        }
        String content = editor.getText();
        applyHighlighting();
        try {
            repository.saveScript(activeScript, content);
            statusLabel.setText(STATUS_SAVED);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText(String.format(ERROR_SAVE_FAILED, e.getMessage()));
            return;
        }
        try {
            ScriptCompiler.compile(content, baseClass, additionalImports);
            statusLabel.setText(STATUS_COMPILED);
            errorLabel.setText("");
            if (onScriptSaved != null) {
                onScriptSaved.accept(activeScript);
            }
        } catch (ScriptCompilationException e) {
            errorLabel.setText(String.format(ERROR_LINE_FORMAT, e.getLine(), e.getDetail()));
        }
    }

    private boolean isDefaultName(String name) {
        return name == null || name.isBlank() || defaultName.equals(name);
    }

}
