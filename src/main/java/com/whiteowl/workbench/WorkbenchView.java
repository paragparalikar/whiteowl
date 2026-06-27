package com.whiteowl.workbench;

import com.whiteowl.Context;
import com.whiteowl.core.collection.CollectionResolver;
import com.whiteowl.client.kite.adapter.KiteBrokerAdapter;
import com.whiteowl.core.account.service.ActiveAccountManager;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.core.indicator.script.IndicatorScriptConstants;
import com.whiteowl.core.ranker.script.RankerScriptConstants;
import com.whiteowl.core.screener.script.ScreenerScriptConstants;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.script.ScriptType;
import com.whiteowl.workbench.backtest.BacktestPane;
import com.whiteowl.workbench.help.ScriptReferenceWindow;
import com.whiteowl.workbench.backtest.BacktestResultPane;
import com.whiteowl.workbench.charting.ChartPane;
import com.whiteowl.workbench.charting.ChartOrderService;
import com.whiteowl.workbench.charting.OrderBookPanel;
import com.whiteowl.workbench.script.ScriptEditorPane;
import com.whiteowl.workbench.script.ScriptsPane;
import com.whiteowl.workbench.downloader.DownloaderPane;
import com.whiteowl.workbench.examplegroup.ExampleGroupPane;
import com.whiteowl.workbench.common.ScripNavigable;
import com.whiteowl.workbench.explorer.ExplorerPane;
import com.whiteowl.workbench.group.GroupPane;
import com.whiteowl.workbench.ranker.RankerPane;
import com.whiteowl.workbench.screener.ScreenerPane;
import com.whiteowl.workbench.watchlist.WatchlistPane;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public final class WorkbenchView {

    private static final String WINDOW_BORDER_STYLE = "window-border";
    private static final String LEFT_TAB_PANE_STYLE = "left-tab-pane";
    private static final String RIGHT_TAB_PANE_STYLE = "right-tab-pane";
    private static final String MENU_BAR_STYLE = "workbench-menu-bar";
    private static final String TAB_EXPLORER = "Explorer";
    private static final String TAB_WATCHLISTS = "Watchlists";
    private static final String TAB_GROUPS = "Groups";
    private static final String TAB_EXAMPLE_GROUPS = "Example Groups";
    private static final String TAB_DOWNLOADER = "Downloader";
    private static final String TAB_SCREENER = "Screener";
    private static final String TAB_RANKER = "Ranker";
    private static final String TAB_BACKTEST = "Backtest";
    private static final String TAB_CHARTING = "Charting";
    private static final String TAB_SCRIPTS = "Scripts";
    private static final String MENU_FILE = "File";
    private static final String MENU_COLLECTIONS = "Collections";
    private static final String MENU_MODULES = "Modules";
    private static final String MENU_HELP = "Help";
    private static final String MENU_SCRIPT_REFERENCE = "Script Reference";
    private static final String MENU_EXIT = "Exit";
    private static final String VIEW_ICON_STYLE = "view-icon";
    private static final String TAB_ICON_STYLE = "tab-icon";
    private static final int MENU_ICON_SIZE = 14;
    private static final int TAB_ICON_SIZE = 14;
    private static final double DIVIDER_POSITION = 0.20;
    private static final double ORDER_BOOK_DIVIDER = 0.65;

    private final BorderPane root;
    private final TabPane leftTabPane;
    private final TabPane rightTabPane;
    private final SplitPane mainSplitPane;
    private final Context context;
    private final ExplorerPane explorerPane;
    private TitleBar titleBar;
    private ChartPane chartPane;
    private OrderBookPanel orderBookPanel;
    private ChartOrderService chartOrderService;
    private boolean orderBookVisible;
    private WatchlistPane watchlistPane;
    private GroupPane groupPane;
    private ExampleGroupPane exampleGroupPane;
    private DownloaderPane downloaderPane;
    private ScreenerPane screenerPane;
    private RankerPane rankerPane;
    private BacktestPane backtestPane;
    private ScriptsPane scriptsPane;
    private final ScriptRepository indicatorScriptRepo;
    private final ScriptRepository screenerScriptRepo;
    private final ScriptRepository rankerScriptRepo;
    private final ScriptReferenceWindow scriptReferenceWindow = new ScriptReferenceWindow();

    public WorkbenchView(Stage stage, Context context) {
        this.context = context;
        this.indicatorScriptRepo = new ScriptRepository(ScriptType.INDICATOR,
                IndicatorScriptConstants.INDICATORS_DIR, IndicatorScriptConstants.DEFAULT_TEMPLATE);
        this.screenerScriptRepo = new ScriptRepository(ScriptType.SCREENER,
                ScreenerScriptConstants.SCREENERS_DIR, ScreenerScriptConstants.DEFAULT_TEMPLATE);
        this.rankerScriptRepo = new ScriptRepository(ScriptType.RANKER,
                RankerScriptConstants.RANKERS_DIR, RankerScriptConstants.DEFAULT_TEMPLATE);
        this.leftTabPane = new TabPane();
        this.rightTabPane = new TabPane();
        this.mainSplitPane = new SplitPane();
        this.mainSplitPane.setOrientation(Orientation.VERTICAL);
        this.root = new BorderPane();
        this.root.getStyleClass().add(WINDOW_BORDER_STYLE);
        this.explorerPane = new ExplorerPane(context.getScripRepository());
        this.chartPane = new ChartPane(context.getBarsRepository(), context.getScripRepository(),
                context.getExampleGroupRepository(), indicatorScriptRepo, screenerScriptRepo);
        explorerPane.setOnScripSelected(scrip -> { ensureChartingTab(); chartPane.showChart(scrip); });
        wireActiveAccountListener();
        buildLayout(stage);
        preloadCollections();
    }

    public BorderPane getRoot() {
        return root;
    }

    public TitleBar getTitleBar() {
        return titleBar;
    }

    public EventHandler<KeyEvent> getKeyNavigationHandler() {
        return chartPane.getKeyNavigationHandler();
    }

    private void buildLayout(Stage stage) {
        MenuBar menuBar = buildMenuBar(stage);
        menuBar.getStyleClass().add(MENU_BAR_STYLE);
        titleBar = new TitleBar(stage, menuBar);
        root.setTop(titleBar);
        leftTabPane.getStyleClass().add(LEFT_TAB_PANE_STYLE);
        rightTabPane.getStyleClass().add(RIGHT_TAB_PANE_STYLE);
        leftTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> updateScripNavigable(newTab));
        addLeftTab(TAB_EXPLORER, FluentUiRegularAL.FOLDER_OPEN_16, explorerPane);
        SplitPane topSplitPane = new SplitPane(leftTabPane, rightTabPane);
        topSplitPane.setOrientation(Orientation.HORIZONTAL);
        topSplitPane.setDividerPositions(DIVIDER_POSITION);
        SplitPane.setResizableWithParent(leftTabPane, false);
        mainSplitPane.getItems().add(topSplitPane);
        root.setCenter(mainSplitPane);
        StatusBar statusBar = new StatusBar(context.getActiveAccountManager(), context.getAccountService());
        statusBar.setOnOrderBookToggle(this::toggleOrderBook);
        root.setBottom(statusBar);
        chartPane.setOnOrderBookRefresh(this::refreshOrderBook);
    }

    private WatchlistPane ensureWatchlistPane() {
        if (watchlistPane == null) {
            watchlistPane = new WatchlistPane(context.getWatchlistRepository(), context.getScripRepository());
            watchlistPane.setOnScripSelected(scrip -> { ensureChartingTab(); chartPane.showChart(scrip); });
            chartPane.setWatchlistPane(watchlistPane);
            explorerPane.setWatchlistPane(watchlistPane);
            if (groupPane != null) {
                watchlistPane.setGroupPane(groupPane);
                groupPane.setWatchlistPane(watchlistPane);
            }
        }
        return watchlistPane;
    }

    private GroupPane ensureGroupPane() {
        if (groupPane == null) {
            groupPane = new GroupPane(context.getGroupRepository(), context.getScripRepository());
            groupPane.setOnScripSelected(scrip -> { ensureChartingTab(); chartPane.showChart(scrip); });
            chartPane.setGroupPane(groupPane);
            explorerPane.setGroupPane(groupPane);
            if (watchlistPane != null) {
                watchlistPane.setGroupPane(groupPane);
                groupPane.setWatchlistPane(watchlistPane);
            }
        }
        return groupPane;
    }

    private ExampleGroupPane ensureExampleGroupPane() {
        if (exampleGroupPane == null) {
            exampleGroupPane = new ExampleGroupPane(context.getExampleGroupRepository(),
                    context.getScripRepository());
            exampleGroupPane.setOnExampleSelected(example -> {
                context.getScripRepository().findById(example.getScripId()).ifPresent(scrip -> {
                    ensureChartingTab();
                    chartPane.showExample(scrip, example.getTimeframe(), example.getTimestamp());
                });
            });
            chartPane.setExampleGroupPane(exampleGroupPane);
        }
        return exampleGroupPane;
    }

    private DownloaderPane ensureDownloaderPane() {
        if (downloaderPane == null) {
            downloaderPane = new DownloaderPane(context.getScripRepository(),
                    context.getCompositeBarDataDownloader(), buildCollectionResolver());
            downloaderPane.setExplorerPane(explorerPane);
        }
        return downloaderPane;
    }

    private ScreenerPane ensureScreenerPane() {
        if (screenerPane == null) {
            screenerPane = new ScreenerPane(context.getScripRepository(), context.getBarsRepository(),
                    context.getExampleGroupRepository(), buildCollectionResolver(), screenerScriptRepo);
            screenerPane.setOnScripSelected(scrip -> { ensureChartingTab(); chartPane.showChart(scrip); });
        }
        return screenerPane;
    }

    private RankerPane ensureRankerPane() {
        if (rankerPane == null) {
            rankerPane = new RankerPane(context.getScripRepository(), context.getBarsRepository(),
                    buildCollectionResolver());
            rankerPane.setOnScripSelected(scrip -> { ensureChartingTab(); chartPane.showChart(scrip); });
        }
        return rankerPane;
    }

    private BacktestPane ensureBacktestPane() {
        if (backtestPane == null) {
            backtestPane = new BacktestPane(context.getScripRepository(), context.getBarsRepository());
            backtestPane.setOnBacktestStarted(this::openBacktestResultTab);
        }
        return backtestPane;
    }


    private void openBacktestResultTab(ScriptDescriptor strategy, BacktestResultPane resultPane) {
        Tab tab = new Tab(strategy.getName(), resultPane);
        tab.setGraphic(createTabIcon(FluentUiRegularAL.DATA_BAR_VERTICAL_20));
        rightTabPane.getTabs().add(tab);
        rightTabPane.getSelectionModel().select(tab);
    }

    private MenuBar buildMenuBar(Stage stage) {
        MenuItem exitItem = new MenuItem(MENU_EXIT);
        exitItem.setOnAction(e -> { stage.close(); Platform.exit(); });
        Menu fileMenu = new Menu(MENU_FILE, null, exitItem);
        MenuItem viewExplorer = createViewMenuItem(TAB_EXPLORER, FluentUiRegularAL.FOLDER_OPEN_16,
                () -> addLeftTab(TAB_EXPLORER, FluentUiRegularAL.FOLDER_OPEN_16, explorerPane));
        MenuItem viewWatchlists = createViewMenuItem(TAB_WATCHLISTS, FluentUiRegularMZ.STAR_16,
                () -> addLeftTab(TAB_WATCHLISTS, FluentUiRegularMZ.STAR_16, ensureWatchlistPane()));
        MenuItem viewGroups = createViewMenuItem(TAB_GROUPS, FluentUiRegularMZ.PEOPLE_COMMUNITY_16,
                () -> addLeftTab(TAB_GROUPS, FluentUiRegularMZ.PEOPLE_COMMUNITY_16, ensureGroupPane()));
        MenuItem viewExampleGroups = createViewMenuItem(TAB_EXAMPLE_GROUPS, FluentUiRegularAL.BEAKER_16,
                () -> addLeftTab(TAB_EXAMPLE_GROUPS, FluentUiRegularAL.BEAKER_16, ensureExampleGroupPane()));
        Menu collectionsMenu = new Menu(MENU_COLLECTIONS, null,
                viewExplorer, viewWatchlists, viewGroups, viewExampleGroups);
        MenuItem viewCharting = createViewMenuItem(TAB_CHARTING, FluentUiRegularAL.DATA_LINE_24,
                this::ensureChartingTab);
        MenuItem viewScreener = createViewMenuItem(TAB_SCREENER, FluentUiRegularAL.FILTER_20,
                () -> addLeftTab(TAB_SCREENER, FluentUiRegularAL.FILTER_20, ensureScreenerPane()));
        MenuItem viewRanker = createViewMenuItem(TAB_RANKER, FluentUiRegularAL.ARROW_SORT_20,
                () -> addLeftTab(TAB_RANKER, FluentUiRegularAL.ARROW_SORT_20, ensureRankerPane()));
        MenuItem viewDownloader = createViewMenuItem(TAB_DOWNLOADER, FluentUiRegularAL.ARROW_DOWNLOAD_16,
                () -> addLeftTab(TAB_DOWNLOADER, FluentUiRegularAL.ARROW_DOWNLOAD_16, ensureDownloaderPane()));
        MenuItem viewBacktest = createViewMenuItem(TAB_BACKTEST, FluentUiRegularAL.DATA_BAR_VERTICAL_20,
                () -> addLeftTab(TAB_BACKTEST, FluentUiRegularAL.DATA_BAR_VERTICAL_20, ensureBacktestPane()));
        MenuItem viewScripts = createViewMenuItem(TAB_SCRIPTS, FluentUiRegularAL.CODE_20,
                () -> addLeftTab(TAB_SCRIPTS, FluentUiRegularAL.CODE_20, ensureScriptsPane()));
        Menu modulesMenu = new Menu(MENU_MODULES, null,
                viewCharting, viewScreener, viewRanker, viewDownloader, viewBacktest, viewScripts);
        MenuItem scriptRefItem = new MenuItem(MENU_SCRIPT_REFERENCE);
        scriptRefItem.setOnAction(e -> scriptReferenceWindow.show());
        Menu helpMenu = new Menu(MENU_HELP, null, scriptRefItem);
        return new MenuBar(fileMenu, collectionsMenu, modulesMenu, helpMenu);
    }

    private MenuItem createViewMenuItem(String label, Ikon ikon, Runnable action) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(MENU_ICON_SIZE);
        icon.getStyleClass().add(VIEW_ICON_STYLE);
        MenuItem item = new MenuItem(label, icon);
        item.setOnAction(e -> action.run());
        return item;
    }

    private void addLeftTab(String title, Ikon ikon, Node content) {
        for (Tab tab : leftTabPane.getTabs()) {
            if (title.equals(tab.getText())) {
                leftTabPane.getSelectionModel().select(tab);
                return;
            }
        }
        Tab tab = new Tab(title, content);
        tab.setGraphic(createTabIcon(ikon));
        leftTabPane.getTabs().add(tab);
        leftTabPane.getSelectionModel().select(tab);
    }

    private void ensureChartingTab() {
        for (Tab tab : rightTabPane.getTabs()) {
            if (TAB_CHARTING.equals(tab.getText())) {
                rightTabPane.getSelectionModel().select(tab);
                return;
            }
        }
        Tab tab = new Tab(TAB_CHARTING, chartPane);
        tab.setGraphic(createTabIcon(FluentUiRegularAL.DATA_LINE_24));
        rightTabPane.getTabs().add(tab);
        rightTabPane.getSelectionModel().select(tab);
    }

    private FontIcon createTabIcon(Ikon ikon) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TAB_ICON_SIZE);
        icon.getStyleClass().add(TAB_ICON_STYLE);
        return icon;
    }

    private CollectionResolver buildCollectionResolver() {
        return new CollectionResolver(
                context.getWatchlistRepository(),
                context.getGroupRepository(),
                context.getExampleGroupRepository());
    }

    private void updateScripNavigable(Tab tab) {
        if (tab == null || tab.getContent() == null) {
            chartPane.setScripNavigable(null);
            return;
        }
        Node content = tab.getContent();
        if (content instanceof ScripNavigable navigable) {
            chartPane.setScripNavigable(navigable);
        } else {
            chartPane.setScripNavigable(null);
        }
    }

    private ScriptsPane ensureScriptsPane() {
        if (scriptsPane == null) {
            scriptsPane = new ScriptsPane(indicatorScriptRepo, screenerScriptRepo, rankerScriptRepo);
            scriptsPane.setOnEditScript(this::openScriptEditor);
        }
        return scriptsPane;
    }

    private void wireActiveAccountListener() {
        ActiveAccountManager manager = context.getActiveAccountManager();
        manager.addListener(adapterOpt -> Platform.runLater(() ->
                adapterOpt.ifPresentOrElse(this::wireChartOrderService, this::clearChartOrderService)
        ));
    }

    private void clearChartOrderService() {
        chartOrderService = null;
        chartPane.setChartOrderService(null, 0);
        if (orderBookPanel != null) orderBookPanel.setChartOrderService(null);
    }

    private void wireChartOrderService(KiteBrokerAdapter brokerAdapter) {
        chartOrderService = new ChartOrderService() {
            @Override
            public java.util.List<com.whiteowl.core.gtt.model.GttOrder> fetchGtts() {
                return brokerAdapter.fetchGtts();
            }

            @Override
            public com.whiteowl.core.gtt.model.GttOrder createGtt(com.whiteowl.core.gtt.model.GttOrder gtt) {
                return brokerAdapter.createGtt(gtt);
            }

            @Override
            public com.whiteowl.core.gtt.model.GttOrder updateGtt(com.whiteowl.core.gtt.model.GttOrder gtt) {
                return brokerAdapter.updateGtt(gtt);
            }

            @Override
            public void cancelGtt(int gttId) {
                brokerAdapter.cancelGtt(gttId);
            }

            @Override
            public com.whiteowl.core.order.model.Order createOrder(com.whiteowl.core.order.model.Order order) {
                return brokerAdapter.createOrder(order);
            }

            @Override
            public com.whiteowl.core.order.model.Order updateOrder(com.whiteowl.core.order.model.Order order) {
                return brokerAdapter.updateOrder(order);
            }

            @Override
            public com.whiteowl.core.order.model.Order cancelOrder(com.whiteowl.core.order.model.Order order) {
                return brokerAdapter.cancelOrder(order);
            }

            @Override
            public java.util.List<com.whiteowl.core.order.model.Order> fetchOrders() {
                return brokerAdapter.fetchOrders();
            }

            @Override
            public java.util.List<com.whiteowl.core.portfolio.model.Position> fetchPositions() {
                return brokerAdapter.fetchPositions();
            }

            @Override
            public java.util.List<com.whiteowl.core.portfolio.model.Holding> fetchHoldings() {
                return brokerAdapter.fetchHoldings();
            }

            @Override
            public com.whiteowl.core.portfolio.model.Funds fetchFunds() {
                return brokerAdapter.fetchFunds();
            }
        };
        int positionCount = context.getActiveAccountManager().getActiveAccount()
                .map(com.whiteowl.core.account.model.Account::getPreferredPositionCount)
                .orElse(com.whiteowl.core.account.model.Account.DEFAULT_PREFERRED_POSITION_COUNT);
        chartPane.setChartOrderService(chartOrderService, positionCount);
        if (orderBookPanel != null) orderBookPanel.setChartOrderService(chartOrderService);
        chartPane.refreshGttOrders();
    }

    private void toggleOrderBook() {
        if (orderBookVisible) {
            hideOrderBook();
        } else {
            showOrderBook();
        }
    }

    private void showOrderBook() {
        if (orderBookPanel == null) {
            orderBookPanel = new OrderBookPanel();
            orderBookPanel.setChartOrderService(chartOrderService);
            orderBookPanel.setOnCloseRequested(this::hideOrderBook);
            orderBookPanel.setOnNavigateToScrip(scripId ->
                    resolveScrip(scripId).ifPresent(scrip -> {
                        ensureChartingTab();
                        chartPane.showChart(scrip);
                    }));
        }
        if (mainSplitPane.getItems().size() < 2) {
            mainSplitPane.getItems().add(orderBookPanel);
        }
        mainSplitPane.setDividerPositions(ORDER_BOOK_DIVIDER);
        SplitPane.setResizableWithParent(orderBookPanel, false);
        orderBookVisible = true;
        orderBookPanel.refreshAll();
    }

    private void hideOrderBook() {
        mainSplitPane.getItems().remove(orderBookPanel);
        orderBookVisible = false;
    }

    private void refreshOrderBook() {
        if (orderBookPanel != null && orderBookVisible) {
            orderBookPanel.refreshAll();
        }
    }

    private Optional<Scrip> resolveScrip(String scripId) {
        if (scripId == null) return Optional.empty();
        ScripRepository repo = context.getScripRepository();
        Optional<Scrip> result = repo.findById(scripId);
        if (result.isPresent()) return result;
        int colonIndex = scripId.indexOf(':');
        if (colonIndex > 0) {
            String symbol = scripId.substring(colonIndex + 1);
            String exchangeName = scripId.substring(0, colonIndex);
            try {
                result = repo.findBySymbolAndExchange(symbol, Exchange.valueOf(exchangeName));
                if (result.isPresent()) return result;
            } catch (IllegalArgumentException ignored) { }
            for (Exchange exchange : Exchange.values()) {
                result = repo.findBySymbolAndExchange(symbol, exchange);
                if (result.isPresent()) return result;
            }
        }
        for (Exchange exchange : Exchange.values()) {
            result = repo.findBySymbolAndExchange(scripId, exchange);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private void preloadCollections() {
        Thread loader = new Thread(() -> {
            context.getWatchlistRepository().loadAll();
            context.getGroupRepository().loadAll();
            context.getExampleGroupRepository().loadAll();
            Platform.runLater(() -> {
                ensureWatchlistPane();
                ensureGroupPane();
                ensureExampleGroupPane();
            });
        });
        loader.setDaemon(true);
        loader.setName("collection-preloader");
        loader.start();
    }

    public void openScriptEditor(ScriptDescriptor descriptor, ScriptEditorPane editorPane) {
        for (Tab tab : rightTabPane.getTabs()) {
            if (tab.getContent() instanceof ScriptEditorPane existingEditor
                    && existingEditor.getActiveScript() != null
                    && existingEditor.getActiveScript().getId().equals(descriptor.getId())) {
                rightTabPane.getSelectionModel().select(tab);
                return;
            }
        }
        Tab tab = new Tab(descriptor.getName(), editorPane);
        tab.setGraphic(createTabIcon(FluentUiRegularAL.CODE_20));
        editorPane.setOnScriptRenamed((oldScript, newScript) -> tab.setText(newScript.getName()));
        editorPane.setOnScriptSaved(saved -> chartPane.refreshScriptedComponents());
        editorPane.openScript(descriptor);
        rightTabPane.getTabs().add(tab);
        rightTabPane.getSelectionModel().select(tab);
    }


}
