package com.whiteowl.workbench.account;

import static com.whiteowl.core.account.model.Account.DEFAULT_GTT_EXPIRY_WARNING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_POSITION_SIZE;
import static com.whiteowl.core.account.model.Account.DEFAULT_STALE_HOLDING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_MIN_RISK_REWARD_RATIO;
import static com.whiteowl.core.account.model.Account.DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.collection.ObservableListMerger.merge;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.portfolio.model.BrokerType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public final class AccountsPane extends VBox {

    private static final String PANE_STYLE = "accounts-pane";
    private static final String CARD_STYLE = "accounts-card";
    private static final String HEADER_BAR_STYLE = "accounts-header-bar";
    private static final String HEADER_TITLE_STYLE = "accounts-header-title";
    private static final String HEADER_COUNT_STYLE = "accounts-header-count";
    private static final String TABLE_STYLE = "accounts-table";
    private static final String EMPTY_STATE_STYLE = "accounts-empty-state";
    private static final String EMPTY_ICON_STYLE = "accounts-empty-icon";
    private static final String EMPTY_TEXT_STYLE = "accounts-empty-text";
    private static final String FORM_CARD_STYLE = "accounts-form-card";
    private static final String FORM_STYLE = "accounts-form";
    private static final String FORM_LABEL_STYLE = "accounts-form-label";
    private static final String FORM_FIELD_STYLE = "accounts-form-field";
    private static final String FORM_COMBO_STYLE = "accounts-form-combo";
    private static final String FORM_HEADER_STYLE = "accounts-form-header";
    private static final String FORM_HEADER_ICON_STYLE = "accounts-form-header-icon";
    private static final String FORM_MESSAGE_STYLE = "accounts-form-message";
    private static final String FORM_ERROR_STYLE = "accounts-form-error";
    private static final String FORM_SUCCESS_STYLE = "accounts-form-success";
    private static final String FORM_DIVIDER_STYLE = "accounts-form-divider";
    private static final String FORM_BUTTON_ROW_STYLE = "accounts-form-button-row";
    private static final String SAVE_BUTTON_STYLE = "accounts-save-button";
    private static final String SAVE_ICON_STYLE = "accounts-save-icon";
    private static final String CANCEL_BUTTON_STYLE = "accounts-cancel-button";
    private static final String CANCEL_ICON_STYLE = "accounts-cancel-icon";
    private static final String ADD_BUTTON_STYLE = "accounts-add-button";
    private static final String ADD_ICON_STYLE = "accounts-add-icon";
    private static final String EDIT_BUTTON_STYLE = "accounts-edit-button";
    private static final String EDIT_ICON_STYLE = "accounts-edit-icon";
    private static final String DELETE_BUTTON_STYLE = "accounts-delete-button";
    private static final String DELETE_ICON_STYLE = "accounts-delete-icon";
    private static final String ACTION_CELL_STYLE = "accounts-action-cell";
    private static final String BROKER_BADGE_STYLE = "accounts-broker-badge";
    private static final String BROKER_ZERODHA_STYLE = "accounts-broker-zerodha";
    private static final String BROKER_FINVASIA_STYLE = "accounts-broker-finvasia";
    private static final String CONTENT_STYLE = "accounts-content";
    private static final String COL_BROKER = "Broker";
    private static final String COL_NAME = "Name";
    private static final String COL_USER_ID = "User ID";
    private static final String COL_ACTIONS = "";
    private static final String TITLE_ACCOUNTS = "Broker Accounts";
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
    private static final String BUTTON_CANCEL = "Cancel";
    private static final String TOOLTIP_ADD = "Add new account";
    private static final String TOOLTIP_EDIT = "Edit account";
    private static final String TOOLTIP_DELETE = "Delete account";
    private static final double CARD_MAX_WIDTH = 480;
    private static final String EMPTY_TITLE = "No accounts configured";
    private static final String EMPTY_HINT = "Click + to add your first broker account";
    private static final String MSG_CREATED = "Account created successfully";
    private static final String MSG_UPDATED = "Account updated successfully";
    private static final String MSG_DELETED = "Account deleted";
    private static final int ICON_SIZE = 14;
    private static final int SMALL_ICON_SIZE = 12;
    private static final int FORM_SPACING = 10;
    private static final int FORM_HGAP = 10;
    private static final int FORM_VGAP = 8;
    private static final int ACTION_GAP = 2;
    private static final int SECTION_GAP = 8;
    private static final double LABEL_COL_PERCENT = 22;
    private static final double FIELD_COL_PERCENT = 78;

    private final AccountService accountService;
    private final ObservableList<Account> tableData;
    private final TableView<Account> tableView;
    private final Label countLabel;
    private final Label formHeader;
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
    private final Label messageLabel;
    private final VBox formCard;
    private Account editingAccount;

    public AccountsPane(AccountService accountService) {
        this.accountService = accountService;
        this.tableData = FXCollections.observableArrayList(accountService.getAccounts());
        this.countLabel = buildCountLabel();
        this.tableView = buildTable();
        this.formHeader = buildFormHeader();
        this.nameField = buildTextField();
        this.brokerCombo = buildBrokerCombo();
        this.userIdField = buildTextField();
        this.passwordField = buildPasswordField();
        this.pinField = buildPasswordField();
        this.positionSizeSpinner = buildPositionSizeSpinner();
        this.stopLossPctSpinner = buildStopLossPctSpinner();
        this.wideSlPctSpinner = buildDoubleSpinner(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT, DEFAULT_WIDE_STOP_LOSS_PERCENTAGE, STOP_LOSS_PCT_STEP);
        this.tightSlPctSpinner = buildDoubleSpinner(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT, DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE, STOP_LOSS_PCT_STEP);
        this.maxConcentrationSpinner = buildDoubleSpinner(MIN_STOP_LOSS_PCT, 100.0, DEFAULT_MAX_CONCENTRATION_PERCENTAGE, 1.0);
        this.staleDaysSpinner = buildIntSpinner(MIN_DAYS, MAX_STALE_DAYS, DEFAULT_STALE_HOLDING_DAYS);
        this.gttExpiryDaysSpinner = buildIntSpinner(MIN_DAYS, MAX_EXPIRY_DAYS, DEFAULT_GTT_EXPIRY_WARNING_DAYS);
        this.minRrRatioSpinner = buildDoubleSpinner(MIN_RR_RATIO, MAX_RR_RATIO, DEFAULT_MIN_RISK_REWARD_RATIO, RR_RATIO_STEP);
        this.messageLabel = buildMessageLabel();
        this.formCard = buildFormCard();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    private void buildLayout() {
        HBox headerBar = buildHeaderBar();
        VBox tableCard = new VBox(headerBar, tableView);
        tableCard.getStyleClass().add(CARD_STYLE);
        tableCard.setMaxWidth(CARD_MAX_WIDTH);
        formCard.setMaxWidth(CARD_MAX_WIDTH);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox content = new VBox(SECTION_GAP, tableCard, formCard);
        content.getStyleClass().add(CONTENT_STYLE);
        content.setAlignment(Pos.TOP_CENTER);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("accounts-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private HBox buildHeaderBar() {
        FontIcon titleIcon = new FontIcon(FluentUiRegularMZ.PERSON_ACCOUNTS_24);
        titleIcon.setIconSize(ICON_SIZE);
        titleIcon.getStyleClass().add(HEADER_TITLE_STYLE);
        Label title = new Label(TITLE_ACCOUNTS);
        title.getStyleClass().add(HEADER_TITLE_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = buildAddButton();
        HBox bar = new HBox(6, titleIcon, title, countLabel, spacer, addButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add(HEADER_BAR_STYLE);
        return bar;
    }

    private Label buildCountLabel() {
        Label label = new Label(String.valueOf(tableData.size()));
        label.getStyleClass().add(HEADER_COUNT_STYLE);
        return label;
    }

    @SuppressWarnings("unchecked")
    private TableView<Account> buildTable() {
        TableView<Account> table = new TableView<>(tableData);
        table.getStyleClass().add(TABLE_STYLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<Account, String> brokerCol = buildBrokerColumn();
        TableColumn<Account, String> nameCol = buildNameColumn();
        TableColumn<Account, String> userIdCol = buildUserIdColumn();
        TableColumn<Account, Void> actionsCol = buildActionsColumn();
        table.getColumns().addAll(brokerCol, nameCol, userIdCol, actionsCol);
        VBox emptyState = buildEmptyState();
        table.setPlaceholder(emptyState);
        return table;
    }

    private VBox buildEmptyState() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.PERSON_ACCOUNTS_24);
        icon.setIconSize(32);
        icon.getStyleClass().add(EMPTY_ICON_STYLE);
        Label title = new Label(EMPTY_TITLE);
        title.getStyleClass().addAll(EMPTY_TEXT_STYLE, HEADER_TITLE_STYLE);
        Label hint = new Label(EMPTY_HINT);
        hint.getStyleClass().add(EMPTY_TEXT_STYLE);
        VBox box = new VBox(6, icon, title, hint);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add(EMPTY_STATE_STYLE);
        return box;
    }

    private TableColumn<Account, String> buildBrokerColumn() {
        TableColumn<Account, String> col = new TableColumn<>(COL_BROKER);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBrokerType().name()));
        col.setCellFactory(column -> new BrokerBadgeCell());
        col.setSortable(false);
        col.setReorderable(false);
        col.setMinWidth(90);
        col.setMaxWidth(110);
        return col;
    }

    private TableColumn<Account, String> buildNameColumn() {
        TableColumn<Account, String> col = new TableColumn<>(COL_NAME);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        col.setSortable(false);
        col.setReorderable(false);
        return col;
    }

    private TableColumn<Account, String> buildUserIdColumn() {
        TableColumn<Account, String> col = new TableColumn<>(COL_USER_ID);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserId()));
        col.setSortable(false);
        col.setReorderable(false);
        return col;
    }

    private TableColumn<Account, Void> buildActionsColumn() {
        TableColumn<Account, Void> col = new TableColumn<>(COL_ACTIONS);
        col.setSortable(false);
        col.setReorderable(false);
        col.setMaxWidth(64);
        col.setMinWidth(64);
        col.setCellFactory(column -> new ActionCell());
        return col;
    }

    private Button buildAddButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ADD_16);
        icon.setIconSize(SMALL_ICON_SIZE);
        icon.getStyleClass().add(ADD_ICON_STYLE);
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add(ADD_BUTTON_STYLE);
        button.setTooltip(new Tooltip(TOOLTIP_ADD));
        button.setOnAction(e -> showNewForm());
        return button;
    }

    private VBox buildFormCard() {
        VBox form = buildForm();
        VBox card = new VBox(form);
        card.getStyleClass().add(FORM_CARD_STYLE);
        card.setVisible(false);
        card.setManaged(false);
        return card;
    }

    private VBox buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(FORM_HGAP);
        grid.setVgap(FORM_VGAP);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(LABEL_COL_PERCENT);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setPercentWidth(FIELD_COL_PERCENT);
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);
        int row = 0;
        grid.add(buildLabel(LABEL_NAME), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(buildLabel(LABEL_BROKER), 0, row);
        grid.add(brokerCombo, 1, row++);
        grid.add(buildLabel(LABEL_USER_ID), 0, row);
        grid.add(userIdField, 1, row++);
        grid.add(buildLabel(LABEL_PASSWORD), 0, row);
        grid.add(passwordField, 1, row++);
        grid.add(buildLabel(LABEL_PIN), 0, row);
        grid.add(pinField, 1, row++);
        grid.add(buildLabel(LABEL_POSITION_SIZE), 0, row);
        grid.add(positionSizeSpinner, 1, row++);
        grid.add(buildLabel(LABEL_STOP_LOSS_PCT), 0, row);
        grid.add(stopLossPctSpinner, 1, row++);
        grid.add(buildLabel(LABEL_WIDE_SL_PCT), 0, row);
        grid.add(wideSlPctSpinner, 1, row++);
        grid.add(buildLabel(LABEL_TIGHT_SL_PCT), 0, row);
        grid.add(tightSlPctSpinner, 1, row++);
        grid.add(buildLabel(LABEL_MAX_CONCENTRATION), 0, row);
        grid.add(maxConcentrationSpinner, 1, row++);
        grid.add(buildLabel(LABEL_STALE_DAYS), 0, row);
        grid.add(staleDaysSpinner, 1, row++);
        grid.add(buildLabel(LABEL_GTT_EXPIRY_DAYS), 0, row);
        grid.add(gttExpiryDaysSpinner, 1, row++);
        grid.add(buildLabel(LABEL_MIN_RR_RATIO), 0, row);
        grid.add(minRrRatioSpinner, 1, row);
        Region divider = new Region();
        divider.getStyleClass().add(FORM_DIVIDER_STYLE);
        HBox buttonRow = buildFormButtons();
        VBox form = new VBox(FORM_SPACING, buildFormHeaderRow(), grid, messageLabel, divider, buttonRow);
        form.getStyleClass().add(FORM_STYLE);
        return form;
    }

    private HBox buildFormHeaderRow() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.EDIT_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(FORM_HEADER_ICON_STYLE);
        HBox row = new HBox(6, icon, formHeader);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildFormButtons() {
        FontIcon saveIcon = new FontIcon(FluentUiRegularMZ.SAVE_20);
        saveIcon.setIconSize(ICON_SIZE);
        saveIcon.getStyleClass().add(SAVE_ICON_STYLE);
        Button saveButton = new Button(BUTTON_SAVE, saveIcon);
        saveButton.getStyleClass().add(SAVE_BUTTON_STYLE);
        saveButton.setOnAction(e -> onSave());
        FontIcon cancelIcon = new FontIcon(FluentUiRegularAL.DISMISS_16);
        cancelIcon.setIconSize(SMALL_ICON_SIZE);
        cancelIcon.getStyleClass().add(CANCEL_ICON_STYLE);
        Button cancelButton = new Button(BUTTON_CANCEL, cancelIcon);
        cancelButton.getStyleClass().add(CANCEL_BUTTON_STYLE);
        cancelButton.setOnAction(e -> hideForm());
        HBox row = new HBox(SECTION_GAP, saveButton, cancelButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(FORM_BUTTON_ROW_STYLE);
        return row;
    }

    private Label buildFormHeader() {
        Label label = new Label(HEADER_NEW);
        label.getStyleClass().add(FORM_HEADER_STYLE);
        return label;
    }

    private Label buildLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(FORM_LABEL_STYLE);
        return label;
    }

    private TextField buildTextField() {
        TextField field = new TextField();
        field.getStyleClass().add(FORM_FIELD_STYLE);
        return field;
    }

    private PasswordField buildPasswordField() {
        PasswordField field = new PasswordField();
        field.getStyleClass().add(FORM_FIELD_STYLE);
        return field;
    }

    private Spinner<Double> buildPositionSizeSpinner() {
        Spinner<Double> spinner = new Spinner<>(MIN_POSITION_SIZE, MAX_POSITION_SIZE,
                DEFAULT_POSITION_SIZE, POSITION_SIZE_STEP);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private Spinner<Double> buildStopLossPctSpinner() {
        Spinner<Double> spinner = new Spinner<>(MIN_STOP_LOSS_PCT, MAX_STOP_LOSS_PCT,
                DEFAULT_STOP_LOSS_PERCENTAGE, STOP_LOSS_PCT_STEP);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private Spinner<Double> buildDoubleSpinner(double min, double max, double initial, double step) {
        Spinner<Double> spinner = new Spinner<>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private Spinner<Integer> buildIntSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private ComboBox<BrokerType> buildBrokerCombo() {
        ComboBox<BrokerType> combo = new ComboBox<>();
        combo.getItems().addAll(BrokerType.values());
        combo.setValue(BrokerType.ZERODHA);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(FORM_COMBO_STYLE);
        return combo;
    }

    private Label buildMessageLabel() {
        Label label = new Label();
        label.getStyleClass().add(FORM_MESSAGE_STYLE);
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void showNewForm() {
        editingAccount = null;
        formHeader.setText(HEADER_NEW);
        clearForm();
        showForm();
    }

    private void showEditForm(Account account) {
        editingAccount = account;
        formHeader.setText(HEADER_EDIT);
        nameField.setText(account.getName());
        brokerCombo.setValue(account.getBrokerType());
        userIdField.setText(account.getUserId());
        passwordField.setText(account.getPassword());
        pinField.setText(account.getPin());
        positionSizeSpinner.getValueFactory().setValue(account.getPositionSize());
        stopLossPctSpinner.getValueFactory().setValue(account.getStopLossPercentage());
        wideSlPctSpinner.getValueFactory().setValue(account.getWideStopLossPercentage());
        tightSlPctSpinner.getValueFactory().setValue(account.getTightStopLossPercentage());
        maxConcentrationSpinner.getValueFactory().setValue(account.getMaxConcentrationPercentage());
        staleDaysSpinner.getValueFactory().setValue(account.getStaleHoldingDays());
        gttExpiryDaysSpinner.getValueFactory().setValue(account.getGttExpiryWarningDays());
        minRrRatioSpinner.getValueFactory().setValue(account.getMinRiskRewardRatio());
        clearMessage();
        showForm();
    }

    private void showForm() {
        formCard.setVisible(true);
        formCard.setManaged(true);
        nameField.requestFocus();
    }

    private void hideForm() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        editingAccount = null;
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        brokerCombo.setValue(BrokerType.ZERODHA);
        userIdField.clear();
        passwordField.clear();
        pinField.clear();
        positionSizeSpinner.getValueFactory().setValue(DEFAULT_POSITION_SIZE);
        stopLossPctSpinner.getValueFactory().setValue(DEFAULT_STOP_LOSS_PERCENTAGE);
        wideSlPctSpinner.getValueFactory().setValue(DEFAULT_WIDE_STOP_LOSS_PERCENTAGE);
        tightSlPctSpinner.getValueFactory().setValue(DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE);
        maxConcentrationSpinner.getValueFactory().setValue(DEFAULT_MAX_CONCENTRATION_PERCENTAGE);
        staleDaysSpinner.getValueFactory().setValue(DEFAULT_STALE_HOLDING_DAYS);
        gttExpiryDaysSpinner.getValueFactory().setValue(DEFAULT_GTT_EXPIRY_WARNING_DAYS);
        minRrRatioSpinner.getValueFactory().setValue(DEFAULT_MIN_RISK_REWARD_RATIO);
        clearMessage();
    }

    private void onSave() {
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
            showSuccess(MSG_UPDATED);
        } else {
            accountService.create(name, broker, userId, password, pin,
                    positionSize, stopLossPct, wideSlPct, tightSlPct, maxConcentration, staleDays, gttExpiryDays, minRrRatio);
            showSuccess(MSG_CREATED);
        }
        refreshTable();
        hideForm();
    }

    private void onDelete(Account account) {
        accountService.delete(account);
        refreshTable();
        showSuccess(MSG_DELETED);
        if (editingAccount != null && editingAccount.getId().equals(account.getId())) {
            hideForm();
        }
    }

    private void refreshTable() {
        merge(tableData, accountService.getAccounts(), Account::getId);
        countLabel.setText(String.valueOf(tableData.size()));
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll(FORM_SUCCESS_STYLE);
        if (!messageLabel.getStyleClass().contains(FORM_ERROR_STYLE)) {
            messageLabel.getStyleClass().add(FORM_ERROR_STYLE);
        }
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll(FORM_ERROR_STYLE);
        if (!messageLabel.getStyleClass().contains(FORM_SUCCESS_STYLE)) {
            messageLabel.getStyleClass().add(FORM_SUCCESS_STYLE);
        }
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.getStyleClass().removeAll(FORM_ERROR_STYLE, FORM_SUCCESS_STYLE);
    }

    private static final class BrokerBadgeCell extends TableCell<Account, String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(item);
            badge.getStyleClass().add(BROKER_BADGE_STYLE);
            if (BrokerType.ZERODHA.name().equals(item)) {
                badge.getStyleClass().add(BROKER_ZERODHA_STYLE);
            } else {
                badge.getStyleClass().add(BROKER_FINVASIA_STYLE);
            }
            setGraphic(badge);
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

    }

    private final class ActionCell extends TableCell<Account, Void> {

        private final HBox container;

        ActionCell() {
            FontIcon editIcon = new FontIcon(FluentUiRegularAL.EDIT_16);
            editIcon.setIconSize(SMALL_ICON_SIZE);
            editIcon.getStyleClass().add(EDIT_ICON_STYLE);
            Button editButton = new Button();
            editButton.setGraphic(editIcon);
            editButton.getStyleClass().add(EDIT_BUTTON_STYLE);
            editButton.setTooltip(new Tooltip(TOOLTIP_EDIT));
            editButton.setOnAction(e -> {
                Account account = getTableView().getItems().get(getIndex());
                showEditForm(account);
            });
            FontIcon deleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            deleteIcon.setIconSize(SMALL_ICON_SIZE);
            deleteIcon.getStyleClass().add(DELETE_ICON_STYLE);
            Button deleteButton = new Button();
            deleteButton.setGraphic(deleteIcon);
            deleteButton.getStyleClass().add(DELETE_BUTTON_STYLE);
            deleteButton.setTooltip(new Tooltip(TOOLTIP_DELETE));
            deleteButton.setOnAction(e -> {
                Account account = getTableView().getItems().get(getIndex());
                onDelete(account);
            });
            container = new HBox(ACTION_GAP, editButton, deleteButton);
            container.setAlignment(Pos.CENTER);
            container.getStyleClass().add(ACTION_CELL_STYLE);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : container);
        }

    }

}
