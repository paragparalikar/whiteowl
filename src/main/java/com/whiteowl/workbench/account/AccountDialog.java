package com.whiteowl.workbench.account;

import static com.whiteowl.core.account.model.Account.DEFAULT_PREFERRED_POSITION_COUNT;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.portfolio.model.BrokerType;
import com.whiteowl.workbench.common.BaseDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

import java.util.Optional;

public final class AccountDialog extends BaseDialog {

    private static final String HEADER_NEW = "New Account";
    private static final String HEADER_EDIT = "Edit Account";
    private static final String LABEL_NAME = "Name";
    private static final String LABEL_BROKER = "Broker";
    private static final String LABEL_USER_ID = "User ID";
    private static final String LABEL_PASSWORD = "Password";
    private static final String LABEL_PIN = "PIN";
    private static final String LABEL_PREFERRED_POSITIONS = "Preferred Positions";
    private static final String BUTTON_SAVE = "Save";
    private static final int DIALOG_WIDTH = 420;
    private static final int MIN_POSITION_COUNT = 1;
    private static final int MAX_POSITION_COUNT = 100;

    private final AccountService accountService;
    private final Account editingAccount;
    private final TextField nameField;
    private final ComboBox<BrokerType> brokerCombo;
    private final TextField userIdField;
    private final PasswordField passwordField;
    private final PasswordField pinField;
    private final Spinner<Integer> preferredPositionsSpinner;
    @Getter private boolean saved;

    public AccountDialog(AccountService accountService) {
        this(accountService, null);
    }

    public AccountDialog(AccountService accountService, Account editingAccount) {
        this.accountService = accountService;
        this.editingAccount = editingAccount;
        this.nameField = createTextField();
        this.brokerCombo = createBrokerCombo();
        this.userIdField = createTextField();
        this.passwordField = createPasswordField();
        this.pinField = createPasswordField();
        int positionCount = editingAccount != null
                ? editingAccount.getPreferredPositionCount() : DEFAULT_PREFERRED_POSITION_COUNT;
        this.preferredPositionsSpinner = createSpinner(MIN_POSITION_COUNT, MAX_POSITION_COUNT, positionCount);
        if (editingAccount != null) {
            nameField.setText(editingAccount.getName());
            brokerCombo.setValue(editingAccount.getBrokerType());
            userIdField.setText(editingAccount.getUserId());
            passwordField.setText(editingAccount.getPassword());
            pinField.setText(editingAccount.getPin());
        }
    }

    @Override
    protected String getTitle() {
        return editingAccount != null ? HEADER_EDIT : HEADER_NEW;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return editingAccount != null ? FluentUiRegularAL.EDIT_16 : FluentUiRegularAL.ADD_16;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        int row = 0;
        addFormRow(grid, LABEL_NAME, nameField, row++);
        addFormRow(grid, LABEL_BROKER, brokerCombo, row++);
        addFormRow(grid, LABEL_USER_ID, userIdField, row++);
        addFormRow(grid, LABEL_PASSWORD, passwordField, row++);
        addFormRow(grid, LABEL_PIN, pinField, row++);
        addFormRow(grid, LABEL_PREFERRED_POSITIONS, preferredPositionsSpinner, row);
    }

    @Override
    protected String getPrimaryButtonText() {
        return BUTTON_SAVE;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SAVE_20;
    }

    @Override
    protected void onPrimaryAction() {
        String name = nameField.getText();
        BrokerType broker = brokerCombo.getValue();
        String userId = userIdField.getText();
        String password = passwordField.getText();
        String pin = pinField.getText();
        String excludeId = editingAccount != null ? editingAccount.getId() : null;
        Optional<String> error = accountService.validate(name, broker, userId, password, pin, excludeId);
        if (error.isPresent()) {
            showError(error.get());
            return;
        }
        int positionCount = preferredPositionsSpinner.getValue();
        if (editingAccount != null) {
            accountService.update(editingAccount, name, broker, userId, password, pin, positionCount);
        } else {
            accountService.create(name, broker, userId, password, pin, positionCount);
        }
        saved = true;
        closeDialog();
    }

    private ComboBox<BrokerType> createBrokerCombo() {
        ComboBox<BrokerType> combo = createComboBox();
        combo.getItems().addAll(BrokerType.values());
        combo.setValue(BrokerType.ZERODHA);
        return combo;
    }

}
