package com.whiteowl.workbench.charting;

import static com.whiteowl.workbench.charting.ChartTheme.INDICATOR_PALETTE;
import static com.whiteowl.workbench.charting.ChartTheme.INFO_VALUE;

import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsView;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.portfolio.service.PortfolioQuantityProvider;
import com.whiteowl.core.order.model.OrderStatus;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenRegistry;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.core.trendline.Trendline;
import com.whiteowl.core.trendline.TrendlineDirection;
import com.whiteowl.core.trendline.TrendlineDetector;
import com.whiteowl.core.trendline.TrendlineSettings;
import com.whiteowl.workbench.StatusBar;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.screener.ScreenConfigDialog;
import com.whiteowl.workbench.charting.drawing.DrawingManager;
import com.whiteowl.workbench.charting.drawing.DrawingTool;
import com.whiteowl.workbench.charting.drawing.DrawingToolIcons;
import com.whiteowl.workbench.charting.drawing.repository.DrawingRepository;
import com.whiteowl.workbench.charting.drawing.repository.FileDrawingRepository;
import com.whiteowl.core.examplegroup.model.Example;
import com.whiteowl.core.examplegroup.model.ExampleGroup;
import com.whiteowl.workbench.common.AddToCollectionMenuBuilder;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.charting.drawing.Drawing;
import com.whiteowl.workbench.examplegroup.ExampleGroupPane;
import com.whiteowl.workbench.group.GroupPane;
import com.whiteowl.core.watchlist.model.Watchlist;
import com.whiteowl.workbench.watchlist.WatchlistPane;
import com.whiteowl.workbench.charting.indicator.ActiveIndicator;
import com.whiteowl.workbench.charting.indicator.ActiveSubChartIndicator;
import com.whiteowl.workbench.charting.indicator.IndicatorRegistry;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import com.whiteowl.workbench.charting.indicator.IndicatorSettingsDialog;
import com.whiteowl.workbench.charting.indicator.OverlayIndicator;
import com.whiteowl.workbench.charting.indicator.SubChartIndicator;
import com.whiteowl.workbench.charting.indicator.SubChartResult;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileData;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileIndicator;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class ChartPane extends BorderPane {

    private static final String CHART_PANE_STYLE = "chart-pane";
    private static final String CHART_TOOLBAR_STYLE = "chart-toolbar";
    private static final String BUTTON_GROUP_STYLE = "chart-button-group";
    private static final String TIMEFRAME_BUTTON_STYLE = "timeframe-button";
    private static final String TIMEFRAME_BUTTON_ACTIVE_STYLE = "timeframe-button-active";
    private static final String INDICATOR_POPUP_STYLE = "indicator-popup";
    private static final String INDICATOR_ADD_ITEM_STYLE = "indicator-add-item";
    private static final String INDICATOR_LIST_STYLE = "indicator-list";
    private static final String INDICATOR_TAG_STYLE = "indicator-tag";
    private static final String INDICATOR_TAG_DELETE_STYLE = "indicator-tag-delete";
    private static final String INDICATOR_TAG_EDIT_STYLE = "indicator-tag-edit";
    private static final String INDICATOR_TAG_NAME_STYLE = "indicator-tag-name";
    private static final String INDICATOR_TAG_VALUE_STYLE = "indicator-tag-value";
    private static final String NO_DATA_LABEL_STYLE = "chart-no-data";
    private static final String TEXT_FILL_PREFIX = "-fx-text-fill: ";
    private static final String STYLE_SUFFIX = ";";
    private static final String NO_DATA_TEXT = "Select a scrip to view chart";
    private static final String INDICATORS_LABEL = "Indicators";
    private static final String ADD_LABEL = "+ ";
    private static final String SUB_CHART_WRAPPER_STYLE = "sub-chart-wrapper";
    private static final String POPUP_SEARCH_STYLE = "popup-search-field";
    private static final String SEARCH_INDICATOR_PROMPT = "Search indicators...";
    private static final String SEARCH_SCREEN_PROMPT = "Search screens...";
    private static final int MAX_VISIBLE_POPUP_ITEMS = 10;
    private static final double POPUP_ITEM_HEIGHT = 27;
    private static final String LOADING_LABEL = "Loading...";
    private static final int MIN_SEARCH_LENGTH = 1;
    private static final String DRAWING_BUTTON_STYLE = "drawing-button";
    private static final String DRAWING_BUTTON_ACTIVE_STYLE = "drawing-button-active";
    private static final String CLEAR_DRAWINGS_LABEL = "Clear";
    private static final String CLEAR_DRAWINGS_ICON_STYLE = "drawing-clear-icon";
    private static final int CLEAR_ICON_SIZE = 14;
    private static final String ADD_TO_WATCHLIST_LABEL = "Add to Watchlist";
    private static final String ADD_TO_GROUP_LABEL = "Add to Group";
    private static final String NO_EXAMPLE_GROUPS_LABEL = "None available";
    private static final String SCREEN_TAG_STYLE = "screen-tag";
    private static final String SCREEN_TAG_NAME_STYLE = "screen-tag-name";
    private static final String SCREEN_TAG_DELETE_STYLE = "screen-tag-delete";
    private static final String SCREEN_TAG_EDIT_STYLE = "screen-tag-edit";
    private static final String SCREENS_LABEL = "Screens";
    private static final String TRENDLINES_LABEL = "Trendlines";
    private static final String TRENDLINE_TAG_STYLE = "screen-tag";
    private static final String TRENDLINE_TAG_NAME_STYLE = "screen-tag-name";
    private static final String TRENDLINE_TAG_DELETE_STYLE = "screen-tag-delete";
    private static final String TRENDLINE_TAG_EDIT_STYLE = "screen-tag-edit";
    private static final String ZOOM_BUTTON_STYLE = "zoom-button";
    private static final String ZOOM_IN_TEXT = "+";
    private static final String ZOOM_OUT_TEXT = "\u2212";
    private static final int ZOOM_DELTA = 20;
    private static final int ZOOM_BUTTON_SPACING = 4;
    private static final int ZOOM_MARGIN_BOTTOM = 24;
    private static final int TAG_ICON_SIZE = 10;
    private static final double INDICATOR_LIST_TOP_OFFSET = 18;
    private static final double SUB_CHART_HEIGHT = 100;
    private static final double SEPARATOR_TAG_TOP_OFFSET = 2;
    private static final String LOG_SCALE_LABEL = "Log";
    private static final String GTT_BUY_LABEL = "GTT Buy";
    private static final String GTT_SELL_LABEL = "GTT Sell";
    private static final String GTT_CANCEL_TITLE = "Cancel GTT";
    private static final String GTT_CANCEL_MESSAGE = "Cancel this GTT order at %.2f?";
    private static final String GTT_CANCEL_CONFIRM = "Cancel GTT";
    private static final String GTT_NO_ACCOUNT = "Connect an account to use GTT orders";
    private static final String GTT_CREATING = "Creating GTT order...";
    private static final String GTT_CREATED = "GTT order created successfully";
    private static final String GTT_CREATE_FAILED = "Failed to create GTT: %s";
    private static final String GTT_UPDATING = "Updating GTT order...";
    private static final String GTT_UPDATED = "GTT order updated successfully";
    private static final String GTT_UPDATE_FAILED = "Failed to update GTT: %s";
    private static final String GTT_CANCELLING = "Cancelling GTT order...";
    private static final String GTT_CANCELLED = "GTT order cancelled";
    private static final String GTT_CANCEL_FAILED = "Failed to cancel GTT: %s";
    private static final String GTT_REFRESHING = "Refreshing GTT orders...";
    private static final String GTT_REFRESHED = "Loaded %d active GTT orders";
    private static final String GTT_REFRESH_FAILED = "Failed to refresh GTTs: %s";
    private static final String OCO_LABEL = "OCO";
    private static final String OCO_NO_POSITION = "No open position for OCO";
    private static final String OCO_CREATING = "Creating OCO GTT...";
    private static final String OCO_CREATED = "OCO GTT created successfully";
    private static final String OCO_CREATE_FAILED = "Failed to create OCO GTT: %s";
    private static final String OCO_UPDATING = "Updating OCO GTT...";
    private static final String OCO_UPDATED = "OCO GTT updated successfully";
    private static final String OCO_UPDATE_FAILED = "Failed to update OCO GTT: %s";
    private static final String OCO_CANCEL_TITLE = "Cancel OCO GTT";
    private static final String OCO_CANCEL_MESSAGE = "Cancel this OCO GTT (SL %.2f / Target %.2f)?";
    private static final String OCO_CANCEL_CONFIRM = "Cancel OCO";
    private static final String ORDER_BUY_LABEL = "Buy";
    private static final String ORDER_SELL_LABEL = "Sell";
    private static final String ORDER_NO_ACCOUNT = "Connect an account to place orders";
    private static final String ORDER_PLACING = "Placing order...";
    private static final String ORDER_PLACED = "Order placed successfully";
    private static final String ORDER_PLACE_FAILED = "Failed to place order: %s";
    private static final String ORDER_UPDATING = "Updating order...";
    private static final String ORDER_UPDATED = "Order updated successfully";
    private static final String ORDER_UPDATE_FAILED = "Failed to update order: %s";
    private static final String ORDER_CANCEL_TITLE = "Cancel Order";
    private static final String ORDER_CANCEL_MESSAGE = "Cancel this order at %.2f?";
    private static final String ORDER_CANCEL_CONFIRM = "Cancel";
    private static final String ORDER_CANCELLING = "Cancelling order...";
    private static final String ORDER_CANCELLED = "Order cancelled";
    private static final String ORDER_CANCEL_FAILED = "Failed to cancel order: %s";
    private static final String ORDER_REFRESHING = "Refreshing orders...";
    private static final String ORDER_REFRESHED = "Loaded %d active orders";
    private static final String ORDER_REFRESH_FAILED = "Failed to refresh orders: %s";
    private static final String ORDER_BUTTON_STYLE = "order-action-button";
    private static final int ORDER_BUTTON_ICON_SIZE = 14;
    private static final int TOOLBAR_ICON_SIZE = 12;
    private static final String WATCHLIST_PILLS_STYLE = "watchlist-pills-bar";
    private static final String WATCHLIST_PILL_STYLE = "watchlist-pill";
    private static final double WATCHLIST_PILLS_TOP_OFFSET = 6;
    private static final double WATCHLIST_PILLS_RIGHT_OFFSET = 8;
    private static final int WATCHLIST_PILLS_GAP = 4;

    private final ChartViewport viewport;
    private final BarDataProvider dataProvider;
    private final CandlestickCanvas canvas;
    private final StackPane canvasContainer;
    private final VBox chartStack;
    private final IndicatorRegistry indicatorRegistry;
    private final ScreenRegistry screenRegistry;
    private final List<ActiveIndicator> activeIndicators;
    private final List<ActiveSubChartIndicator> activeSubChartIndicators;
    private final List<SubChartCanvas> subChartCanvases;
    private final List<Label> indicatorValueLabels;
    private final List<Label> subChartValueLabels;
    private final VBox indicatorListBar;
    private final FlowPane watchlistPillsBar;
    private final DrawingManager drawingManager;
    private final DrawingRepository drawingRepository;
    private final ContextMenu chartContextMenu;
    private final ContextMenu rulerExampleMenu;
    private final BarsRepository barsRepository;
    private final ScripRepository scripRepository;
    private Screen activeScreen;
    private final TrendlineSettings trendlineSettings = new TrendlineSettings();
    private boolean trendlinesActive;
    private ToggleGroup timeframeToggleGroup;
    private ToggleButton activeDrawingButton;
    private WatchlistPane watchlistPane;
    private GroupPane groupPane;
    private ExampleGroupPane exampleGroupPane;
    private Timeframe activeTimeframe;
    private Scrip activeScrip;
    private Bars lastBars;
    private List<IndicatorResult> lastOverlayResults = List.of();
    private ScripNavigable scripNavigable;
    private final EventHandler<KeyEvent> keyNavigationHandler = this::handleKeyNavigation;
    private long contextMenuTimestamp = -1;
    private long exampleCutoffTimestamp = -1;
    private ChartOrderService chartOrderService;
    private double positionSize;
    private List<GttOrder> allGttOrders = List.of();
    private List<GttOrder> activeGttOrders = List.of();
    private List<Order> allRegularOrders = List.of();
    private List<Order> activeRegularOrders = List.of();
    private List<OcoGttOrder> allOcoOrders = List.of();
    private List<OcoGttOrder> activeOcoOrders = List.of();
    private float ocoFirstPrice;
    private Button ocoButton;
    private Runnable onOrderBookRefresh;

    public ChartPane(BarsRepository barsRepository, ScripRepository scripRepository,
                     ScriptRepository indicatorScriptRepo, ScriptRepository screenerScriptRepo) {
        getStyleClass().add(CHART_PANE_STYLE);
        this.barsRepository = barsRepository;
        this.scripRepository = scripRepository;
        this.viewport = new ChartViewport();
        this.dataProvider = new BarDataProvider(barsRepository);
        this.canvas = new CandlestickCanvas(viewport, dataProvider);
        this.canvasContainer = new StackPane();
        this.chartStack = new VBox();
        this.indicatorRegistry = new IndicatorRegistry(barsRepository, indicatorScriptRepo);
        this.screenRegistry = new ScreenRegistry(screenerScriptRepo);
        this.activeIndicators = new ArrayList<>();
        this.activeSubChartIndicators = new ArrayList<>();
        this.subChartCanvases = new ArrayList<>();
        this.indicatorValueLabels = new ArrayList<>();
        this.subChartValueLabels = new ArrayList<>();
        this.indicatorListBar = new VBox(0);
        this.watchlistPillsBar = new FlowPane(WATCHLIST_PILLS_GAP, WATCHLIST_PILLS_GAP);
        this.watchlistPillsBar.getStyleClass().add(WATCHLIST_PILLS_STYLE);
        this.watchlistPillsBar.setPickOnBounds(false);
        this.watchlistPillsBar.setMaxHeight(Region.USE_PREF_SIZE);
        this.watchlistPillsBar.setMaxWidth(Region.USE_PREF_SIZE);
        this.drawingManager = new DrawingManager();
        this.drawingRepository = new FileDrawingRepository();
        this.chartContextMenu = new ContextMenu();
        this.chartContextMenu.setAutoHide(true);
        this.rulerExampleMenu = new ContextMenu();
        this.rulerExampleMenu.setAutoHide(true);
        this.activeTimeframe = Timeframe.DAILY;
        drawingManager.setOnChanged(this::saveDrawings);
        canvas.setDrawingManager(drawingManager);
        canvas.setOnDrawingComplete(this::onDrawingComplete);
        canvas.setOnHoverChanged(this::onMainChartHoverChanged);
        canvas.setOnViewportChanged(this::onViewportChanged);
        canvas.setOnContextMenuRequested(this::showChartContextMenu);
        canvas.setOnContextMenuHide(this::hideContextMenu);
        canvas.setOnProfileRangeChanged(this::onProfileRangeChanged);
        canvas.setOnGttModified(this::handleGttModified);
        canvas.setOnGttCancelled(this::handleGttCancelRequest);
        canvas.setOnOcoModified(this::handleOcoModified);
        canvas.setOnOcoCancelled(this::handleOcoCancelRequest);
        canvas.setOnOrderClicked(this::handleOrderClicked);
        canvas.setOnOrderCancelled(this::handleOrderCancelRequest);
        canvas.setOnOrderModified(this::handleOrderModified);
        canvas.setOnPricePicked(this::handlePricePicked);
        canvas.setOnRulerAddButtonClicked(this::handleRulerAddButton);
        buildLayout();
        showEmptyState();
    }

    public void showChart(Scrip scrip) {
        exampleCutoffTimestamp = -1;
        this.activeScrip = scrip;
        canvas.setScrip(scrip);
        drawingManager.loadDrawings(drawingRepository.loadDrawings(scrip.getId()));
        resetVolumeProfileIndicators();
        loadData(scrip.getId(), activeTimeframe);
        filterGttOrdersForActiveScrip();
        filterOcoOrdersForActiveScrip();
        filterRegularOrdersForActiveScrip();
        updateOcoButtonState();
        refreshWatchlistPills();
    }

    public void showTradeOverlay(Scrip scrip, Timeframe timeframe,
                                List<TradeRecord> trades,
                                TradeRecord selected) {
        exampleCutoffTimestamp = -1;
        this.activeScrip = scrip;
        canvas.setScrip(scrip);
        drawingManager.loadDrawings(drawingRepository.loadDrawings(scrip.getId()));
        resetVolumeProfileIndicators();
        selectTimeframeButton(timeframe);
        loadData(scrip.getId(), timeframe);
        canvas.setTradeOverlay(trades);
        canvas.render();
        scrollToTrade(selected);
    }

    public void clearTradeOverlay() {
        canvas.setTradeOverlay(null);
        canvas.render();
    }

    private void scrollToTrade(TradeRecord trade) {
        if (trade == null || lastBars == null || lastBars.size() == 0) return;
        int entryIdx = findBarByTimestamp(trade.getEntryTimestamp());
        int exitIdx = findBarByTimestamp(trade.getExitTimestamp());
        int padding = viewport.getVisibleBars() / 4;
        int targetStart = Math.max(0, entryIdx - padding);
        int targetEnd = exitIdx + padding;
        int needed = targetEnd - targetStart;
        if (needed > viewport.getVisibleBars()) {
            viewport.zoomBy(needed - viewport.getVisibleBars());
        }
        viewport.panBy(targetStart - viewport.getStartIndex());
        canvas.render();
        renderSubCharts();
    }

    private int findBarByTimestamp(long timestamp) {
        if (lastBars == null) return 0;
        for (int i = 0; i < lastBars.size(); i++) {
            if (lastBars.getTimestamp(i) >= timestamp) return i;
        }
        return lastBars.size() - 1;
    }

    public void showExample(Scrip scrip, Timeframe timeframe, long startTimestamp, long endTimestamp) {
        exampleCutoffTimestamp = endTimestamp;
        this.activeScrip = scrip;
        canvas.setScrip(scrip);
        drawingManager.loadDrawings(drawingRepository.loadDrawings(scrip.getId()));
        selectTimeframeButton(timeframe);
        loadData(scrip.getId(), timeframe);
        refreshWatchlistPills();
    }

    public void setWatchlistPane(WatchlistPane pane) {
        this.watchlistPane = pane;
        pane.setOnWatchlistsChanged(() -> {
            rebuildContextMenu();
            refreshWatchlistPills();
        });
        rebuildContextMenu();
    }

    public void setGroupPane(GroupPane pane) {
        this.groupPane = pane;
        rebuildContextMenu();
    }

    public void setExampleGroupPane(ExampleGroupPane pane) {
        this.exampleGroupPane = pane;
    }

    public void setScripNavigable(ScripNavigable navigable) {
        this.scripNavigable = navigable;
    }

    public void setChartOrderService(ChartOrderService service, double positionSize) {
        this.chartOrderService = service;
        this.positionSize = positionSize;
        rebuildContextMenu();
        if (service == null) {
            allGttOrders = List.of();
            activeGttOrders = List.of();
            allRegularOrders = List.of();
            activeRegularOrders = List.of();
            canvas.setGttOrders(activeGttOrders);
            canvas.setRegularOrders(activeRegularOrders);
            canvas.render();
        }
    }

    public void setOnOrderBookRefresh(Runnable handler) {
        this.onOrderBookRefresh = handler;
    }

    public EventHandler<KeyEvent> getKeyNavigationHandler() {
        return keyNavigationHandler;
    }

    private void handleKeyNavigation(KeyEvent event) {
        if (scripNavigable == null) return;
        if (!isVisible() || getScene() == null) return;
        Node focusOwner = getScene().getFocusOwner();
        if (focusOwner instanceof javafx.scene.control.TextInputControl) return;
        if (focusOwner instanceof javafx.scene.control.ListView) return;
        if (focusOwner instanceof javafx.scene.control.TableView) return;
        if (event.getCode() == KeyCode.DOWN) {
            scripNavigable.selectNext();
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            scripNavigable.selectPrevious();
            event.consume();
        }
    }

    private void toggleLogScale(boolean enabled) {
        canvas.setLogScale(enabled);
        canvas.render();
        renderSubCharts();
    }

    private void saveDrawings() {
        if (activeScrip == null) return;
        drawingRepository.saveDrawings(activeScrip.getId(), drawingManager.getDrawings());
    }

    public void hideContextMenu() {
        chartContextMenu.hide();
        rulerExampleMenu.hide();
    }

    private void handleRulerAddButton(Drawing rulerDrawing, double px, double py) {
        if (activeScrip == null || exampleGroupPane == null) return;
        if (rulerDrawing.getAnchor1() == null || rulerDrawing.getAnchor2() == null) return;
        long ts1 = rulerDrawing.getAnchor1().getTimestamp();
        long ts2 = rulerDrawing.getAnchor2().getTimestamp();
        long startTs = Math.min(ts1, ts2);
        long endTs = Math.max(ts1, ts2);
        rulerExampleMenu.getItems().clear();
        List<ExampleGroup> groups = exampleGroupPane.getGroups();
        if (groups == null || groups.isEmpty()) {
            MenuItem empty = new MenuItem(NO_EXAMPLE_GROUPS_LABEL);
            empty.setDisable(true);
            rulerExampleMenu.getItems().add(empty);
        } else {
            for (ExampleGroup group : groups) {
                MenuItem item = new MenuItem(group.getName());
                item.setOnAction(ev -> {
                    Example example = new Example(activeScrip.getId(), activeTimeframe, startTs, endTs);
                    exampleGroupPane.addExampleToGroup(group, example);
                    rulerExampleMenu.hide();
                });
                rulerExampleMenu.getItems().add(item);
            }
        }
        Point2D screen = canvas.localToScreen(px, py);
        rulerExampleMenu.show(canvas, screen.getX(), screen.getY());
    }

    private void showChartContextMenu(double screenX, double screenY) {
        if (activeScrip == null) return;
        contextMenuTimestamp = canvas.getHoveredTimestamp();
        chartContextMenu.hide();
        rebuildContextMenu();
        chartContextMenu.show(canvas, screenX, screenY);
    }

    private void rebuildContextMenu() {
        chartContextMenu.getItems().clear();
        if (watchlistPane != null) {
            chartContextMenu.getItems().add(AddToCollectionMenuBuilder.build(
                    ADD_TO_WATCHLIST_LABEL,
                    FluentUiRegularMZ.STAR_16,
                    () -> watchlistPane.getWatchlists(),
                    () -> activeScrip != null ? List.of(activeScrip.getId()) : List.of(),
                    () -> { watchlistPane.persist(); watchlistPane.syncAllItems(); hideContextMenu(); }
            ));
        }
        if (groupPane != null) {
            chartContextMenu.getItems().add(AddToCollectionMenuBuilder.build(
                    ADD_TO_GROUP_LABEL,
                    FluentUiRegularMZ.TAG_16,
                    () -> groupPane.getGroups(),
                    () -> activeScrip != null ? List.of(activeScrip.getId()) : List.of(),
                    () -> { groupPane.persist(); groupPane.refresh(); hideContextMenu(); }
            ));
        }
    
    }

    private List<MenuItem> buildOrderMenuItems() {
        MenuItem buyItem = new MenuItem(ORDER_BUY_LABEL);
        FontIcon buyIcon = new FontIcon(FluentUiRegularAL.CART_16);
        buyIcon.setIconSize(16);
        buyItem.setGraphic(buyIcon);
        buyItem.setOnAction(e -> startPricePicker(PricePickerMode.BUY));
        MenuItem sellItem = new MenuItem(ORDER_SELL_LABEL);
        FontIcon sellIcon = new FontIcon(FluentUiRegularAL.CART_16);
        sellIcon.setIconSize(16);
        sellItem.setGraphic(sellIcon);
        sellItem.setOnAction(e -> startPricePicker(PricePickerMode.SELL));
        return List.of(buyItem, sellItem);
    }

    private List<MenuItem> buildGttMenuItems() {
        MenuItem buyItem = new MenuItem(GTT_BUY_LABEL);
        FontIcon buyIcon = new FontIcon(FluentUiRegularAL.ARROW_UP_20);
        buyIcon.setIconSize(16);
        buyItem.setGraphic(buyIcon);
        buyItem.setOnAction(e -> startPricePicker(PricePickerMode.GTT_BUY));
        MenuItem sellItem = new MenuItem(GTT_SELL_LABEL);
        FontIcon sellIcon = new FontIcon(FluentUiRegularAL.ARROW_DOWN_20);
        sellIcon.setIconSize(16);
        sellItem.setGraphic(sellIcon);
        sellItem.setOnAction(e -> startPricePicker(PricePickerMode.GTT_SELL));
        MenuItem ocoItem = new MenuItem(OCO_LABEL);
        FontIcon ocoIcon = new FontIcon(FluentUiRegularAL.ARROW_SORT_20);
        ocoIcon.setIconSize(16);
        ocoItem.setGraphic(ocoIcon);
        ocoItem.setOnAction(e -> startOcoPricePicker());
        boolean hasPosition = activeScrip != null
                && PortfolioQuantityProvider.getInstance().getQuantity(activeScrip.getId()) != 0;
        ocoItem.setDisable(!hasPosition || chartOrderService == null);
        return List.of(buyItem, sellItem, ocoItem);
    }

    private void startPricePicker(PricePickerMode mode) {
        if (activeScrip == null || chartOrderService == null) {
            StatusBar.showError(mode.isGtt() ? GTT_NO_ACCOUNT : ORDER_NO_ACCOUNT);
            return;
        }
        canvas.startPricePicker(mode);
    }

    private void handlePricePicked(PricePickerMode mode, float price) {
        float roundedPrice = roundToTickSize(price);
        switch (mode) {
            case BUY -> showRegularOrderDialog(OrderSide.BUY, roundedPrice);
            case SELL -> showRegularOrderDialog(OrderSide.SELL, roundedPrice);
            case GTT_BUY -> showGttOrderDialog(OrderSide.BUY, roundedPrice);
            case GTT_SELL -> showGttOrderDialog(OrderSide.SELL, roundedPrice);
            case OCO_FIRST -> {
                ocoFirstPrice = roundedPrice;
                canvas.startPricePicker(PricePickerMode.OCO_SECOND);
            }
            case OCO_SECOND -> showOcoGttOrderDialog(ocoFirstPrice, roundedPrice);
        }
    }

    private float roundToTickSize(float price) {
        if (activeScrip == null) return price;
        float tickSize = activeScrip.getTickSize();
        if (tickSize <= 0) return price;
        return Math.round(price / tickSize) * tickSize;
    }

    private void showRegularOrderDialog(OrderSide side, float price) {
        if (activeScrip == null || chartOrderService == null) {
            StatusBar.showError(ORDER_NO_ACCOUNT);
            return;
        }
        if (price <= 0) return;
        int suggestedQty = resolveQuantity(side, price);
        RegularOrderDialog dialog = new RegularOrderDialog(side, price, activeScrip.getId(), suggestedQty);
        dialog.show(getScene().getWindow());
        Order orderResult = dialog.getResult();
        if (orderResult != null) {
            StatusBar.showInfo(ORDER_PLACING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.createOrder(orderResult);
                    StatusBar.showSuccess(ORDER_PLACED);
                    Platform.runLater(this::fireOrderBookRefresh);
                } catch (Exception ex) {
                    log.error("Failed to place order", ex);
                    StatusBar.showError(String.format(ORDER_PLACE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void showGttOrderDialog(OrderSide side, float price) {
        if (activeScrip == null || chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        if (price <= 0) return;
        float lastPrice = getLastClosePrice();
        int suggestedQty = resolveQuantity(side, price);
        GttOrderDialog dialog = new GttOrderDialog(side, price, lastPrice, activeScrip.getId(), suggestedQty);
        dialog.show(getScene().getWindow());
        GttOrder result = dialog.getResult();
        if (result != null) {
            StatusBar.showInfo(GTT_CREATING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.createGtt(result);
                    StatusBar.showSuccess(GTT_CREATED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to create GTT", ex);
                    StatusBar.showError(String.format(GTT_CREATE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private int resolveQuantity(OrderSide side, float price) {
        return chartOrderService.findPortfolioQuantity(activeScrip.getId(), side)
                .orElseGet(() -> roundToLotSize(chartOrderService.suggestQuantity(price, positionSize)));
    }

    private int roundToLotSize(int quantity) {
        if (activeScrip == null) return quantity;
        int lotSize = (int) activeScrip.getLotSize();
        if (lotSize <= 1) return quantity;
        return Math.max(lotSize, (quantity / lotSize) * lotSize);
    }

    private void handleGttModified(GttOrder order) {
        if (chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        GttOrderDialog dialog = new GttOrderDialog(order);
        dialog.show(getScene().getWindow());
        GttOrder result = dialog.getResult();
        if (result != null) {
            StatusBar.showInfo(GTT_UPDATING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.updateGtt(result);
                    StatusBar.showSuccess(GTT_UPDATED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to update GTT", ex);
                    StatusBar.showError(String.format(GTT_UPDATE_FAILED, extractMessage(ex)));
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                }
            });
        }
    }

    private void handleGttCancelRequest(GttOrder order) {
        if (chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        String message = String.format(GTT_CANCEL_MESSAGE, order.getTriggerPrice());
        ConfirmationDialog dialog = new ConfirmationDialog(GTT_CANCEL_TITLE, message, GTT_CANCEL_CONFIRM);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            StatusBar.showInfo(GTT_CANCELLING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.cancelGtt(order.getId());
                    StatusBar.showSuccess(GTT_CANCELLED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to cancel GTT", ex);
                    StatusBar.showError(String.format(GTT_CANCEL_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void showOcoGttOrderDialog(float price1, float price2) {
        if (activeScrip == null || chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        int qty = PortfolioQuantityProvider.getInstance().getQuantity(activeScrip.getId());
        if (qty == 0) {
            StatusBar.showError(OCO_NO_POSITION);
            return;
        }
        OrderSide side = qty > 0 ? OrderSide.SELL : OrderSide.BUY;
        int absQty = Math.abs(qty);
        float slPrice = Math.min(price1, price2);
        float tgtPrice = Math.max(price1, price2);
        if (side == OrderSide.BUY) {
            slPrice = Math.max(price1, price2);
            tgtPrice = Math.min(price1, price2);
        }
        float lastPrice = getLastClosePrice();
        OcoGttOrderDialog dialog = new OcoGttOrderDialog(
                side, slPrice, tgtPrice, lastPrice, activeScrip.getId(), absQty);
        dialog.show(getScene().getWindow());
        OcoGttOrder result = dialog.getResult();
        if (result != null) {
            StatusBar.showInfo(OCO_CREATING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.createOcoGtt(result);
                    StatusBar.showSuccess(OCO_CREATED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to create OCO GTT", ex);
                    StatusBar.showError(String.format(OCO_CREATE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void handleOcoModified(OcoGttOrder order) {
        if (chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        OcoGttOrderDialog dialog = new OcoGttOrderDialog(order);
        dialog.show(getScene().getWindow());
        OcoGttOrder result = dialog.getResult();
        if (result != null) {
            StatusBar.showInfo(OCO_UPDATING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.updateOcoGtt(result);
                    StatusBar.showSuccess(OCO_UPDATED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to update OCO GTT", ex);
                    StatusBar.showError(String.format(OCO_UPDATE_FAILED, extractMessage(ex)));
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                }
            });
        }
    }

    private void handleOcoCancelRequest(OcoGttOrder order) {
        if (chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        String message = String.format(OCO_CANCEL_MESSAGE,
                order.getStoplossTriggerPrice(), order.getTargetTriggerPrice());
        ConfirmationDialog dialog = new ConfirmationDialog(
                OCO_CANCEL_TITLE, message, OCO_CANCEL_CONFIRM);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            StatusBar.showInfo(GTT_CANCELLING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.cancelGtt(order.getId());
                    StatusBar.showSuccess(GTT_CANCELLED);
                    Platform.runLater(() -> { refreshGttOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to cancel OCO GTT", ex);
                    StatusBar.showError(String.format(GTT_CANCEL_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void startOcoPricePicker() {
        if (activeScrip == null || chartOrderService == null) {
            StatusBar.showError(GTT_NO_ACCOUNT);
            return;
        }
        int qty = PortfolioQuantityProvider.getInstance().getQuantity(activeScrip.getId());
        if (qty == 0) {
            StatusBar.showError(OCO_NO_POSITION);
            return;
        }
        ocoFirstPrice = 0;
        canvas.startPricePicker(PricePickerMode.OCO_FIRST);
    }

    private void updateOcoButtonState() {
        if (ocoButton == null) return;
        boolean hasPosition = activeScrip != null
                && PortfolioQuantityProvider.getInstance().getQuantity(activeScrip.getId()) != 0;
        ocoButton.setDisable(!hasPosition || chartOrderService == null);
    }

    private void filterOcoOrdersForActiveScrip() {
        String scripId = activeScrip != null ? activeScrip.getId() : null;
        activeOcoOrders = allOcoOrders.stream()
                .filter(o -> o.getStatus() == GttStatus.ACTIVE)
                .filter(o -> scripId == null || scripId.equals(o.getScripId()))
                .toList();
        canvas.setOcoOrders(activeOcoOrders);
        canvas.render();
    }

    public void refreshGttOrders() {
        if (chartOrderService == null) return;
        StatusBar.showInfo(GTT_REFRESHING);
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchGtts();
            } catch (Exception ex) {
                log.error("Failed to fetch GTTs", ex);
                StatusBar.showError(String.format(GTT_REFRESH_FAILED, extractMessage(ex)));
                return List.<GttOrder>of();
            }
        }).thenAccept(orders -> Platform.runLater(() -> {
            allGttOrders = orders;
            filterGttOrdersForActiveScrip();
            StatusBar.showSuccess(String.format(GTT_REFRESHED, activeGttOrders.size()));
        }));
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchOcoGtts();
            } catch (Exception ex) {
                log.error("Failed to fetch OCO GTTs", ex);
                return List.<OcoGttOrder>of();
            }
        }).thenAccept(orders -> Platform.runLater(() -> {
            allOcoOrders = orders;
            filterOcoOrdersForActiveScrip();
        }));
    }

    private void filterGttOrdersForActiveScrip() {
        String scripId = activeScrip != null ? activeScrip.getId() : null;
        activeGttOrders = allGttOrders.stream()
                .filter(o -> o.getStatus() == GttStatus.ACTIVE)
                .filter(o -> scripId == null || scripId.equals(o.getScripId()))
                .toList();
        canvas.setGttOrders(activeGttOrders);
        canvas.render();
    }

    private void handleOrderClicked(Order order) {
        if (activeScrip == null || chartOrderService == null) return;
        RegularOrderDialog dialog = new RegularOrderDialog(order);
        dialog.show(getScene().getWindow());
        Order updated = dialog.getResult();
        if (updated != null) {
            StatusBar.showInfo(ORDER_UPDATING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.updateOrder(updated);
                    StatusBar.showSuccess(ORDER_UPDATED);
                    Platform.runLater(() -> { refreshRegularOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to update order", ex);
                    StatusBar.showError(String.format(ORDER_UPDATE_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    private void handleOrderModified(Order order) {
        handleOrderClicked(order);
    }

    private void handleOrderCancelRequest(Order order) {
        if (chartOrderService == null) {
            StatusBar.showError(ORDER_NO_ACCOUNT);
            return;
        }
        float price = order.getPrice() > 0 ? order.getPrice() : order.getTriggerPrice();
        String message = String.format(ORDER_CANCEL_MESSAGE, price);
        ConfirmationDialog dialog = new ConfirmationDialog(ORDER_CANCEL_TITLE, message, ORDER_CANCEL_CONFIRM);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            StatusBar.showInfo(ORDER_CANCELLING);
            CompletableFuture.runAsync(() -> {
                try {
                    chartOrderService.cancelOrder(order);
                    StatusBar.showSuccess(ORDER_CANCELLED);
                    Platform.runLater(() -> { refreshRegularOrders(); fireOrderBookRefresh(); });
                } catch (Exception ex) {
                    log.error("Failed to cancel order", ex);
                    StatusBar.showError(String.format(ORDER_CANCEL_FAILED, extractMessage(ex)));
                }
            });
        }
    }

    public void refreshRegularOrders() {
        if (chartOrderService == null) return;
        StatusBar.showInfo(ORDER_REFRESHING);
        CompletableFuture.supplyAsync(() -> {
            try {
                return chartOrderService.fetchOrders();
            } catch (Exception ex) {
                log.error("Failed to fetch orders", ex);
                StatusBar.showError(String.format(ORDER_REFRESH_FAILED, extractMessage(ex)));
                return List.<Order>of();
            }
        }).thenAccept(orders -> Platform.runLater(() -> {
            allRegularOrders = orders;
            filterRegularOrdersForActiveScrip();
            StatusBar.showSuccess(String.format(ORDER_REFRESHED, activeRegularOrders.size()));
        }));
    }

    private void filterRegularOrdersForActiveScrip() {
        String scripId = activeScrip != null ? activeScrip.getId() : null;
        activeRegularOrders = allRegularOrders.stream()
                .filter(o -> !o.getStatus().isTerminal())
                .filter(o -> scripId == null || scripId.equals(o.getScripId()))
                .toList();
        canvas.setRegularOrders(activeRegularOrders);
        canvas.render();
    }

    private void fireOrderBookRefresh() {
        if (onOrderBookRefresh != null) onOrderBookRefresh.run();
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

    private float getLastClosePrice() {
        if (lastBars == null || lastBars.size() == 0) return 0;
        return lastBars.getClose(lastBars.size() - 1);
    }

    private void loadData(String scripId, Timeframe timeframe) {
        try {
            int totalBars = dataProvider.configure(scripId, timeframe);
            if (totalBars == 0) {
                showEmptyState();
                return;
            }
            if (exampleCutoffTimestamp > 0) {
                int cutoffBars = findCutoffBarCount(scripId, timeframe, totalBars);
                if (cutoffBars > 0) {
                    totalBars = cutoffBars;
                    dataProvider.invalidateCache();
                    dataProvider.setMaxBars(totalBars);
                }
            }
            viewport.configure(totalBars);
            canvas.setTimeframe(timeframe);
            subChartCanvases.forEach(sc -> sc.setActiveTimeframe(timeframe));
            lastBars = dataProvider.fetchBars(0, totalBars);
            computeIndicators();
            computeScreenMarkers();
            computeTrendlines();
            showCanvas();
            computeSubChartIndicators();
            canvas.render();
            renderSubCharts();
        } catch (Exception e) {
            log.error("Failed to load chart data for {} {}", scripId, timeframe, e);
            showEmptyState();
        }
    }

    private int findCutoffBarCount(String scripId, Timeframe timeframe, int totalBars) throws java.io.IOException {
        Bars allBars = dataProvider.fetchBars(0, totalBars);
        for (int i = allBars.size() - 1; i >= 0; i--) {
            if (allBars.getTimestamp(i) <= exampleCutoffTimestamp) {
                return i + 1;
            }
        }
        return 0;
    }

    private void resetVolumeProfileIndicators() {
        for (ActiveIndicator ai : activeIndicators) {
            if (ai.getIndicator() instanceof VolumeProfileIndicator vpi) {
                vpi.reset();
            }
        }
    }

    private void onProfileRangeChanged() {
        computeIndicators();
        canvas.render();
    }

    private void computeIndicators() {
        if (lastBars == null || lastBars.size() == 0) {
            canvas.setOverlayResults(List.of());
            canvas.setVolumeProfileData(null);
            return;
        }
        List<IndicatorResult> results = new ArrayList<>();
        VolumeProfileData profileData = null;
        int viewStart = viewport.getStartIndex();
        int viewEnd = viewport.getEndIndex();
        for (ActiveIndicator ai : activeIndicators) {
            if (ai.getIndicator() instanceof VolumeProfileIndicator vpi) {
                vpi.setViewportRange(viewStart, viewEnd);
            }
            results.add(ai.compute(lastBars));
            if (ai.getIndicator() instanceof VolumeProfileIndicator vpi) {
                profileData = vpi.getLastData();
            }
        }
        lastOverlayResults = results;
        canvas.setOverlayResults(results);
        canvas.setVolumeProfileData(profileData);
    }

    private void computeSubChartIndicators() {
        if (lastBars == null || lastBars.size() == 0) return;
        for (int i = 0; i < activeSubChartIndicators.size(); i++) {
            SubChartResult result = activeSubChartIndicators.get(i).compute(lastBars);
            if (i < subChartCanvases.size()) {
                subChartCanvases.get(i).setResult(result);
            }
        }
    }

    private void renderSubCharts() {
        for (SubChartCanvas sc : subChartCanvases) {
            sc.render();
        }
    }

    private void onMainChartHoverChanged() {
        syncMouseXToSubCharts();
        updateIndicatorValues();
        updateSubChartValues();
    }

    private void onSubChartHoverChanged() {
        updateSubChartValues();
    }

    private void onSubChartViewportChanged() {
        canvas.render();
        renderSubCharts();
    }

    private void onDrawingComplete() {
        if (activeDrawingButton != null) {
            activeDrawingButton.setSelected(false);
            activeDrawingButton.getStyleClass().remove(DRAWING_BUTTON_ACTIVE_STYLE);
            activeDrawingButton = null;
        }
    }

    private void syncMouseXToSubCharts() {
        double mx = canvas.getMouseX();
        for (SubChartCanvas sc : subChartCanvases) {
            sc.setMainMouseX(mx);
            sc.render();
        }
    }

    private void buildLayout() {
        setBottom(buildToolbar());
        indicatorListBar.getStyleClass().add(INDICATOR_LIST_STYLE);
        indicatorListBar.setAlignment(Pos.TOP_LEFT);
        indicatorListBar.setPadding(new Insets(0, 8, 0, 8));
        indicatorListBar.setPickOnBounds(false);
        indicatorListBar.setMaxHeight(Region.USE_PREF_SIZE);
        indicatorListBar.setMaxWidth(Region.USE_PREF_SIZE);
        canvasContainer.getStyleClass().add(CHART_PANE_STYLE);
        setCenter(canvasContainer);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(6);
        toolbar.getStyleClass().add(CHART_TOOLBAR_STYLE);
        HBox timeframeGroup = buildButtonGroup();
        timeframeToggleGroup = new ToggleGroup();
        for (Timeframe tf : Timeframe.values()) {
            ToggleButton btn = new ToggleButton(tf.getCode());
            btn.getStyleClass().add(TIMEFRAME_BUTTON_STYLE);
            btn.setToggleGroup(timeframeToggleGroup);
            btn.setFocusTraversable(false);
            btn.setUserData(tf);
            if (tf == activeTimeframe) {
                btn.setSelected(true);
                btn.getStyleClass().add(TIMEFRAME_BUTTON_ACTIVE_STYLE);
            }
            btn.setOnAction(e -> switchTimeframe(tf, btn, timeframeToggleGroup));
            timeframeGroup.getChildren().add(btn);
        }
        HBox chartGroup = buildButtonGroup();
        ToggleButton logBtn = createToolbarToggle(LOG_SCALE_LABEL, FluentUiRegularMZ.SCALE_FIT_16);
        logBtn.setOnAction(e -> toggleLogScale(logBtn.isSelected()));
        Button indicatorBtn = createToolbarButton(INDICATORS_LABEL, FluentUiRegularAL.DATA_LINE_24);
        indicatorBtn.setOnAction(e -> showAddIndicatorPopup(indicatorBtn));
        Button screenBtn = createToolbarButton(SCREENS_LABEL, FluentUiRegularAL.FILTER_20);
        screenBtn.setOnAction(e -> showAddScreenPopup(screenBtn));
        Button trendlineBtn = createToolbarButton(TRENDLINES_LABEL, FluentUiRegularMZ.TIMELINE_24);
        trendlineBtn.setOnAction(e -> onTrendlineButtonClicked());
        chartGroup.getChildren().addAll(logBtn, indicatorBtn, screenBtn, trendlineBtn);
        HBox orderGroup = buildOrderActionButtons();
        HBox rulerGroup = buildRulerButton();
        HBox drawingGroup = buildDrawingToolButtons();
        Region s1 = new Region();
        Region s2 = new Region();
        Region s3 = new Region();
        Region s4 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox.setHgrow(s4, Priority.ALWAYS);
        toolbar.getChildren().addAll(timeframeGroup, s1, chartGroup, s2, orderGroup, s3,
                rulerGroup, s4, drawingGroup);
        return toolbar;
    }

    private HBox buildRulerButton() {
        HBox box = buildButtonGroup();
        ToggleButton btn = new ToggleButton();
        btn.setGraphic(DrawingToolIcons.createIcon(DrawingTool.RULER));
        btn.setTooltip(new Tooltip(DrawingTool.RULER.getLabel()));
        btn.getStyleClass().add(DRAWING_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> onDrawingToolSelected(DrawingTool.RULER, btn));
        box.getChildren().add(btn);
        return box;
    }

    private HBox buildDrawingToolButtons() {
        HBox box = buildButtonGroup();
        for (DrawingTool tool : DrawingTool.values()) {
            if (tool == DrawingTool.RULER) continue;
            ToggleButton btn = new ToggleButton();
            btn.setGraphic(DrawingToolIcons.createIcon(tool));
            btn.setTooltip(new Tooltip(tool.getLabel()));
            btn.getStyleClass().add(DRAWING_BUTTON_STYLE);
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> onDrawingToolSelected(tool, btn));
            box.getChildren().add(btn);
        }
        Button clearBtn = new Button();
        FontIcon clearIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
        clearIcon.setIconSize(CLEAR_ICON_SIZE);
        clearIcon.getStyleClass().add(CLEAR_DRAWINGS_ICON_STYLE);
        clearBtn.setGraphic(clearIcon);
        clearBtn.setTooltip(new Tooltip(CLEAR_DRAWINGS_LABEL));
        clearBtn.getStyleClass().add(DRAWING_BUTTON_STYLE);
        clearBtn.setFocusTraversable(false);
        clearBtn.setOnAction(e -> {
            drawingManager.clearAll();
            canvas.render();
            renderSubCharts();
        });
        box.getChildren().add(clearBtn);
        return box;
    }

    private HBox buildOrderActionButtons() {
        HBox box = buildButtonGroup();
        Button buyBtn = createOrderActionButton(ORDER_BUY_LABEL, FluentUiRegularAL.CART_16,
                () -> startPricePicker(PricePickerMode.BUY));
        Button sellBtn = createOrderActionButton(ORDER_SELL_LABEL, FluentUiRegularAL.CART_16,
                () -> startPricePicker(PricePickerMode.SELL));
        Button gttBuyBtn = createOrderActionButton(GTT_BUY_LABEL, FluentUiRegularAL.ARROW_UP_20,
                () -> startPricePicker(PricePickerMode.GTT_BUY));
        Button gttSellBtn = createOrderActionButton(GTT_SELL_LABEL, FluentUiRegularAL.ARROW_DOWN_20,
                () -> startPricePicker(PricePickerMode.GTT_SELL));
        ocoButton = createOrderActionButton(OCO_LABEL, FluentUiRegularAL.ARROW_SORT_20,
                this::startOcoPricePicker);
        ocoButton.setDisable(true);
        box.getChildren().addAll(buyBtn, sellBtn, gttBuyBtn, gttSellBtn, ocoButton);
        return box;
    }

    private Button createOrderActionButton(String label, Ikon ikon, Runnable action) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(ORDER_BUTTON_ICON_SIZE);
        Button btn = new Button(label);
        btn.setGraphic(icon);
        btn.getStyleClass().add(ORDER_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private HBox buildButtonGroup() {
        HBox group = new HBox(0);
        group.getStyleClass().add(BUTTON_GROUP_STYLE);
        group.setAlignment(Pos.CENTER);
        return group;
    }

    private Button createToolbarButton(String label, Ikon ikon) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TOOLBAR_ICON_SIZE);
        Button btn = new Button(label);
        btn.setGraphic(icon);
        btn.getStyleClass().add(TIMEFRAME_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        return btn;
    }

    private ToggleButton createToolbarToggle(String label, Ikon ikon) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TOOLBAR_ICON_SIZE);
        ToggleButton btn = new ToggleButton(label);
        btn.setGraphic(icon);
        btn.getStyleClass().add(TIMEFRAME_BUTTON_STYLE);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void onDrawingToolSelected(DrawingTool tool, ToggleButton btn) {
        if (activeDrawingButton != null && activeDrawingButton != btn) {
            activeDrawingButton.setSelected(false);
            activeDrawingButton.getStyleClass().remove(DRAWING_BUTTON_ACTIVE_STYLE);
        }
        if (btn.isSelected()) {
            drawingManager.setActiveTool(tool);
            btn.getStyleClass().add(DRAWING_BUTTON_ACTIVE_STYLE);
            activeDrawingButton = btn;
        } else {
            drawingManager.clearActiveTool();
            btn.getStyleClass().remove(DRAWING_BUTTON_ACTIVE_STYLE);
            activeDrawingButton = null;
        }
    }

    private void showAddIndicatorPopup(Button anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox content = new VBox(2);
        content.getStyleClass().add(INDICATOR_POPUP_STYLE);
        content.setPadding(new Insets(6));
        TextField searchField = new TextField();
        searchField.getStyleClass().add(POPUP_SEARCH_STYLE);
        searchField.setPromptText(SEARCH_INDICATOR_PROMPT);
        VBox itemsBox = new VBox(2);
        Label loadingLabel = new Label(LOADING_LABEL);
        loadingLabel.getStyleClass().add(INDICATOR_ADD_ITEM_STYLE);
        itemsBox.getChildren().add(loadingLabel);
        ScrollPane scrollPane = createPopupScrollPane(itemsBox);
        content.getChildren().addAll(searchField, scrollPane);
        popup.getContent().add(content);
        Window window = anchor.getScene().getWindow();
        double anchorX = anchor.localToScreen(0, 0).getX();
        double anchorY = anchor.localToScreen(0, anchor.getHeight()).getY();
        popup.show(window, anchorX, anchorY);
        searchField.requestFocus();
        CompletableFuture.supplyAsync(() -> {
            List<OverlayIndicator> overlays = indicatorRegistry.getOverlayIndicators();
            List<SubChartIndicator> subCharts = indicatorRegistry.getSubChartIndicators();
            return new Object[]{overlays, subCharts};
        }).thenAccept(result -> Platform.runLater(() -> {
            if (!popup.isShowing()) return;
            @SuppressWarnings("unchecked")
            List<OverlayIndicator> overlays = (List<OverlayIndicator>) result[0];
            @SuppressWarnings("unchecked")
            List<SubChartIndicator> subCharts = (List<SubChartIndicator>) result[1];
            Runnable refreshItems = () -> {
                String query = searchField.getText();
                itemsBox.getChildren().clear();
                for (OverlayIndicator ind : overlays) {
                    if (matchesSearch(ind.getName(), query)) {
                        itemsBox.getChildren().add(buildOverlayPopupItem(ind, popup));
                    }
                }
                for (SubChartIndicator ind : subCharts) {
                    if (matchesSearch(ind.getName(), query)) {
                        itemsBox.getChildren().add(buildSubChartPopupItem(ind, popup));
                    }
                }
            };
            refreshItems.run();
            searchField.textProperty().addListener((obs, o, n) -> refreshItems.run());
        }));
    }

    private Button buildOverlayPopupItem(OverlayIndicator ind, Popup popup) {
        Button addBtn = new Button(ADD_LABEL + ind.getName());
        addBtn.getStyleClass().add(INDICATOR_ADD_ITEM_STYLE);
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> {
            if (ind instanceof VolumeProfileIndicator vpi) vpi.reset();
            Color color = INDICATOR_PALETTE[activeIndicators.size() % INDICATOR_PALETTE.length];
            activeIndicators.add(new ActiveIndicator(ind, color));
            refreshIndicatorList();
            popup.hide();
            computeIndicatorsAsync();
        });
        return addBtn;
    }

    private Button buildSubChartPopupItem(SubChartIndicator ind, Popup popup) {
        Button addBtn = new Button(ADD_LABEL + ind.getName());
        addBtn.getStyleClass().add(INDICATOR_ADD_ITEM_STYLE);
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> {
            int totalCount = activeIndicators.size() + activeSubChartIndicators.size();
            Color color = INDICATOR_PALETTE[totalCount % INDICATOR_PALETTE.length];
            activeSubChartIndicators.add(new ActiveSubChartIndicator(ind, color));
            rebuildSubChartPanes();
            popup.hide();
            computeSubChartIndicatorsAsync();
        });
        return addBtn;
    }

    private void refreshIndicatorList() {
        indicatorListBar.getChildren().clear();
        indicatorValueLabels.clear();
        for (int i = 0; i < activeIndicators.size(); i++) {
            indicatorListBar.getChildren().add(buildIndicatorTag(activeIndicators.get(i), i));
        }
        updateIndicatorValues();
        refreshScreenTag();
    }

    private HBox buildIndicatorTag(ActiveIndicator ai, int index) {
        HBox tag = new HBox(2);
        tag.getStyleClass().add(INDICATOR_TAG_STYLE);
        tag.setAlignment(Pos.CENTER_LEFT);
        String colorHex = toHex(ai.getColor());
        String colorStyle = TEXT_FILL_PREFIX + colorHex + STYLE_SUFFIX;
        Button deleteBtn = createTagButton(FluentUiRegularAL.DELETE_16, INDICATOR_TAG_DELETE_STYLE);
        deleteBtn.setOnAction(e -> removeIndicator(index));
        Label nameLabel = new Label(ai.getDisplayLabel());
        nameLabel.getStyleClass().add(INDICATOR_TAG_NAME_STYLE);
        nameLabel.setStyle(colorStyle);
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add(INDICATOR_TAG_VALUE_STYLE);
        valueLabel.setStyle(colorStyle);
        indicatorValueLabels.add(valueLabel);
        tag.getChildren().add(deleteBtn);
        if (!ai.getSettings().isEmpty()) {
            Button editBtn = createTagButton(FluentUiRegularAL.EDIT_16, INDICATOR_TAG_EDIT_STYLE);
            editBtn.setOnAction(e -> openSettingsDialog(ai));
            tag.getChildren().add(editBtn);
        }
        tag.getChildren().addAll(nameLabel, valueLabel);
        return tag;
    }

    private void updateIndicatorValues() {
        int hoveredIdx = canvas.getHoveredBarIndex();
        for (int i = 0; i < activeIndicators.size(); i++) {
            if (i >= indicatorValueLabels.size()) break;
            Label valueLabel = indicatorValueLabels.get(i);
            if (hoveredIdx < 0 || lastBars == null || i >= lastOverlayResults.size()) {
                valueLabel.setText("");
                continue;
            }
            IndicatorResult result = lastOverlayResults.get(i);
            if (result == null || hoveredIdx >= result.getValues().length
                    || Double.isNaN(result.getValues()[hoveredIdx])) {
                valueLabel.setText("");
                continue;
            }
            double val = result.getValues()[hoveredIdx];
            valueLabel.setText(formatIndicatorValue(val));
        }
    }

    private HBox buildSubChartTag(ActiveSubChartIndicator ai, int index) {
        HBox tag = new HBox(2);
        tag.getStyleClass().add(INDICATOR_TAG_STYLE);
        tag.setAlignment(Pos.CENTER_LEFT);
        tag.setMaxHeight(Region.USE_PREF_SIZE);
        tag.setMaxWidth(Region.USE_PREF_SIZE);
        tag.setPadding(new Insets(0, 8, 0, 8));
        String colorHex = toHex(ai.getColor());
        String colorStyle = TEXT_FILL_PREFIX + colorHex + STYLE_SUFFIX;
        Button deleteBtn = createTagButton(FluentUiRegularAL.DELETE_16, INDICATOR_TAG_DELETE_STYLE);
        deleteBtn.setOnAction(e -> removeSubChartIndicator(index));
        Label nameLabel = new Label(ai.getDisplayLabel());
        nameLabel.getStyleClass().add(INDICATOR_TAG_NAME_STYLE);
        nameLabel.setStyle(colorStyle);
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add(INDICATOR_TAG_VALUE_STYLE);
        valueLabel.setStyle(colorStyle);
        subChartValueLabels.add(valueLabel);
        tag.getChildren().add(deleteBtn);
        if (!ai.getSettings().isEmpty()) {
            Button editBtn = createTagButton(FluentUiRegularAL.EDIT_16, INDICATOR_TAG_EDIT_STYLE);
            editBtn.setOnAction(e -> openSubChartSettingsDialog(ai));
            tag.getChildren().add(editBtn);
        }
        tag.getChildren().addAll(nameLabel, valueLabel);
        return tag;
    }

    private void updateSubChartValues() {
        for (int i = 0; i < activeSubChartIndicators.size(); i++) {
            if (i >= subChartValueLabels.size() || i >= subChartCanvases.size()) break;
            Label valueLabel = subChartValueLabels.get(i);
            int hoveredIdx = subChartCanvases.get(i).getHoveredBarIndex();
            SubChartResult result = subChartCanvases.get(i).getResult();
            if (hoveredIdx < 0 || result == null || hoveredIdx >= result.getValues().length
                    || Double.isNaN(result.getValues()[hoveredIdx])) {
                valueLabel.setText("");
                continue;
            }
            double val = result.getValues()[hoveredIdx];
            valueLabel.setText(formatIndicatorValue(val));
        }
    }

    private void openSubChartSettingsDialog(ActiveSubChartIndicator ai) {
        Window owner = getScene().getWindow();
        new IndicatorSettingsDialog(ai, scripRepository, () -> {
            computeSubChartIndicators();
            refreshIndicatorList();
            renderSubCharts();
        }).show(owner);
    }

    private static String formatIndicatorValue(double value) {
        return value >= 1000 ? String.format("  %.0f", value) : String.format("  %.2f", value);
    }

    private static boolean matchesSearch(String name, String query) {
        if (query == null || query.length() < MIN_SEARCH_LENGTH) return true;
        return name.toLowerCase().contains(query.toLowerCase());
    }

    private Button createTagButton(FluentUiRegularAL ikon, String styleClass) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TAG_ICON_SIZE);
        icon.getStyleClass().add(styleClass);
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add(styleClass);
        btn.setFocusTraversable(false);
        return btn;
    }


    private void openSettingsDialog(ActiveIndicator ai) {
        Window owner = getScene().getWindow();
        new IndicatorSettingsDialog(ai, scripRepository, () -> {
            computeIndicators();
            refreshIndicatorList();
            canvas.render();
        }).show(owner);
    }

    private void removeIndicator(int index) {
        activeIndicators.remove(index);
        computeIndicators();
        refreshIndicatorList();
        canvas.render();
    }

    private void showAddScreenPopup(Button anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox content = new VBox(2);
        content.getStyleClass().add(INDICATOR_POPUP_STYLE);
        content.setPadding(new Insets(6));
        TextField searchField = new TextField();
        searchField.getStyleClass().add(POPUP_SEARCH_STYLE);
        searchField.setPromptText(SEARCH_SCREEN_PROMPT);
        VBox itemsBox = new VBox(2);
        Label loadingLabel = new Label(LOADING_LABEL);
        loadingLabel.getStyleClass().add(INDICATOR_ADD_ITEM_STYLE);
        itemsBox.getChildren().add(loadingLabel);
        ScrollPane scrollPane = createPopupScrollPane(itemsBox);
        content.getChildren().addAll(searchField, scrollPane);
        popup.getContent().add(content);
        Window window = anchor.getScene().getWindow();
        double anchorX = anchor.localToScreen(0, 0).getX();
        double anchorY = anchor.localToScreen(0, anchor.getHeight()).getY();
        popup.show(window, anchorX, anchorY);
        searchField.requestFocus();
        CompletableFuture.supplyAsync(() -> screenRegistry.getScreens())
                .thenAccept(screens -> Platform.runLater(() -> {
                    if (!popup.isShowing()) return;
                    Runnable refreshItems = () -> {
                        String query = searchField.getText();
                        itemsBox.getChildren().clear();
                        for (Screen screen : screens) {
                            if (matchesSearch(screen.getName(), query)) {
                                Button addBtn = new Button(ADD_LABEL + screen.getName());
                                addBtn.getStyleClass().add(INDICATOR_ADD_ITEM_STYLE);
                                addBtn.setFocusTraversable(false);
                                addBtn.setOnAction(e -> {
                                    onScreenSelected(screen);
                                    popup.hide();
                                });
                                itemsBox.getChildren().add(addBtn);
                            }
                        }
                    };
                    refreshItems.run();
                    searchField.textProperty().addListener((obs, o, n) -> refreshItems.run());
                }));
    }

    private ScrollPane createPopupScrollPane(VBox itemsBox) {
        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(MAX_VISIBLE_POPUP_ITEMS * POPUP_ITEM_HEIGHT);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private void onScreenSelected(Screen screen) {
        ScreenConfigDialog dialog = new ScreenConfigDialog(screen);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            activeScreen = screen;
            refreshScreenTag();
            computeScreenMarkersAsync();
        }
    }

    private void computeScreenMarkers() {
        if (activeScreen == null || lastBars == null || lastBars.size() == 0) {
            canvas.setScreenMarkers(null);
            return;
        }
        int size = lastBars.size();
        boolean[] markers = new boolean[size];
        BarsView view = new BarsView(lastBars);
        for (int i = 0; i < size; i++) {
            view.setViewSize(i + 1);
            markers[i] = activeScreen.matches(activeScrip, view);
        }
        canvas.setScreenMarkers(markers);
    }

    private void computeIndicatorsAsync() {
        Bars bars = lastBars;
        List<ActiveIndicator> snapshot = new ArrayList<>(activeIndicators);
        CompletableFuture.supplyAsync(() -> {
            if (bars == null || bars.size() == 0) return List.<IndicatorResult>of();
            List<IndicatorResult> results = new ArrayList<>();
            for (ActiveIndicator ai : snapshot) {
                results.add(ai.compute(bars));
            }
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            lastOverlayResults = results;
            canvas.setOverlayResults(results);
            canvas.setVolumeProfileData(extractVolumeProfileData(snapshot));
            canvas.render();
        }));
    }

    private void computeSubChartIndicatorsAsync() {
        Bars bars = lastBars;
        List<ActiveSubChartIndicator> snapshot = new ArrayList<>(activeSubChartIndicators);
        CompletableFuture.supplyAsync(() -> {
            if (bars == null || bars.size() == 0) return List.<SubChartResult>of();
            List<SubChartResult> results = new ArrayList<>();
            for (ActiveSubChartIndicator asi : snapshot) {
                results.add(asi.compute(bars));
            }
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            for (int i = 0; i < results.size() && i < subChartCanvases.size(); i++) {
                subChartCanvases.get(i).setResult(results.get(i));
            }
            renderSubCharts();
        }));
    }

    private void computeScreenMarkersAsync() {
        Screen screen = activeScreen;
        Bars bars = lastBars;
        Scrip scrip = activeScrip;
        CompletableFuture.supplyAsync(() -> {
            if (screen == null || bars == null || bars.size() == 0) return null;
            int size = bars.size();
            boolean[] markers = new boolean[size];
            BarsView view = new BarsView(bars);
            for (int i = 0; i < size; i++) {
                view.setViewSize(i + 1);
                markers[i] = screen.matches(scrip, view);
            }
            return markers;
        }).thenAccept(markers -> Platform.runLater(() -> {
            canvas.setScreenMarkers(markers);
            canvas.render();
        }));
    }

    private VolumeProfileData extractVolumeProfileData(List<ActiveIndicator> indicators) {
        for (ActiveIndicator ai : indicators) {
            if (ai.getIndicator() instanceof VolumeProfileIndicator vpi) {
                return vpi.getLastData();
            }
        }
        return null;
    }

    private void refreshScreenTag() {
        indicatorListBar.getChildren().removeIf(n -> n.getStyleClass().contains(SCREEN_TAG_STYLE));
        if (activeScreen != null) {
            indicatorListBar.getChildren().add(buildScreenTag());
        }
    }

    private HBox buildScreenTag() {
        HBox tag = new HBox(2);
        tag.getStyleClass().add(SCREEN_TAG_STYLE);
        tag.setAlignment(Pos.CENTER_LEFT);
        Button deleteBtn = createTagButton(FluentUiRegularAL.DELETE_16, SCREEN_TAG_DELETE_STYLE);
        deleteBtn.setOnAction(e -> removeScreen());
        Label nameLabel = new Label(activeScreen.getName());
        nameLabel.getStyleClass().add(SCREEN_TAG_NAME_STYLE);
        tag.getChildren().add(deleteBtn);
        if (!activeScreen.getSettings().isEmpty()) {
            Button editBtn = createTagButton(FluentUiRegularAL.EDIT_16, SCREEN_TAG_EDIT_STYLE);
            editBtn.setOnAction(e -> openScreenSettingsDialog());
            tag.getChildren().add(editBtn);
        }
        tag.getChildren().add(nameLabel);
        return tag;
    }

    private void openScreenSettingsDialog() {
        ScreenConfigDialog dialog = new ScreenConfigDialog(activeScreen);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            refreshScreenTag();
            computeScreenMarkersAsync();
        }
    }

    private void removeScreen() {
        activeScreen = null;
        canvas.setScreenMarkers(null);
        refreshScreenTag();
        canvas.render();
    }

    private void refreshWatchlistPills() {
        watchlistPillsBar.getChildren().clear();
        if (activeScrip == null || watchlistPane == null) return;
        String scripId = activeScrip.getId();
        for (Watchlist wl : watchlistPane.getWatchlists()) {
            if (wl.containsScrip(scripId)) {
                Label pill = new Label(wl.getName());
                pill.getStyleClass().add(WATCHLIST_PILL_STYLE);
                watchlistPillsBar.getChildren().add(pill);
            }
        }
    }

    public void refreshScriptedComponents() {
        computeIndicatorsAsync();
        computeScreenMarkersAsync();
    }

    private void onViewportChanged() {
        computeTrendlines();
        renderSubCharts();
    }

    private void onTrendlineButtonClicked() {
        TrendlineConfigDialog dialog = new TrendlineConfigDialog(trendlineSettings);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            trendlinesActive = true;
            computeTrendlines();
            refreshTrendlineTag();
            canvas.render();
        }
    }

    private void computeTrendlines() {
        if (!trendlinesActive || lastBars == null || lastBars.size() == 0) {
            canvas.setTrendlines(null);
            return;
        }
        Bars visibleBars = extractVisibleBars();
        if (visibleBars.size() == 0) {
            canvas.setTrendlines(null);
            return;
        }
        TrendlineDetector detector = trendlineSettings.buildDetector();
        List<Trendline> lines = new ArrayList<>();
        if (trendlineSettings.isShowResistance()) {
            lines.addAll(filterByScore(detector.findResistanceLines(visibleBars)));
        }
        if (trendlineSettings.isShowSupport()) {
            lines.addAll(filterByScore(detector.findSupportLines(visibleBars)));
        }
        canvas.setTrendlines(lines);
    }

    private Bars extractVisibleBars() {
        int viewStart = Math.max(0, viewport.getStartIndex());
        int viewEnd = Math.min(viewport.getEndIndex(), lastBars.size());
        int count = viewEnd - viewStart;
        if (count <= 0) return new Bars(lastBars.getScripId(), lastBars.getTimeframe(), 0);
        Bars window = new Bars(lastBars.getScripId(), lastBars.getTimeframe(), count);
        for (int i = viewStart; i < viewEnd; i++) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= lastBars.size()) continue;
            window.append(lastBars.getTimestamp(idx), lastBars.getOpen(idx), lastBars.getHigh(idx),
                    lastBars.getLow(idx), lastBars.getClose(idx), lastBars.getVolume(idx));
        }
        return window;
    }

    private List<Trendline> filterByScore(List<Trendline> lines) {
        return lines.stream()
                .filter(l -> l.getScore() >= trendlineSettings.getMinScore())
                .toList();
    }

    private void refreshTrendlineTag() {
        indicatorListBar.getChildren().removeIf(n -> n.getStyleClass().contains(TRENDLINE_TAG_STYLE)
                && n.getUserData() != null && TRENDLINES_LABEL.equals(n.getUserData()));
        if (trendlinesActive) {
            indicatorListBar.getChildren().add(buildTrendlineTag());
        }
    }

    private HBox buildTrendlineTag() {
        HBox tag = new HBox(2);
        tag.getStyleClass().add(TRENDLINE_TAG_STYLE);
        tag.setUserData(TRENDLINES_LABEL);
        tag.setAlignment(Pos.CENTER_LEFT);
        Button deleteBtn = createTagButton(FluentUiRegularAL.DELETE_16, TRENDLINE_TAG_DELETE_STYLE);
        deleteBtn.setOnAction(e -> removeTrendlines());
        Button editBtn = createTagButton(FluentUiRegularAL.EDIT_16, TRENDLINE_TAG_EDIT_STYLE);
        editBtn.setOnAction(e -> openTrendlineSettingsDialog());
        Label nameLabel = new Label(TRENDLINES_LABEL);
        nameLabel.getStyleClass().add(TRENDLINE_TAG_NAME_STYLE);
        tag.getChildren().addAll(deleteBtn, editBtn, nameLabel);
        return tag;
    }

    private void openTrendlineSettingsDialog() {
        TrendlineConfigDialog dialog = new TrendlineConfigDialog(trendlineSettings);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            computeTrendlines();
            refreshTrendlineTag();
            canvas.render();
        }
    }

    private void removeTrendlines() {
        trendlinesActive = false;
        canvas.setTrendlines(null);
        refreshTrendlineTag();
        canvas.render();
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void switchTimeframe(Timeframe timeframe, ToggleButton selected, ToggleGroup group) {
        if (timeframe == activeTimeframe && exampleCutoffTimestamp < 0) return;
        exampleCutoffTimestamp = -1;
        group.getToggles().forEach(t -> {
            ToggleButton tb = (ToggleButton) t;
            tb.getStyleClass().remove(TIMEFRAME_BUTTON_ACTIVE_STYLE);
        });
        selected.getStyleClass().add(TIMEFRAME_BUTTON_ACTIVE_STYLE);
        activeTimeframe = timeframe;
        if (activeScrip != null) {
            loadData(activeScrip.getId(), timeframe);
        }
    }

    private void selectTimeframeButton(Timeframe timeframe) {
        activeTimeframe = timeframe;
        timeframeToggleGroup.getToggles().forEach(t -> {
            ToggleButton tb = (ToggleButton) t;
            tb.getStyleClass().remove(TIMEFRAME_BUTTON_ACTIVE_STYLE);
            if (tb.getUserData() == timeframe) {
                tb.setSelected(true);
                tb.getStyleClass().add(TIMEFRAME_BUTTON_ACTIVE_STYLE);
            }
        });
    }

    private void showCanvas() {
        Pane mainWrapper = createResizableCanvasWrapper(canvas, () -> canvas.render());
        VBox.setVgrow(mainWrapper, Priority.ALWAYS);
        chartStack.getChildren().clear();
        chartStack.getChildren().add(mainWrapper);
        rebuildSubChartPanes();
        HBox zoomControls = buildZoomControls();
        StackPane overlay = new StackPane(chartStack, indicatorListBar, watchlistPillsBar, zoomControls);
        StackPane.setAlignment(indicatorListBar, Pos.TOP_LEFT);
        StackPane.setMargin(indicatorListBar, new Insets(INDICATOR_LIST_TOP_OFFSET, 0, 0, 0));
        StackPane.setAlignment(watchlistPillsBar, Pos.TOP_RIGHT);
        StackPane.setMargin(watchlistPillsBar, new Insets(WATCHLIST_PILLS_TOP_OFFSET, WATCHLIST_PILLS_RIGHT_OFFSET, 0, 0));
        StackPane.setAlignment(zoomControls, Pos.BOTTOM_CENTER);
        StackPane.setMargin(zoomControls, new Insets(0, 0, ZOOM_MARGIN_BOTTOM, 0));
        canvasContainer.getChildren().setAll(overlay);
        refreshIndicatorList();
        refreshScreenTag();
        refreshTrendlineTag();
        refreshWatchlistPills();
    }

    private HBox buildZoomControls() {
        Button zoomInBtn = new Button(ZOOM_IN_TEXT);
        zoomInBtn.getStyleClass().add(ZOOM_BUTTON_STYLE);
        zoomInBtn.setFocusTraversable(false);
        zoomInBtn.setOnAction(e -> {
            viewport.zoomBy(-ZOOM_DELTA);
            canvas.render();
            renderSubCharts();
        });
        Button zoomOutBtn = new Button(ZOOM_OUT_TEXT);
        zoomOutBtn.getStyleClass().add(ZOOM_BUTTON_STYLE);
        zoomOutBtn.setFocusTraversable(false);
        zoomOutBtn.setOnAction(e -> {
            viewport.zoomBy(ZOOM_DELTA);
            canvas.render();
            renderSubCharts();
        });
        HBox box = new HBox(ZOOM_BUTTON_SPACING, zoomInBtn, zoomOutBtn);
        box.setAlignment(Pos.CENTER);
        box.setPickOnBounds(false);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return box;
    }

    private void rebuildSubChartPanes() {
        subChartCanvases.clear();
        subChartValueLabels.clear();
        chartStack.getChildren().removeIf(n -> n.getStyleClass().contains(SUB_CHART_WRAPPER_STYLE));
        for (int i = 0; i < activeSubChartIndicators.size(); i++) {
            SubChartCanvas sc = new SubChartCanvas(viewport, dataProvider);
            sc.setCanvasId(i);
            sc.setActiveTimeframe(activeTimeframe);
            sc.setDrawingManager(drawingManager);
            sc.setOnDrawingComplete(this::onDrawingComplete);
            sc.setOnHoverChanged(this::onSubChartHoverChanged);
            sc.setOnViewportChanged(this::onSubChartViewportChanged);
            subChartCanvases.add(sc);
            Pane canvasPane = createResizableCanvasWrapper(sc, sc::render);
            HBox tag = buildSubChartTag(activeSubChartIndicators.get(i), i);
            StackPane wrapper = new StackPane(canvasPane, tag);
            StackPane.setAlignment(tag, Pos.TOP_LEFT);
            StackPane.setMargin(tag, new Insets(SEPARATOR_TAG_TOP_OFFSET, 0, 0, 0));
            wrapper.getStyleClass().add(SUB_CHART_WRAPPER_STYLE);
            wrapper.setPrefHeight(SUB_CHART_HEIGHT);
            wrapper.setMinHeight(SUB_CHART_HEIGHT);
            wrapper.setMaxHeight(SUB_CHART_HEIGHT);
            chartStack.getChildren().add(wrapper);
        }
    }

    private void removeSubChartIndicator(int index) {
        activeSubChartIndicators.remove(index);
        rebuildSubChartPanes();
        computeSubChartIndicators();
        refreshIndicatorList();
        renderSubCharts();
    }

    private void showEmptyState() {
        Label noData = new Label(NO_DATA_TEXT);
        noData.getStyleClass().add(NO_DATA_LABEL_STYLE);
        canvasContainer.getChildren().setAll(noData);
    }

    private Pane createResizableCanvasWrapper(Canvas target, Runnable onResize) {
        Pane wrapper = new Pane(target);
        target.widthProperty().bind(wrapper.widthProperty());
        target.heightProperty().bind(wrapper.heightProperty());
        wrapper.widthProperty().addListener((obs, o, n) -> onResize.run());
        wrapper.heightProperty().addListener((obs, o, n) -> onResize.run());
        return wrapper;
    }

}
