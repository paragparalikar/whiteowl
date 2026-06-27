package com.whiteowl.workbench;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.account.service.AccountService;
import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.portfolio.model.BrokerType;
import com.whiteowl.workbench.account.AccountDialog;
import com.whiteowl.workbench.common.ConfirmationDialog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class StatusBar extends HBox {

    private static final String STATUS_BAR_STYLE = "status-bar";
    private static final String ACCOUNT_PILL_STYLE = "status-account-pill";
    private static final String ACCOUNT_LABEL_STYLE = "status-account-label";
    private static final String STATUS_DOT_STYLE = "status-dot";
    private static final String STATUS_DOT_CONNECTED_STYLE = "status-dot-connected";
    private static final String STATUS_DOT_DISCONNECTED_STYLE = "status-dot-disconnected";
    private static final String STATUS_DOT_CONNECTING_STYLE = "status-dot-connecting";
    private static final String MENU_ITEM_ACTIVE_STYLE = "status-menu-active";
    private static final String LABEL_NOT_CONNECTED = "Not Connected";
    private static final String LABEL_CONNECTING = "Connecting...";
    private static final String LABEL_DISCONNECT = "Disconnect";
    private static final String LABEL_NO_ACCOUNTS = "No accounts configured";
    private static final String TOAST_CONNECTED = "Connected to %s";
    private static final String TOAST_DISCONNECTED = "Account disconnected";
    private static final String TOAST_CONNECT_FAILED = "Failed to connect: %s";
    private static final String TOAST_STYLE = "status-toast";
    private static final String TOAST_INFO_STYLE = "status-toast-info";
    private static final String TOAST_SUCCESS_STYLE = "status-toast-success";
    private static final String TOAST_ERROR_STYLE = "status-toast-error";
    private static final String ORDER_BOOK_BUTTON_STYLE = "status-order-book-button";
    private static final String MENU_ITEM_ROW_STYLE = "status-menu-item-row";
    private static final String MENU_ITEM_LABEL_STYLE = "status-menu-item-label";
    private static final String MENU_ACTION_BUTTON_STYLE = "status-menu-action-button";
    private static final String MENU_ACTION_EDIT_STYLE = "status-menu-action-edit";
    private static final String MENU_ACTION_DELETE_STYLE = "status-menu-action-delete";
    private static final String MENU_CREATE_STYLE = "status-menu-create";
    private static final String LABEL_NEW_ACCOUNT = "New Account";
    private static final String MSG_CREATED = "Account created successfully";
    private static final String MSG_UPDATED = "Account updated successfully";
    private static final String MSG_DELETED = "Account deleted";
    private static final String DELETE_TITLE = "Delete Account";
    private static final String DELETE_MESSAGE = "Delete account \"%s\"?";
    private static final String DELETE_CONFIRM = "Delete";
    private static final String ZERODHA_LOGO_PATH = "/images/zerodha-logo.png";
    private static final String FINVASIA_LOGO_PATH = "/images/finvasia-logo.png";
    private static final Map<BrokerType, String> BROKER_LOGO_PATHS = Map.of(
            BrokerType.ZERODHA, ZERODHA_LOGO_PATH,
            BrokerType.FINVASIA, FINVASIA_LOGO_PATH
    );
    private static final int BROKER_LOGO_SIZE = 16;
    private static final double DOT_RADIUS = 4;
    private static final int ICON_SIZE = 12;
    private static final int PILL_GAP = 6;
    private static final int TOAST_ICON_SIZE = 12;
    private static final double TOAST_DISMISS_SECONDS = 5;
    private static StatusBar instance;

    private final ActiveAccountManager accountManager;
    private final AccountService accountService;
    private final Circle statusDot;
    private final Label accountLabel;
    private final HBox accountPill;
    private final Button orderBookButton;
    private final Label toastLabel;
    private final FontIcon toastIcon;
    private Runnable onOrderBookToggle;
    private Timeline toastTimeline;

    public StatusBar(ActiveAccountManager accountManager, AccountService accountService) {
        this.accountManager = accountManager;
        this.accountService = accountService;
        this.statusDot = buildStatusDot();
        this.accountLabel = buildAccountLabel();
        this.accountPill = buildAccountPill();
        this.orderBookButton = buildOrderBookButton();
        this.toastIcon = new FontIcon();
        this.toastIcon.setIconSize(TOAST_ICON_SIZE);
        this.toastLabel = new Label();
        this.toastLabel.setGraphic(toastIcon);
        this.toastLabel.getStyleClass().add(TOAST_STYLE);
        this.toastLabel.setVisible(false);
        this.toastLabel.setManaged(false);
        getStyleClass().add(STATUS_BAR_STYLE);
        setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(toastLabel, spacer, orderBookButton, accountPill);
        accountPill.setOnMouseClicked(e -> showAccountMenu());
        accountManager.addListener(adapter -> Platform.runLater(this::refreshState));
        refreshState();
        instance = this;
    }

    public void setOnOrderBookToggle(Runnable handler) {
        this.onOrderBookToggle = handler;
    }

    public static void showInfo(String message) {
        show(message, ToastLevel.INFO);
    }

    public static void showSuccess(String message) {
        show(message, ToastLevel.SUCCESS);
    }

    public static void showError(String message) {
        show(message, ToastLevel.ERROR);
    }

    private static void show(String message, ToastLevel level) {
        if (instance == null) return;
        if (Platform.isFxApplicationThread()) {
            instance.displayToast(message, level);
        } else {
            Platform.runLater(() -> instance.displayToast(message, level));
        }
    }

    private void displayToast(String message, ToastLevel level) {
        if (toastTimeline != null) toastTimeline.stop();
        toastLabel.setText(message);
        toastLabel.getStyleClass().removeAll(TOAST_INFO_STYLE, TOAST_SUCCESS_STYLE, TOAST_ERROR_STYLE);
        switch (level) {
            case INFO -> {
                toastLabel.getStyleClass().add(TOAST_INFO_STYLE);
                toastIcon.setIconCode(FluentUiRegularAL.INFO_16);
            }
            case SUCCESS -> {
                toastLabel.getStyleClass().add(TOAST_SUCCESS_STYLE);
                toastIcon.setIconCode(FluentUiRegularAL.CHECKMARK_CIRCLE_16);
            }
            case ERROR -> {
                toastLabel.getStyleClass().add(TOAST_ERROR_STYLE);
                toastIcon.setIconCode(FluentUiRegularAL.DISMISS_CIRCLE_16);
            }
        }
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        toastTimeline = new Timeline(new KeyFrame(
                Duration.seconds(TOAST_DISMISS_SECONDS),
                e -> { toastLabel.setVisible(false); toastLabel.setManaged(false); }));
        toastTimeline.play();
    }

    private enum ToastLevel { INFO, SUCCESS, ERROR }

    private Circle buildStatusDot() {
        Circle dot = new Circle(DOT_RADIUS);
        dot.getStyleClass().addAll(STATUS_DOT_STYLE, STATUS_DOT_DISCONNECTED_STYLE);
        return dot;
    }

    private Label buildAccountLabel() {
        Label label = new Label(LABEL_NOT_CONNECTED);
        label.getStyleClass().add(ACCOUNT_LABEL_STYLE);
        return label;
    }

    private HBox buildAccountPill() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.PERSON_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(ACCOUNT_LABEL_STYLE);
        HBox pill = new HBox(PILL_GAP, statusDot, icon, accountLabel);
        pill.setAlignment(Pos.CENTER);
        pill.getStyleClass().add(ACCOUNT_PILL_STYLE);
        return pill;
    }

    private Button buildOrderBookButton() {
        FontIcon icon = new FontIcon(FluentUiRegularMZ.OPEN_16);
        icon.setIconSize(ICON_SIZE);
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add(ORDER_BOOK_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> { if (onOrderBookToggle != null) onOrderBookToggle.run(); });
        return btn;
    }


    private void refreshState() {
        boolean connected = accountManager.isConnected();
        boolean wasConnecting = statusDot.getStyleClass().contains(STATUS_DOT_CONNECTING_STYLE);
        statusDot.getStyleClass().removeAll(
                STATUS_DOT_CONNECTED_STYLE, STATUS_DOT_DISCONNECTED_STYLE, STATUS_DOT_CONNECTING_STYLE);
        if (connected) {
            statusDot.getStyleClass().add(STATUS_DOT_CONNECTED_STYLE);
            accountManager.getActiveAccount().ifPresent(account -> {
                accountLabel.setText(account.getName());
                displayToast(String.format(TOAST_CONNECTED, account.getName()), ToastLevel.SUCCESS);
            });
        } else {
            statusDot.getStyleClass().add(STATUS_DOT_DISCONNECTED_STYLE);
            accountLabel.setText(LABEL_NOT_CONNECTED);
            if (wasConnecting) {
                displayToast(String.format(TOAST_CONNECT_FAILED, "check logs for details"), ToastLevel.ERROR);
            }
        }
    }

    private void showAccountMenu() {
        ContextMenu menu = new ContextMenu();
        List<HBox> accountRows = new java.util.ArrayList<>();
        List<Account> accounts = accountManager.getAccounts();
        if (accounts.isEmpty()) {
            MenuItem noAccounts = new MenuItem(LABEL_NO_ACCOUNTS);
            noAccounts.setDisable(true);
            menu.getItems().add(noAccounts);
        } else {
            String activeId = accountManager.getActiveAccount().map(Account::getId).orElse(null);
            for (Account account : accounts) {
                CustomMenuItem item = buildAccountMenuItem(account, account.getId().equals(activeId), menu, accountRows);
                menu.getItems().add(item);
            }
            if (accountManager.isConnected()) {
                menu.getItems().add(new SeparatorMenuItem());
                FontIcon disconnectIcon = new FontIcon(FluentUiRegularMZ.PLUG_DISCONNECTED_20);
                disconnectIcon.setIconSize(ICON_SIZE);
                MenuItem disconnectItem = new MenuItem(LABEL_DISCONNECT, disconnectIcon);
                disconnectItem.setOnAction(e -> {
                    accountManager.deactivate();
                    displayToast(TOAST_DISCONNECTED, ToastLevel.INFO);
                });
                menu.getItems().add(disconnectItem);
            }
        }
        menu.getItems().add(new SeparatorMenuItem());
        FontIcon addIcon = new FontIcon(FluentUiRegularAL.ADD_16);
        addIcon.setIconSize(ICON_SIZE);
        MenuItem createItem = new MenuItem(LABEL_NEW_ACCOUNT, addIcon);
        createItem.getStyleClass().add(MENU_CREATE_STYLE);
        createItem.setOnAction(e -> onCreateAccount(menu));
        menu.getItems().add(createItem);
        menu.setOnShown(e -> bindRowWidths(menu, accountRows));
        menu.show(accountPill, Side.TOP, 0, 0);
    }

    private void bindRowWidths(ContextMenu menu, List<HBox> rows) {
        double menuWidth = menu.getSkin().getNode().prefWidth(-1);
        double rowWidth = menuWidth - 28;
        for (HBox row : rows) {
            row.setPrefWidth(rowWidth);
        }
    }

    private CustomMenuItem buildAccountMenuItem(Account account, boolean active, ContextMenu menu, List<HBox> rows) {
        String displayName = account.getName() + " (" + account.getUserId() + ")";
        Label label = new Label(active ? "\u2713 " + displayName : displayName);
        label.getStyleClass().add(MENU_ITEM_LABEL_STYLE);
        if (active) {
            label.getStyleClass().add(MENU_ITEM_ACTIVE_STYLE);
        }
        Button editBtn = buildMenuActionButton(FluentUiRegularAL.EDIT_16, MENU_ACTION_EDIT_STYLE);
        editBtn.setOnAction(e -> { menu.hide(); onEditAccount(account); });
        Button deleteBtn = buildMenuActionButton(FluentUiRegularAL.DELETE_16, MENU_ACTION_DELETE_STYLE);
        deleteBtn.setOnAction(e -> { menu.hide(); onDeleteAccount(account); });
        HBox leftGroup = new HBox(PILL_GAP);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        ImageView brokerLogo = createBrokerLogo(account.getBrokerType());
        if (brokerLogo != null) {
            leftGroup.getChildren().add(brokerLogo);
        }
        leftGroup.getChildren().add(label);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(PILL_GAP, leftGroup, spacer, editBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(MENU_ITEM_ROW_STYLE);
        rows.add(row);
        CustomMenuItem item = new CustomMenuItem(row, true);
        item.setOnAction(e -> activateAsync(account));
        return item;
    }

    private ImageView createBrokerLogo(BrokerType brokerType) {
        String path = BROKER_LOGO_PATHS.get(brokerType);
        if (path == null) return null;
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) return null;
        ImageView view = new ImageView(new Image(stream));
        view.setFitHeight(BROKER_LOGO_SIZE);
        view.setFitWidth(BROKER_LOGO_SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private Button buildMenuActionButton(org.kordamp.ikonli.Ikon ikon, String styleClass) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(styleClass);
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add(MENU_ACTION_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void onCreateAccount(ContextMenu menu) {
        menu.hide();
        Window owner = getScene().getWindow();
        AccountDialog dialog = new AccountDialog(accountService);
        dialog.show(owner);
        if (dialog.isSaved()) {
            displayToast(MSG_CREATED, ToastLevel.SUCCESS);
        }
    }

    private void onEditAccount(Account account) {
        Window owner = getScene().getWindow();
        AccountDialog dialog = new AccountDialog(accountService, account);
        dialog.show(owner);
        if (dialog.isSaved()) {
            accountLabel.setText(account.getName());
            displayToast(MSG_UPDATED, ToastLevel.SUCCESS);
        }
    }

    private void onDeleteAccount(Account account) {
        Window owner = getScene().getWindow();
        String message = String.format(DELETE_MESSAGE, account.getName());
        ConfirmationDialog dialog = new ConfirmationDialog(DELETE_TITLE, message, DELETE_CONFIRM);
        dialog.show(owner);
        if (dialog.isConfirmed()) {
            if (accountManager.getActiveAccount().map(Account::getId).orElse("").equals(account.getId())) {
                accountManager.deactivate();
            }
            accountService.delete(account);
            displayToast(MSG_DELETED, ToastLevel.SUCCESS);
        }
    }

    private void activateAsync(Account account) {
        statusDot.getStyleClass().removeAll(
                STATUS_DOT_CONNECTED_STYLE, STATUS_DOT_DISCONNECTED_STYLE, STATUS_DOT_CONNECTING_STYLE);
        statusDot.getStyleClass().add(STATUS_DOT_CONNECTING_STYLE);
        accountLabel.setText(LABEL_CONNECTING);
        CompletableFuture.runAsync(() -> accountManager.activate(account));
    }

}
