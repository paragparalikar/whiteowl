package com.whiteowl.workbench;

import atlantafx.base.theme.Dracula;
import com.whiteowl.Context;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WhiteOwlApp extends Application {

    private static final double SCREEN_USE_FACTOR = 0.85;
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private static final String ROOT_CSS = "/css/root.css";
    private static final String WORKBENCH_CSS = "/css/workbench.css";
    private static final String EXPLORER_CSS = "/css/explorer.css";
    private static final String CHARTING_CSS = "/css/charting.css";
    private static final String WATCHLIST_CSS = "/css/watchlist.css";
    private static final String GROUP_CSS = "/css/group.css";
    private static final String DOWNLOADER_CSS = "/css/downloader.css";
    private static final String EXAMPLE_GROUP_CSS = "/css/example-group.css";
    private static final String SCREENER_CSS = "/css/screener.css";
    private static final String RANKER_CSS = "/css/ranker.css";
    private static final String BACKTEST_CSS = "/css/backtest.css";
    private static final String STRATEGY_EDITOR_CSS = "/css/strategy-editor.css";
    private static final String ACCOUNTS_CSS = "/css/accounts.css";
    private static final String COLLECTION_PICKER_CSS = "/css/collection-picker.css";
    private static final String CONFIRM_DIALOG_CSS = "/css/confirm-dialog.css";
    private static final String SCRIPTS_PANE_CSS = "/css/scripts-pane.css";
    private static final String SCRIPT_EDITOR_CSS = "/css/script-editor.css";
    private static final String STATUS_BAR_CSS = "/css/status-bar.css";
    private static final String DIALOG_CSS = "/css/dialog.css";
    private static final String NOTE_EDITOR_CSS = "/css/note-editor.css";
    private static final String NOTE_LIST_CSS = "/css/note-list.css";
    private static final String LOG_VIEWER_CSS = "/css/log-viewer.css";
    private static final String ORB_OPTIMIZER_CSS = "/css/orb-optimizer.css";
    private static final String ROT_ORB_BACKTEST_CSS = "/css/rot-orb-backtest.css";
    private static final String FONT_LOADER_THREAD = "font-loader";
    private static final String CONTEXT_INIT_THREAD = "whiteowl-init";
    private static final String INIT_LOGGING = "Logging configured";
    private static final String INIT_SPLASH = "Splash shown";
    private static final String INIT_FONTS = "Fonts loaded";
    private static final String INIT_CONTEXT = "Context created";
    private static final String INIT_THEME = "Theme applied";
    private static final String INIT_WORKBENCH = "Workbench built";
    private static final String INIT_STYLESHEETS = "Stylesheets applied";
    private static final String INIT_STAGE = "Stage configured";
    private Context context;
    private volatile Thread fontThread;

    @Override
    public void start(Stage primaryStage) {
        StartupTimer timer = new StartupTimer();
        configureLogging();
        timer.checkpoint(INIT_LOGGING);
        AppIcon.apply(primaryStage);
        SplashStage splash = new SplashStage();
        splash.show();
        timer.checkpoint(INIT_SPLASH);
        fontThread = new Thread(() -> {
            FontLoader.loadAll();
            timer.checkpoint(INIT_FONTS);
        });
        fontThread.setDaemon(true);
        fontThread.setName(FONT_LOADER_THREAD);
        fontThread.start();
        Task<Context> initTask = createInitTask();
        initTask.setOnSucceeded(event -> {
            context = initTask.getValue();
            timer.checkpoint(INIT_CONTEXT);
            showMainStage(primaryStage, splash, timer);
        });
        initTask.setOnFailed(event -> {
            log.error("Startup failed", initTask.getException());
            splash.close();
            Platform.exit();
        });
        Thread initThread = new Thread(initTask);
        initThread.setDaemon(true);
        initThread.setName(CONTEXT_INIT_THREAD);
        initThread.start();
    }

    private void showMainStage(Stage primaryStage, SplashStage splash, StartupTimer timer) {
        awaitFontLoading();
        primaryStage.initStyle(StageStyle.UNDECORATED);
        AppIcon.apply(primaryStage);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * SCREEN_USE_FACTOR;
        double height = screenBounds.getHeight() * SCREEN_USE_FACTOR;
        WorkbenchView workbench = new WorkbenchView(primaryStage, context);
        timer.checkpoint(INIT_WORKBENCH);
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        timer.checkpoint(INIT_THEME);
        Scene scene = new Scene(workbench.getRoot(), width, height);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, workbench.getKeyNavigationHandler());
        applyStylesheets(scene);
        timer.checkpoint(INIT_STYLESHEETS);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - width) / 2);
        primaryStage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - height) / 2);
        primaryStage.setScene(scene);
        new WindowResizeHandler(primaryStage);
        timer.checkpoint(INIT_STAGE);
        splash.close();
        primaryStage.show();
        workbench.getTitleBar().maximize();
        log.info("WhiteOwl Workbench launched");
    }

    private void awaitFontLoading() {
        if (fontThread != null && fontThread.isAlive()) {
            try {
                fontThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for font loading");
            }
        }
    }

    private Task<Context> createInitTask() {
        return new Task<>() {
            @Override
            protected Context call() {
                return createContext();
            }
        };
    }

    private void applyStylesheets(Scene scene) {
        scene.getStylesheets().addAll(
                resolveStylesheet(ROOT_CSS),
                resolveStylesheet(WORKBENCH_CSS),
                resolveStylesheet(EXPLORER_CSS),
                resolveStylesheet(CHARTING_CSS),
                resolveStylesheet(WATCHLIST_CSS),
                resolveStylesheet(GROUP_CSS),
                resolveStylesheet(DOWNLOADER_CSS),
                resolveStylesheet(EXAMPLE_GROUP_CSS),
                resolveStylesheet(SCREENER_CSS),
                resolveStylesheet(RANKER_CSS),
                resolveStylesheet(BACKTEST_CSS),
                resolveStylesheet(STRATEGY_EDITOR_CSS),
                resolveStylesheet(ACCOUNTS_CSS),
                resolveStylesheet(COLLECTION_PICKER_CSS),
                resolveStylesheet(CONFIRM_DIALOG_CSS),
                resolveStylesheet(SCRIPTS_PANE_CSS),
                resolveStylesheet(SCRIPT_EDITOR_CSS),
                resolveStylesheet(STATUS_BAR_CSS),
                resolveStylesheet(DIALOG_CSS),
                resolveStylesheet(NOTE_EDITOR_CSS),
                resolveStylesheet(NOTE_LIST_CSS),
                resolveStylesheet(LOG_VIEWER_CSS),
                resolveStylesheet(ORB_OPTIMIZER_CSS),
                resolveStylesheet(ROT_ORB_BACKTEST_CSS)
        );
    }

    private String resolveStylesheet(String path) {
        return getClass().getResource(path).toExternalForm();
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    private Context createContext() {
        return new Context();
    }

    private void configureLogging() {
        log.debug(INIT_LOGGING);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
