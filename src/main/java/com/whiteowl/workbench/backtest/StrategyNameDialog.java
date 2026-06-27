package com.whiteowl.workbench.backtest;

import com.whiteowl.workbench.common.BaseDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

public final class StrategyNameDialog extends BaseDialog {

    private static final String CONFIRM_TEXT = "OK";
    private static final String NAME_LABEL = "Name";
    private static final int DIALOG_WIDTH = 420;

    private final String title;
    private final TextField nameField;
    @Getter private boolean confirmed;

    public StrategyNameDialog(String title, String initialName) {
        this.title = title;
        this.nameField = createTextField(initialName);
        this.nameField.setOnAction(e -> onPrimaryAction());
    }

    public String getName() {
        return nameField.getText().trim();
    }

    @Override
    protected String getTitle() {
        return title;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.EDIT_16;
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
        return CONFIRM_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SAVE_20;
    }

    @Override
    protected void onPrimaryAction() {
        if (!nameField.getText().trim().isEmpty()) {
            confirmed = true;
            closeDialog();
        }
    }

}
