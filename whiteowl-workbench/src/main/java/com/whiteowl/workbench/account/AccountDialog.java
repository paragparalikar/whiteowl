package com.whiteowl.workbench.account;

import static com.whiteowl.core.account.model.Account.DEFAULT_GTT_EXPIRY_WARNING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_POSITION_SIZE;
import static com.whiteowl.core.account.model.Account.DEFAULT_STALE_HOLDING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_MIN_RISK_REWARD_RATIO;
import static com.whiteowl.core.account.model.Account.DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;

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
    private static final String LABEL_POSITION_SIZE = "Position Size";
    private static final String LABEL_STOP_LOSS_PCT = "Stop Loss %";
    private static final String LABEL_WIDE_SL_PCT = "Wide SL %";
    private static final String LABEL_TIGHT_SL_PCT = "Tight SL %";
    private static final String LABEL_MAX_CONCENTRATION = "Max Concentration %";
    private static final String LABEL_STALE_DAYS = "Stale Holding Days";
    private static final String LABEL_GTT_EXPIRY_DAYS = "GTT Expiry Warning Days";
    private static final String LABEL_MIN_RR_RATIO = "Min Risk:Reward Ratio";
    private static final String BUTTON_SAVE = "Save";
    private static final int DIALOG_WIDTH = 420;
    private static final double MIN_POSITION_SIZE = 1;
    private static final double MAX_POSITION_SIZE = 100000000;
    private static final double POSITION_SIZE_STEP = 10000;
    private static final double MIN_STOP_LOSS_PCT = 0.5;
    private static final double MAX_STOP_LOSS_PCT = 50.0;
    private static final double STOP_LOSS_PCT_STEP = 0.5;
    private static final int MIN_DAYS = 1;
    private static final int MAX_STALE_DAYS = 365;
    private static final int MAX_EXPIRY_DAYS = 90;
    private static final double MIN_RR_RATIO = 0.5;
    private static final double MAX_RR_RATIO = 10.0;
    private static final double RR_RATIO_STEP = 0.5;

    private final AccountService accountService;
    private final Account editingAccount;
    private final TextField nameField;
    private final ComboBox<BrokerType> brokerCombo;
    private final TextField userIdField;
    private final PasswordField passwordField;
    private final PasswordField pinField;
    private final Spinner<Double> positionSizeSpinner;
    private final Spinner<Double> stopLossPctSpinner;
    private final Spinner<Double> wideSlPctSpinner;
    private final Spinner<Double> tightSlPctSpinner;
    private final Spinner<Double> maxConcentrationSpinner;
    private final Spinner<Integer> staleDaysSpinner;
    private final Spinner<Integer> gttExpiryDaysSpinner;
    private final Spinner<Double> minRrRatioSpinner;
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
        double positionSize = editingAccount != null
                ? editingAccount.getPositionSize() : DEFAULT_POSITION_SIZE;
        this.positionSizeSpinner = createDoubleSpinner(MIN_POSITION_SIZE, MAX_POSITION_SIZE, positionSize, POSITION_SIZE_STEP);
        double stopLossPct = editingAccount != null
                ? editingAccount.getStopLossPercentage() : DEFAULT_STOP_LOSS_PERCENTAGE;
        this.stopLossPctSpinner = createDoubleSpinner(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT, stopLossPct, STOP_LOSS_PCT_STEP);
        double wideSlPct = editingAccount != null
                ? editingAccount.getWideStopLossPercentage() : DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;
        this.wideSlPctSpinner = createDoubleSpinner(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT, wideSlPct, STOP_LOSS_PCT_STEP);
        double tightSlPct = editingAccount != null
                ? editingAccount.getTightStopLossPercentage() : DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
        this.tightSlPctSpinner = createDoubleSpinner(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT, tightSlPct, STOP_LOSS_PCT_STEP);
        double maxConcentration = editingAccount != null
                ? editingAccount.getMaxConcentrationPercentage() : DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
        this.maxConcentrationSpinner = createDoubleSpinner(MIN_STOP_LOSS_PCT, 100.0, maxConcentration, 1.0);
        int staleDays = editingAccount != null
                ? editingAccount.getStaleHoldingDays() : DEFAULT_STALE_HOLDING_DAYS;
        this.staleDaysSpinner = createSpinner(MIN_DAYS, MAX_STALE_DAYS, staleDays);
        int gttExpiryDays = editingAccount != null
                ? editingAccount.getGttExpiryWarningDays() : DEFAULT_GTT_EXPIRY_WARNING_DAYS;
        this.gttExpiryDaysSpinner = createSpinner(MIN_DAYS, MAX_EXPIRY_DAYS, gttExpiryDays);
        double minRrRatio = editingAccount != null
                ? editingAccount.getMinRiskRewardRatio() : DEFAULT_MIN_RISK_REWARD_RATIO;
        this.minRrRatioSpinner = createDoubleSpinner(MIN_RR_RATIO, MAX_RR_RATIO, minRrRatio, RR_RATIO_STEP);
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
        addFormRow(grid, LABEL_POSITION_SIZE, positionSizeSpinner, row++);
        addFormRow(grid, LABEL_STOP_LOSS_PCT, stopLossPctSpinner, row++);
        addFormRow(grid, LABEL_WIDE_SL_PCT, wideSlPctSpinner, row++);
        addFormRow(grid, LABEL_TIGHT_SL_PCT, tightSlPctSpinner, row++);
        addFormRow(grid, LABEL_MAX_CONCENTRATION, maxConcentrationSpinner, row++);
        addFormRow(grid, LABEL_STALE_DAYS, staleDaysSpinner, row++);
        addFormRow(grid, LABEL_GTT_EXPIRY_DAYS, gttExpiryDaysSpinner, row++);
        addFormRow(grid, LABEL_MIN_RR_RATIO, minRrRatioSpinner, row);
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
        double positionSize = positionSizeSpinner.getValue();
        double stopLossPct = stopLossPctSpinner.getValue();
        double wideSlPct = wideSlPctSpinner.getValue();
        double tightSlPct = tightSlPctSpinner.getValue();
        double maxConcentration = maxConcentrationSpinner.getValue();
        int staleDays = staleDaysSpinner.getValue();
        int gttExpiryDays = gttExpiryDaysSpinner.getValue();
        double minRrRatio = minRrRatioSpinner.getValue();
        if (editingAccount != null) {
            accountService.update(editingAccount, name, broker, userId, password, pin,
                    positionSize, stopLossPct, wideSlPct, tightSlPct, maxConcentration, staleDays, gttExpiryDays, minRrRatio);
        } else {
            accountService.create(name, broker, userId, password, pin,
                    positionSize, stopLossPct, wideSlPct, tightSlPct, maxConcentration, staleDays, gttExpiryDays, minRrRatio);
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
