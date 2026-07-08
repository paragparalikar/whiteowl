package com.whiteowl.workbench.note;

import com.whiteowl.core.note.model.Note;
import com.whiteowl.core.note.repository.NoteRepository;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.BiConsumer;

@Slf4j
public final class NoteEditorPane extends VBox {

    private static final String PANE_STYLE = "note-editor-pane";
    private static final String HEADER_STYLE = "note-editor-header";
    private static final String TITLE_STYLE = "note-editor-title";
    private static final String TITLE_FIELD_STYLE = "note-editor-title-field";
    private static final String TEXT_AREA_STYLE = "note-editor-text-area";
    private static final String SAVE_BUTTON_STYLE = "note-editor-save-button";
    private static final String SAVE_ICON_STYLE = "note-editor-save-icon";
    private static final String STATUS_STYLE = "note-editor-status";
    private static final String SAVE_TEXT = "Save";
    private static final String STATUS_SAVED = "Saved";
    private static final String STATUS_MODIFIED = "Modified";
    private static final int ICON_SIZE = 14;
    private static final int HEADER_SPACING = 8;
    private static final KeyCodeCombination SAVE_KEY_COMBO =
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);

    private final NoteRepository noteRepository;
    private final TextArea textArea;
    private final Label titleLabel;
    private final Label statusLabel;
    private final HBox headerRow;
    @Getter private Note activeNote;
    private BiConsumer<String, String> onNoteRenamed;

    public NoteEditorPane(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
        this.titleLabel = buildTitleLabel();
        this.headerRow = buildHeaderRow();
        this.textArea = buildTextArea();
        this.statusLabel = buildStatusLabel();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        setOnKeyPressed(event -> {
            if (SAVE_KEY_COMBO.match(event)) {
                saveCurrentNote();
                event.consume();
            }
        });
    }

    public void openNote(Note note) {
        saveCurrentNote();
        this.activeNote = note;
        titleLabel.setText(note.getName());
        textArea.setText(note.getContent());
        statusLabel.setText("");
    }

    public void setOnNoteRenamed(BiConsumer<String, String> handler) {
        this.onNoteRenamed = handler;
    }

    public void saveCurrentNote() {
        if (activeNote != null) {
            activeNote.setContent(textArea.getText());
            noteRepository.save(activeNote);
            statusLabel.setText(STATUS_SAVED);
        }
    }

    private void buildLayout() {
        VBox headerSection = new VBox(2, headerRow);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        getChildren().addAll(headerSection, textArea, statusLabel);
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

    private Button buildSaveButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.SAVE_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(SAVE_ICON_STYLE);
        Button button = new Button(SAVE_TEXT, icon);
        button.getStyleClass().add(SAVE_BUTTON_STYLE);
        button.setOnAction(e -> saveCurrentNote());
        return button;
    }

    private TextArea buildTextArea() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.getStyleClass().add(TEXT_AREA_STYLE);
        area.textProperty().addListener((obs, oldVal, newVal) -> statusLabel.setText(STATUS_MODIFIED));
        return area;
    }

    private Label buildStatusLabel() {
        Label label = new Label();
        label.getStyleClass().add(STATUS_STYLE);
        return label;
    }

    private void startInlineRename() {
        if (activeNote == null) return;
        TextField field = new TextField(activeNote.getName());
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
        if (newName.isEmpty() || newName.equals(activeNote.getName())) return;
        String oldName = activeNote.getName();
        noteRepository.rename(oldName, newName);
        activeNote.setName(newName);
        titleLabel.setText(newName);
        if (onNoteRenamed != null) {
            onNoteRenamed.accept(oldName, newName);
        }
    }

    private void cancelRename(TextField field) {
        headerRow.getChildren().set(headerRow.getChildren().indexOf(field), titleLabel);
    }

}
