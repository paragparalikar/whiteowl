package com.whiteowl.workbench.common;

import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;

public final class NameInputDialog extends BaseDialog {

    private static final String CREATE_TEXT = "Create";
    private static final String NAME_LABEL = "Name";
    private static final int DIALOG_WIDTH = 420;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    private final String title;
    private final TextField nameField;
    @Getter private String enteredName;

    public NameInputDialog(String title, String prompt) {
        this.title = title;
        this.nameField = createTextField();
        this.nameField.setPromptText(prompt);
        this.nameField.setOnAction(e -> onPrimaryAction());
    }

    @Override
    protected String getTitle() {
        return title;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.ADD_16;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        addFormRow(grid, NAME_LABEL, nameField, 0);
    }

    @Override
    protected String getPrimaryButtonText() {
        return CREATE_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularAL.ADD_16;
    }

    @Override
    protected void onPrimaryAction() {
        String name = nameField.getText();
        if (name != null && !name.isBlank()
                && name.trim().length() >= MIN_NAME_LENGTH
                && name.trim().length() <= MAX_NAME_LENGTH) {
            enteredName = name.trim();
            closeDialog();
        }
    }

}
