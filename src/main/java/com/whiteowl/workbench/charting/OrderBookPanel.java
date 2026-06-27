package com.whiteowl.workbench.charting;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.order.model.LimitType;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.order.model.OrderStatus;
import com.whiteowl.core.order.model.Product;
import com.whiteowl.core.order.model.Validity;
import com.whiteowl.core.order.model.Variety;
import com.whiteowl.core.portfolio.model.Funds;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.workbench.StatusBar;
import com.whiteowl.workbench.common.ConfirmationDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public final class OrderBookPanel extends VBox {

    private static final String PANEL_STYLE = "order-book-panel";
    private static final String TAB_PANE_STYLE = "order-book-tab-pane";
    private static final String TABLE_STYLE = "order-book-table";
    private static final String HEADER_BUTTON_STYLE = "order-book-header-button";
    private static final String ACTION_BUTTON_STYLE = "order-book-action-button";
    private static final String ACTION_DELETE_STYLE = "order-book-action-delete";
    private static final String ACTION_EXIT_STYLE = "order-book-action-exit";
    private static final String FUNDS_GRID_STYLE = "order-book-funds-grid";
    private static final String FUNDS_LABEL_STYLE = "order-book-funds-label";
    private static final String FUNDS_VALUE_STYLE = "order-book-funds-value";
    private static final String TAB_ORDERS = "Orders";
    private static final String TAB_GTTS = "GTTs";
    private static final String TAB_POSITIONS = "Positions";
    private static final String TAB_HOLDINGS = "Holdings";
    private static final String TAB_FUNDS = "Funds";
    private static final String EMPTY_NO_ACCOUNT = "Connect an account to view orders";
    private static final String EMPTY_NO_ORDERS = "No orders for today";
    private static final String EMPTY_NO_GTTS = "No GTT orders";
    private static final String EMPTY_NO_POSITIONS = "No open positions";
    private static final String EMPTY_NO_HOLDINGS = "No holdings";
    private static final String LOADING_TEXT = "Loading...";
    private static final String REFRESHED_ORDERS = "Loaded %d orders";
    private static final String REFRESH_ORDERS_FAILED = "Failed to refresh orders: %s";
    private static final String REFRESHED_GTTS = "Loaded %d GTTs";
    private static final String REFRESH_GTTS_FAILED = "Failed to refresh GTTs: %s";
    private static final String REFRESHED_POSITIONS = "Loaded %d positions";
    private static final String REFRESH_POSITIONS_FAILED = "Failed to refresh positions: %s";
    private static final String REFRESHED_HOLDINGS = "Loaded %d holdings";
    private static final String REFRESH_HOLDINGS_FAILED = "Failed to refresh holdings: %s";
    private static final String REFRESH_FUNDS_FAILED = "Failed to refresh funds: %s";
    private static final String GTT_CANCEL_TITLE = "Cancel GTT";
    private static final String GTT_CANCEL_MESSAGE = "Cancel GTT #%d at %.2f?";
    private static final String GTT_CANCEL_CONFIRM = "Cancel GTT";
    private static final String GTT_CANCELLED = "GTT cancelled";
    private static final String GTT_CANCEL_FAILED = "Failed to cancel GTT: %s";
    private static final String ORDER_CANCEL_TITLE = "Cancel Order";
    private static final String ORDER_CANCEL_MESSAGE = "Cancel this %s %s order?";
    private static final String ORDER_CANCEL_CONFIRM = "Cancel Order";
    private static final String ORDER_CANCELLED = "Order cancelled";
    private static final String ORDER_CANCEL_FAILED = "Failed to cancel order: %s";
    private static final String ORDER_UPDATED = "Order updated";
    private static final String ORDER_UPDATE_FAILED = "Failed to update order: %s";
    private static final String GTT_UPDATED = "GTT updated";
    private static final String GTT_UPDATE_FAILED = "Failed to update GTT: %s";
    private static final String EXIT_POSITION_TITLE = "Exit Position";
    private static final String EXIT_POSITION_MESSAGE = "Exit %s position of %d qty at market?";
    private static final String EXIT_POSITION_CONFIRM = "Exit Position";
    private static final String EXIT_POSITION_PLACED = "Exit order placed for position";
    private static final String EXIT_POSITION_FAILED = "Failed to exit position: %s";
    private static final String EXIT_HOLDING_TITLE = "Exit Holding";
    private static final String EXIT_HOLDING_MESSAGE = "Sell %d qty of %s at market?";
    private static final String EXIT_HOLDING_CONFIRM = "Exit Holding";
    private static final String EXIT_HOLDING_PLACED = "Exit order placed for holding";
    private static final String EXIT_HOLDING_FAILED = "Failed to exit holding: %s";
    private static final String COL_SCRIP = "Scrip";
    private static final String COL_SIDE = "Side";
    private static final String COL_TYPE = "Type";
    private static final String COL_PRODUCT = "Product";
    private static final String COL_QTY = "Qty";
    private static final String COL_PRICE = "Price";
    private static final String COL_TRIGGER = "Trigger";
    private static final String COL_AVG_PRICE = "Avg Price";
    private static final String COL_ORDER_PRICE = "Order Price";
    private static final String COL_LTP = "LTP";
    private static final String COL_PNL = "P&L";
    private static final String COL_EXCHANGE = "Exchange";
    private static final String COL_ISIN = "ISIN";
    private static final String COL_CLOSE = "Close";
    private static final String COL_DAY_CHG = "Day Chg%";
    private static final String COL_T1_QTY = "T1 Qty";
    private static final String COL_CURRENT_VALUE = "Value";
    private static final String COL_STATUS = "Status";
    private static final String COL_MESSAGE = "Message";
    private static final String COL_CREATED = "Created";
    private static final String COL_ACTIONS = "";
    private static final String DASH = "\u2014";
    private static final int ICON_SIZE = 14;
    private static final int ACTION_ICON_SIZE = 12;
    private static final int ACTION_COLUMN_WIDTH = 70;
    private static final int CORNER_RADIUS = 8;
    private static final int TAB_HEADER_PADDING = 4;
    private static final int TAB_HEADER_HEIGHT = 22;
    private static final int TAB_ICON_SIZE = 12;
    private static final String TAB_ICON_STYLE = "order-book-tab-icon";
    private static final String COLOR_BUY = "#6a8759";
    private static final String COLOR_SELL = "#ff6b68";
    private static final String COLOR_OPEN = "#6897bb";
    private static final String COLOR_WARNING = "#cc7832";
    private static final String COLOR_SUCCESS = "#6a8759";
    private static final String COLOR_MUTED = "#999999";
    private static final String COLOR_ERROR = "#ff6b68";
    private static final String COLOR_POSITIVE = "#6a8759";
    private static final String COLOR_NEGATIVE = "#ff6b68";
    private static final String STYLE_TEXT_FILL = "-fx-text-fill: %s;";
    private static final String FUNDS_EQUITY_CASH = "Equity Available Cash";
    private static final String FUNDS_EQUITY_COLLATERAL = "Equity Collateral";
    private static final String FUNDS_EQUITY_INTRADAY = "Intraday Payin";
    private static final String FUNDS_EQUITY_OPENING = "Opening Balance";
    private static final String FUNDS_EQUITY_NET = "Equity Net";
    private static final String FUNDS_EQUITY_DEBITS = "Debits";
    private static final String FUNDS_EQUITY_EXPOSURE = "Exposure";
    private static final String FUNDS_EQUITY_SPAN = "SPAN";
    private static final String FUNDS_EQUITY_OPTION_PREMIUM = "Option Premium";
    private static final String FUNDS_EQUITY_PAYOUT = "Payout";
    private static final String FUNDS_COMMODITY_CASH = "Commodity Available Cash";
    private static final String FUNDS_COMMODITY_COLLATERAL = "Commodity Collateral";
    private static final String FUNDS_COMMODITY_NET = "Commodity Net";

    private final TableView<Order> ordersTable;
    private final TableView<GttOrder> gttTable;
    private final TableView<Position> positionsTable;
    private final TableView<Holding> holdingsTable;
    private final GridPane fundsGrid;
    private final TabPane tabPane;
    private ChartOrderService chartOrderService;
    private Consumer<String> onNavigateToScrip;
    private Runnable onCloseRequested;

    public OrderBookPanel() {
        getStyleClass().add(PANEL_STYLE);
        ordersTable = buildOrdersTable();
        gttTable = buildGttTable();
        positionsTable = buildPositionsTable();
        holdingsTable = buildHoldingsTable();
        fundsGrid = buildFundsGrid();
        tabPane = buildTabPane();
        HBox headerButtons = buildHeaderButtons();
        headerButtons.setPadding(new Insets(TAB_HEADER_PADDING, TAB_HEADER_PADDING, 0, 0));
        headerButtons.setMaxHeight(TAB_HEADER_HEIGHT);
        headerButtons.setMaxWidth(HBox.USE_PREF_SIZE);
        StackPane layered = new StackPane(tabPane, headerButtons);
        StackPane.setAlignment(headerButtons, Pos.TOP_RIGHT);
        headerButtons.setPickOnBounds(false);
        getChildren().add(layered);
        VBox.setVgrow(layered, Priority.ALWAYS);
        applyClip();
    }

    private void applyClip() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
    }

    public void setChartOrderService(ChartOrderService service) {
        this.chartOrderService = service;
        if (service != null) {
            refreshAll();
        } else {
            clearAll();
        }
    }

    public void setOnNavigateToScrip(Consumer<String> handler) {
        this.onNavigateToScrip = handler;
    }

    public void setOnCloseRequested(Runnable handler) {
        this.onCloseRequested = handler;
    }

    public void refreshAll() {
        refreshOrders();
        refreshGtts();
        refreshPositions();
        refreshHoldings();
        refreshFunds();
    }

    public void refreshOrders() {
        if (chartOrderService == null) return;
        ordersTable.setPlaceholder(new Label(LOADING_TEXT));
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchOrders();
            } catch (Exception ex) {
                log.error("Failed to fetch orders", ex);
                StatusBar.showError(String.format(REFRESH_ORDERS_FAILED, extractMessage(ex)));
                return List.<Order>of();
            }
        }).thenAccept(orders -> Platform.runLater(() -> {
            ordersTable.setItems(FXCollections.observableArrayList(orders));
            ordersTable.setPlaceholder(new Label(EMPTY_NO_ORDERS));
        }));
    }

    public void refreshGtts() {
        if (chartOrderService == null) return;
        gttTable.setPlaceholder(new Label(LOADING_TEXT));
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchGtts();
            } catch (Exception ex) {
                log.error("Failed to fetch GTTs", ex);
                StatusBar.showError(String.format(REFRESH_GTTS_FAILED, extractMessage(ex)));
                return List.<GttOrder>of();
            }
        }).thenAccept(gtts -> Platform.runLater(() -> {
            gttTable.setItems(FXCollections.observableArrayList(gtts));
            gttTable.setPlaceholder(new Label(EMPTY_NO_GTTS));
        }));
    }

    public void refreshPositions() {
        if (chartOrderService == null) return;
        positionsTable.setPlaceholder(new Label(LOADING_TEXT));
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchPositions();
            } catch (Exception ex) {
                log.error("Failed to fetch positions", ex);
                StatusBar.showError(String.format(REFRESH_POSITIONS_FAILED, extractMessage(ex)));
                return List.<Position>of();
            }
        }).thenAccept(positions -> Platform.runLater(() -> {
            positionsTable.setItems(FXCollections.observableArrayList(positions));
            positionsTable.setPlaceholder(new Label(EMPTY_NO_POSITIONS));
        }));
    }

    public void refreshHoldings() {
        if (chartOrderService == null) return;
        holdingsTable.setPlaceholder(new Label(LOADING_TEXT));
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchHoldings();
            } catch (Exception ex) {
                log.error("Failed to fetch holdings", ex);
                StatusBar.showError(String.format(REFRESH_HOLDINGS_FAILED, extractMessage(ex)));
                return List.<Holding>of();
            }
        }).thenAccept(holdings -> Platform.runLater(() -> {
            holdingsTable.setItems(FXCollections.observableArrayList(holdings));
            holdingsTable.setPlaceholder(new Label(EMPTY_NO_HOLDINGS));
        }));
    }

    public void refreshFunds() {
        if (chartOrderService == null) return;
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchFunds();
            } catch (Exception ex) {
                log.error("Failed to fetch funds", ex);
                StatusBar.showError(String.format(REFRESH_FUNDS_FAILED, extractMessage(ex)));
                return Funds.builder().build();
            }
        }).thenAccept(funds -> Platform.runLater(() -> populateFundsGrid(funds)));
    }

    private void clearAll() {
        ordersTable.setItems(FXCollections.observableArrayList());
        gttTable.setItems(FXCollections.observableArrayList());
        positionsTable.setItems(FXCollections.observableArrayList());
        holdingsTable.setItems(FXCollections.observableArrayList());
        ordersTable.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        gttTable.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        positionsTable.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        holdingsTable.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        populateFundsGrid(Funds.builder().build());
    }

    private HBox buildHeaderButtons() {
        Button refreshBtn = new Button();
        FontIcon refreshIcon = new FontIcon(FluentUiRegularAL.ARROW_SYNC_12);
        refreshIcon.setIconSize(ICON_SIZE);
        refreshBtn.setGraphic(refreshIcon);
        refreshBtn.getStyleClass().add(HEADER_BUTTON_STYLE);
        refreshBtn.setFocusTraversable(false);
        refreshBtn.setOnAction(e -> refreshAll());
        Button closeBtn = new Button();
        FontIcon closeIcon = new FontIcon(FluentUiRegularAL.DISMISS_16);
        closeIcon.setIconSize(ICON_SIZE);
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().add(HEADER_BUTTON_STYLE);
        closeBtn.setFocusTraversable(false);
        closeBtn.setOnAction(e -> { if (onCloseRequested != null) onCloseRequested.run(); });
        HBox buttons = new HBox(4, refreshBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPickOnBounds(false);
        return buttons;
    }

    private TabPane buildTabPane() {
        TabPane pane = new TabPane();
        pane.getStyleClass().add(TAB_PANE_STYLE);
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        ScrollPane fundsScroll = new ScrollPane(fundsGrid);
        fundsScroll.setFitToWidth(true);
        fundsScroll.setStyle("-fx-background-color: transparent;");
        pane.getTabs().addAll(
                createTab(TAB_ORDERS, FluentUiRegularAL.LIST_20, ordersTable),
                createTab(TAB_GTTS, FluentUiRegularAL.CLOCK_ALARM_20, gttTable),
                createTab(TAB_POSITIONS, FluentUiRegularAL.ARROW_TRENDING_20, positionsTable),
                createTab(TAB_HOLDINGS, FluentUiRegularAL.BRIEFCASE_20, holdingsTable),
                createTab(TAB_FUNDS, FluentUiRegularMZ.MONEY_20, fundsScroll));
        return pane;
    }

    private Tab createTab(String title, Ikon icon, Node content) {
        Tab tab = new Tab(title, content);
        FontIcon tabIcon = new FontIcon(icon);
        tabIcon.setIconSize(TAB_ICON_SIZE);
        tabIcon.getStyleClass().add(TAB_ICON_STYLE);
        tab.setGraphic(tabIcon);
        return tab;
    }

    @SuppressWarnings("unchecked")
    private TableView<Order> buildOrdersTable() {
        TableView<Order> table = new TableView<>();
        table.getStyleClass().add(TABLE_STYLE);
        table.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Order, String> scripCol = new TableColumn<>(COL_SCRIP);
        scripCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getScripId()));
        scripCol.setPrefWidth(100);
        TableColumn<Order, OrderSide> sideCol = new TableColumn<>(COL_SIDE);
        sideCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSide()));
        sideCol.setCellFactory(col -> createSideCell());
        sideCol.setPrefWidth(50);
        TableColumn<Order, String> typeCol = new TableColumn<>(COL_TYPE);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLimitType() != null ? c.getValue().getLimitType().name() : DASH));
        typeCol.setPrefWidth(60);
        TableColumn<Order, String> qtyCol = new TableColumn<>(COL_QTY);
        qtyCol.setCellValueFactory(c -> {
            Order o = c.getValue();
            String text = o.getFilledQuantity() > 0
                    ? o.getFilledQuantity() + "/" + o.getQuantity()
                    : String.valueOf(o.getQuantity());
            return new SimpleStringProperty(text);
        });
        qtyCol.setPrefWidth(50);
        TableColumn<Order, String> priceCol = new TableColumn<>(COL_PRICE);
        priceCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrice() > 0 ? formatPrice(c.getValue().getPrice()) : DASH));
        priceCol.setPrefWidth(70);
        TableColumn<Order, String> triggerCol = new TableColumn<>(COL_TRIGGER);
        triggerCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTriggerPrice() > 0 ? formatPrice(c.getValue().getTriggerPrice()) : DASH));
        triggerCol.setPrefWidth(70);
        TableColumn<Order, String> avgCol = new TableColumn<>(COL_AVG_PRICE);
        avgCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAveragePrice() > 0 ? formatPrice(c.getValue().getAveragePrice()) : DASH));
        avgCol.setPrefWidth(70);
        TableColumn<Order, OrderStatus> statusCol = new TableColumn<>(COL_STATUS);
        statusCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStatus()));
        statusCol.setCellFactory(col -> createOrderStatusCell());
        statusCol.setPrefWidth(80);
        TableColumn<Order, Order> actionsCol = new TableColumn<>(COL_ACTIONS);
        actionsCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actionsCol.setCellFactory(col -> createOrderActionCell());
        actionsCol.setPrefWidth(ACTION_COLUMN_WIDTH);
        actionsCol.setSortable(false);
        table.getColumns().addAll(scripCol, sideCol, typeCol, qtyCol, priceCol,
                triggerCol, avgCol, statusCol, actionsCol);
        table.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) navigateToScrip(row.getItem().getScripId());
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) navigateToScrip(sel.getScripId());
        });
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<GttOrder> buildGttTable() {
        TableView<GttOrder> table = new TableView<>();
        table.getStyleClass().add(TABLE_STYLE);
        table.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<GttOrder, String> scripCol = new TableColumn<>(COL_SCRIP);
        scripCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getScripId()));
        scripCol.setPrefWidth(100);
        TableColumn<GttOrder, OrderSide> sideCol = new TableColumn<>(COL_SIDE);
        sideCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSide()));
        sideCol.setCellFactory(col -> createGttSideCell());
        sideCol.setPrefWidth(50);
        TableColumn<GttOrder, String> typeCol = new TableColumn<>(COL_TYPE);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLimitType() != null ? c.getValue().getLimitType().name() : DASH));
        typeCol.setPrefWidth(60);
        TableColumn<GttOrder, String> qtyCol = new TableColumn<>(COL_QTY);
        qtyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        qtyCol.setPrefWidth(50);
        TableColumn<GttOrder, String> triggerCol = new TableColumn<>(COL_TRIGGER);
        triggerCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTriggerPrice())));
        triggerCol.setPrefWidth(70);
        TableColumn<GttOrder, String> orderPriceCol = new TableColumn<>(COL_ORDER_PRICE);
        orderPriceCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getOrderPrice())));
        orderPriceCol.setPrefWidth(70);
        TableColumn<GttOrder, String> ltpCol = new TableColumn<>(COL_LTP);
        ltpCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getLastPrice())));
        ltpCol.setPrefWidth(70);
        TableColumn<GttOrder, GttStatus> statusCol = new TableColumn<>(COL_STATUS);
        statusCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStatus()));
        statusCol.setCellFactory(col -> createGttStatusCell());
        statusCol.setPrefWidth(80);
        TableColumn<GttOrder, GttOrder> actionsCol = new TableColumn<>(COL_ACTIONS);
        actionsCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actionsCol.setCellFactory(col -> createGttActionCell());
        actionsCol.setPrefWidth(ACTION_COLUMN_WIDTH);
        actionsCol.setSortable(false);
        table.getColumns().addAll(scripCol, sideCol, typeCol, qtyCol, triggerCol,
                orderPriceCol, ltpCol, statusCol, actionsCol);
        table.setRowFactory(tv -> {
            TableRow<GttOrder> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) navigateToScrip(row.getItem().getScripId());
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) navigateToScrip(sel.getScripId());
        });
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<Position> buildPositionsTable() {
        TableView<Position> table = new TableView<>();
        table.getStyleClass().add(TABLE_STYLE);
        table.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Position, String> scripCol = new TableColumn<>(COL_SCRIP);
        scripCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getScripId()));
        scripCol.setPrefWidth(100);
        TableColumn<Position, String> productCol = new TableColumn<>(COL_PRODUCT);
        productCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduct() != null ? c.getValue().getProduct().name() : DASH));
        productCol.setPrefWidth(60);
        TableColumn<Position, String> qtyCol = new TableColumn<>(COL_QTY);
        qtyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        qtyCol.setPrefWidth(50);
        TableColumn<Position, String> avgCol = new TableColumn<>(COL_AVG_PRICE);
        avgCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getAveragePrice())));
        avgCol.setPrefWidth(70);
        TableColumn<Position, String> ltpCol = new TableColumn<>(COL_LTP);
        ltpCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getLastPrice())));
        ltpCol.setPrefWidth(70);
        TableColumn<Position, Float> pnlCol = new TableColumn<>(COL_PNL);
        pnlCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPnl()));
        pnlCol.setCellFactory(col -> createPnlCell());
        pnlCol.setPrefWidth(80);
        TableColumn<Position, Position> actionsCol = new TableColumn<>(COL_ACTIONS);
        actionsCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actionsCol.setCellFactory(col -> createPositionActionCell());
        actionsCol.setPrefWidth(40);
        actionsCol.setSortable(false);
        table.getColumns().addAll(scripCol, productCol, qtyCol, avgCol, ltpCol, pnlCol, actionsCol);
        table.setRowFactory(tv -> {
            TableRow<Position> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) navigateToScrip(row.getItem().getScripId());
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) navigateToScrip(sel.getScripId());
        });
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<Holding> buildHoldingsTable() {
        TableView<Holding> table = new TableView<>();
        table.getStyleClass().add(TABLE_STYLE);
        table.setPlaceholder(new Label(EMPTY_NO_ACCOUNT));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Holding, String> scripCol = new TableColumn<>(COL_SCRIP);
        scripCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getScripId()));
        scripCol.setPrefWidth(100);
        TableColumn<Holding, String> exchCol = new TableColumn<>(COL_EXCHANGE);
        exchCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExchange()));
        exchCol.setPrefWidth(55);
        TableColumn<Holding, String> qtyCol = new TableColumn<>(COL_QTY);
        qtyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        qtyCol.setPrefWidth(50);
        TableColumn<Holding, String> avgCol = new TableColumn<>(COL_AVG_PRICE);
        avgCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getAveragePrice())));
        avgCol.setPrefWidth(70);
        TableColumn<Holding, String> ltpCol = new TableColumn<>(COL_LTP);
        ltpCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getLastPrice())));
        ltpCol.setPrefWidth(70);
        TableColumn<Holding, String> closeCol = new TableColumn<>(COL_CLOSE);
        closeCol.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getClosePrice())));
        closeCol.setPrefWidth(70);
        TableColumn<Holding, String> valueCol = new TableColumn<>(COL_CURRENT_VALUE);
        valueCol.setCellValueFactory(c -> new SimpleStringProperty(
                formatPrice(c.getValue().getLastPrice() * c.getValue().getQuantity())));
        valueCol.setPrefWidth(80);
        TableColumn<Holding, Float> pnlCol = new TableColumn<>(COL_PNL);
        pnlCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPnl()));
        pnlCol.setCellFactory(col -> createPnlCell());
        pnlCol.setPrefWidth(80);
        TableColumn<Holding, Float> dayChgCol = new TableColumn<>(COL_DAY_CHG);
        dayChgCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDayChangePercentage()));
        dayChgCol.setCellFactory(col -> createPercentChangeCell());
        dayChgCol.setPrefWidth(70);
        TableColumn<Holding, String> t1Col = new TableColumn<>(COL_T1_QTY);
        t1Col.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getT1Quantity())));
        t1Col.setPrefWidth(45);
        TableColumn<Holding, String> productCol = new TableColumn<>(COL_PRODUCT);
        productCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct()));
        productCol.setPrefWidth(55);
        TableColumn<Holding, String> isinCol = new TableColumn<>(COL_ISIN);
        isinCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIsin()));
        isinCol.setPrefWidth(100);
        TableColumn<Holding, Holding> actionsCol = new TableColumn<>(COL_ACTIONS);
        actionsCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        actionsCol.setCellFactory(col -> createHoldingActionCell());
        actionsCol.setPrefWidth(40);
        actionsCol.setSortable(false);
        table.getColumns().addAll(scripCol, exchCol, qtyCol, avgCol, ltpCol, closeCol,
                valueCol, pnlCol, dayChgCol, t1Col, productCol, isinCol, actionsCol);
        table.setRowFactory(tv -> {
            TableRow<Holding> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) navigateToScrip(row.getItem().getScripId());
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) navigateToScrip(sel.getScripId());
        });
        return table;
    }

    private GridPane buildFundsGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add(FUNDS_GRID_STYLE);
        return grid;
    }

    private void populateFundsGrid(Funds funds) {
        fundsGrid.getChildren().clear();
        int row = 0;
        row = addFundsRow(row, FUNDS_EQUITY_CASH, funds.getEquityAvailableCash());
        row = addFundsRow(row, FUNDS_EQUITY_COLLATERAL, funds.getEquityAvailableCollateral());
        row = addFundsRow(row, FUNDS_EQUITY_INTRADAY, funds.getEquityAvailableIntradayPayin());
        row = addFundsRow(row, FUNDS_EQUITY_OPENING, funds.getEquityOpeningBalance());
        row = addFundsRow(row, FUNDS_EQUITY_NET, funds.getEquityNet());
        row = addFundsRow(row, FUNDS_EQUITY_DEBITS, funds.getEquityUtilisedDebits());
        row = addFundsRow(row, FUNDS_EQUITY_EXPOSURE, funds.getEquityUtilisedExposure());
        row = addFundsRow(row, FUNDS_EQUITY_SPAN, funds.getEquityUtilisedSpan());
        row = addFundsRow(row, FUNDS_EQUITY_OPTION_PREMIUM, funds.getEquityUtilisedOptionPremium());
        row = addFundsRow(row, FUNDS_EQUITY_PAYOUT, funds.getEquityUtilisedPayout());
        row = addFundsRow(row, FUNDS_COMMODITY_CASH, funds.getCommodityAvailableCash());
        row = addFundsRow(row, FUNDS_COMMODITY_COLLATERAL, funds.getCommodityAvailableCollateral());
        addFundsRow(row, FUNDS_COMMODITY_NET, funds.getCommodityNet());
    }

    private int addFundsRow(int row, String labelText, double value) {
        Label label = new Label(labelText);
        label.getStyleClass().add(FUNDS_LABEL_STYLE);
        Label valueLabel = new Label(formatFundsValue(value));
        valueLabel.getStyleClass().add(FUNDS_VALUE_STYLE);
        fundsGrid.add(label, 0, row);
        fundsGrid.add(valueLabel, 1, row);
        return row + 1;
    }

    private TableCell<Order, Order> createOrderActionCell() {
        return new TableCell<>() {
            private final Button editBtn = createActionButton(FluentUiRegularAL.EDIT_16, false);
            private final Button deleteBtn = createActionButton(FluentUiRegularAL.DELETE_16, true);
            private final HBox box = new HBox(2, editBtn, deleteBtn);
            {
                editBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    editOrder(order);
                });
                deleteBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    cancelOrder(order);
                });
            }
            @Override
            protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null || order.getStatus() == null || order.getStatus().isTerminal()) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        };
    }

    private TableCell<GttOrder, GttOrder> createGttActionCell() {
        return new TableCell<>() {
            private final Button editBtn = createActionButton(FluentUiRegularAL.EDIT_16, false);
            private final Button deleteBtn = createActionButton(FluentUiRegularAL.DELETE_16, true);
            private final HBox box = new HBox(2, editBtn, deleteBtn);
            {
                editBtn.setOnAction(e -> {
                    GttOrder gtt = getTableView().getItems().get(getIndex());
                    editGtt(gtt);
                });
                deleteBtn.setOnAction(e -> {
                    GttOrder gtt = getTableView().getItems().get(getIndex());
                    cancelGtt(gtt);
                });
            }
            @Override
            protected void updateItem(GttOrder gtt, boolean empty) {
                super.updateItem(gtt, empty);
                if (empty || gtt == null || gtt.getStatus() != GttStatus.ACTIVE) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        };
    }

    private TableCell<Position, Position> createPositionActionCell() {
        return new TableCell<>() {
            private final Button exitBtn = createExitButton();
            {
                exitBtn.setOnAction(e -> {
                    Position pos = getTableView().getItems().get(getIndex());
                    exitPosition(pos);
                });
            }
            @Override
            protected void updateItem(Position pos, boolean empty) {
                super.updateItem(pos, empty);
                if (empty || pos == null || pos.getQuantity() == 0) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(2, exitBtn));
                }
            }
        };
    }

    private TableCell<Holding, Holding> createHoldingActionCell() {
        return new TableCell<>() {
            private final Button exitBtn = createExitButton();
            {
                exitBtn.setOnAction(e -> {
                    Holding holding = getTableView().getItems().get(getIndex());
                    exitHolding(holding);
                });
            }
            @Override
            protected void updateItem(Holding holding, boolean empty) {
                super.updateItem(holding, empty);
                if (empty || holding == null || holding.getQuantity() <= 0) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(2, exitBtn));
                }
            }
        };
    }

    private Button createActionButton(org.kordamp.ikonli.Ikon icon, boolean isDelete) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(ACTION_ICON_SIZE);
        btn.setGraphic(fi);
        btn.getStyleClass().add(ACTION_BUTTON_STYLE);
        if (isDelete) btn.getStyleClass().add(ACTION_DELETE_STYLE);
        btn.setFocusTraversable(false);
        return btn;
    }

    private Button createExitButton() {
        Button btn = new Button();
        FontIcon fi = new FontIcon(FluentUiRegularAL.DELETE_16);
        fi.setIconSize(ACTION_ICON_SIZE);
        btn.setGraphic(fi);
        btn.getStyleClass().addAll(ACTION_BUTTON_STYLE, ACTION_EXIT_STYLE);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void cancelOrder(Order order) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        String message = String.format(ORDER_CANCEL_MESSAGE, order.getSide(), order.getLimitType());
        ConfirmationDialog dialog = new ConfirmationDialog(ORDER_CANCEL_TITLE, message, ORDER_CANCEL_CONFIRM);
        dialog.show(window);
        if (dialog.isConfirmed()) {
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.cancelOrder(order);
                    StatusBar.showSuccess(ORDER_CANCELLED);
                    Platform.runLater(this::refreshOrders);
                } catch (Exception ex) {
                    log.error("Failed to cancel order", ex);
                    StatusBar.showError(String.format(ORDER_CANCEL_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void cancelGtt(GttOrder gtt) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        String message = String.format(GTT_CANCEL_MESSAGE, gtt.getId(), gtt.getTriggerPrice());
        ConfirmationDialog dialog = new ConfirmationDialog(GTT_CANCEL_TITLE, message, GTT_CANCEL_CONFIRM);
        dialog.show(window);
        if (dialog.isConfirmed()) {
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.cancelGtt(gtt.getId());
                    StatusBar.showSuccess(GTT_CANCELLED);
                    Platform.runLater(this::refreshGtts);
                } catch (Exception ex) {
                    log.error("Failed to cancel GTT", ex);
                    StatusBar.showError(String.format(GTT_CANCEL_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void editOrder(Order order) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        RegularOrderDialog dialog = new RegularOrderDialog(order);
        dialog.show(window);
        Order updated = dialog.getResult();
        if (updated != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.updateOrder(updated);
                    StatusBar.showSuccess(ORDER_UPDATED);
                    Platform.runLater(this::refreshOrders);
                } catch (Exception ex) {
                    log.error("Failed to update order", ex);
                    StatusBar.showError(String.format(ORDER_UPDATE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void editGtt(GttOrder gtt) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        GttOrderDialog dialog = new GttOrderDialog(gtt);
        dialog.show(window);
        GttOrder updated = dialog.getResult();
        if (updated != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.updateGtt(updated);
                    StatusBar.showSuccess(GTT_UPDATED);
                    Platform.runLater(this::refreshGtts);
                } catch (Exception ex) {
                    log.error("Failed to update GTT", ex);
                    StatusBar.showError(String.format(GTT_UPDATE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void exitPosition(Position position) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        int qty = Math.abs(position.getQuantity());
        String message = String.format(EXIT_POSITION_MESSAGE, position.getScripId(), qty);
        ConfirmationDialog dialog = new ConfirmationDialog(EXIT_POSITION_TITLE, message, EXIT_POSITION_CONFIRM);
        dialog.show(window);
        if (dialog.isConfirmed()) {
            OrderSide side = position.getQuantity() > 0 ? OrderSide.SELL : OrderSide.BUY;
            Product product = position.getProduct() != null ? position.getProduct() : Product.MIS;
            Order exitOrder = Order.builder()
                    .id(UUID.randomUUID().toString())
                    .scripId(position.getScripId())
                    .side(side)
                    .limitType(LimitType.MARKET)
                    .product(product)
                    .variety(Variety.REGULAR)
                    .validity(Validity.DAY)
                    .quantity(qty)
                    .price(0)
                    .triggerPrice(0)
                    .build();
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.createOrder(exitOrder);
                    StatusBar.showSuccess(EXIT_POSITION_PLACED);
                    Platform.runLater(this::refreshPositions);
                } catch (Exception ex) {
                    log.error("Failed to exit position", ex);
                    StatusBar.showError(String.format(EXIT_POSITION_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void exitHolding(Holding holding) {
        if (chartOrderService == null) return;
        Window window = getScene().getWindow();
        String message = String.format(EXIT_HOLDING_MESSAGE, holding.getQuantity(), holding.getScripId());
        ConfirmationDialog dialog = new ConfirmationDialog(EXIT_HOLDING_TITLE, message, EXIT_HOLDING_CONFIRM);
        dialog.show(window);
        if (dialog.isConfirmed()) {
            Order exitOrder = Order.builder()
                    .id(UUID.randomUUID().toString())
                    .scripId(holding.getScripId())
                    .side(OrderSide.SELL)
                    .limitType(LimitType.MARKET)
                    .product(Product.CNC)
                    .variety(Variety.REGULAR)
                    .validity(Validity.DAY)
                    .quantity(holding.getQuantity())
                    .price(0)
                    .triggerPrice(0)
                    .build();
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.createOrder(exitOrder);
                    StatusBar.showSuccess(EXIT_HOLDING_PLACED);
                    Platform.runLater(this::refreshHoldings);
                } catch (Exception ex) {
                    log.error("Failed to exit holding", ex);
                    StatusBar.showError(String.format(EXIT_HOLDING_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void navigateToScrip(String scripId) {
        if (onNavigateToScrip != null && scripId != null) {
            onNavigateToScrip.accept(scripId);
        }
    }

    private <T> TableCell<T, OrderSide> createSideCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(OrderSide side, boolean empty) {
                super.updateItem(side, empty);
                if (empty || side == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(side.name());
                    String color = side == OrderSide.BUY ? COLOR_BUY : COLOR_SELL;
                    setStyle(String.format(STYLE_TEXT_FILL, color));
                }
            }
        };
    }

    private TableCell<GttOrder, OrderSide> createGttSideCell() {
        return createSideCell();
    }

    private TableCell<Order, OrderStatus> createOrderStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.name());
                    setStyle(String.format(STYLE_TEXT_FILL, resolveOrderStatusColor(status)));
                }
            }
        };
    }

    private TableCell<GttOrder, GttStatus> createGttStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(GttStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.name());
                    setStyle(String.format(STYLE_TEXT_FILL, resolveGttStatusColor(status)));
                }
            }
        };
    }

    private <T> TableCell<T, Float> createPnlCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Float pnl, boolean empty) {
                super.updateItem(pnl, empty);
                if (empty || pnl == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatPrice(pnl));
                    String color = pnl >= 0 ? COLOR_POSITIVE : COLOR_NEGATIVE;
                    setStyle(String.format(STYLE_TEXT_FILL, color));
                }
            }
        };
    }

    private <T> TableCell<T, Float> createPercentChangeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Float pct, boolean empty) {
                super.updateItem(pct, empty);
                if (empty || pct == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%+.2f%%", pct));
                    String color = pct >= 0 ? COLOR_POSITIVE : COLOR_NEGATIVE;
                    setStyle(String.format(STYLE_TEXT_FILL, color));
                }
            }
        };
    }

    private String resolveOrderStatusColor(OrderStatus status) {
        return switch (status) {
            case OPEN, PENDING, DISPATCHED -> COLOR_OPEN;
            case UPDATE_PENDING, CANCEL_PENDING -> COLOR_WARNING;
            case COMPLETE -> COLOR_SUCCESS;
            case CANCELLED -> COLOR_MUTED;
            case REJECTED -> COLOR_ERROR;
        };
    }

    private String resolveGttStatusColor(GttStatus status) {
        return switch (status) {
            case ACTIVE -> COLOR_OPEN;
            case TRIGGERED -> COLOR_SUCCESS;
            case CANCELLED, DISABLED -> COLOR_MUTED;
            case EXPIRED, REJECTED -> COLOR_ERROR;
        };
    }

    private String formatPrice(float price) {
        if (price == 0) return DASH;
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

    private String formatFundsValue(double value) {
        if (value == 0) return DASH;
        return String.format("%,.2f", value);
    }

    private String extractMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) return ex.getClass().getSimpleName();
        int jsonIdx = msg.indexOf("\"message\":\"");
        if (jsonIdx >= 0) {
            int start = jsonIdx + 12;
            int end = msg.indexOf('"', start);
            if (end > start) return msg.substring(start, end);
        }
        return msg.length() > 80 ? msg.substring(0, 80) : msg;
    }

}
