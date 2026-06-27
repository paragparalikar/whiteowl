package com.whiteowl.workbench.group;

import com.whiteowl.workbench.common.BaseDialog;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;

import java.io.File;

public final class GroupImportDialog extends BaseDialog {

    private static final String TITLE_TEXT = "Import Group";
    private static final String IMPORT_TEXT = "Import";
    private static final String NAME_LABEL = "Name";
    private static final String FILE_LABEL = "File";
    private static final String FILE_PROMPT = "Select CSV file...";
    private static final String BROWSE_TEXT = "Browse";
    private static final String CSV_DESCRIPTION = "CSV Files";
    private static final String CSV_EXTENSION = "*.csv";
    private static final String BROWSE_STYLE = "dialog-primary-button";
    private static final int DIALOG_WIDTH = 420;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    private final TextField nameField;
    private final TextField filePathField;
    @Getter private String enteredName;
    @Getter private File selectedFile;

    public GroupImportDialog() {
        this.nameField = createTextField();
        this.filePathField = createTextField();
        this.filePathField.setPromptText(FILE_PROMPT);
        this.filePathField.setEditable(false);
    }

    @Override
    protected String getTitle() {
        return TITLE_TEXT;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.ARROW_IMPORT_20;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        addFormRow(grid, NAME_LABEL, nameField, 0);
        Button browseButton = new Button(BROWSE_TEXT);
        browseButton.getStyleClass().add(BROWSE_STYLE);
        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(CSV_DESCRIPTION, CSV_EXTENSION));
            File file = chooser.showOpenDialog(filePathField.getScene().getWindow());
            if (file != null) {
                filePathField.setText(file.getAbsolutePath());
            }
        });
        HBox fileRow = new HBox(8, filePathField, browseButton);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        addFormRow(grid, FILE_LABEL, fileRow, 1);
    }

    @Override
    protected String getPrimaryButtonText() {
        return IMPORT_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularAL.ARROW_IMPORT_20;
    }

    @Override
    protected void onPrimaryAction() {
        String name = nameField.getText();
        String filePath = filePathField.getText();
        if (name == null || name.isBlank() || filePath == null || filePath.isBlank()) return;
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) return;
        enteredName = trimmed;
        selectedFile = new File(filePath);
        closeDialog();
    }

}
