package com.whiteowl.workbench.surface;

import atlantafx.base.theme.Dracula;
import com.whiteowl.workbench.AppIcon;
import com.whiteowl.workbench.FontLoader;
import com.whiteowl.workbench.TitleBar;
import com.whiteowl.workbench.WindowResizeHandler;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

/**
 * Standalone CSV surface analyzer. Runs on its own undecorated stage
 * (separate from the workbench) while reusing the workbench title bar, resize
 * handling and stylesheet set so it looks and behaves identically.
 *
 * <p>Run with: {@code java com.whiteowl.workbench.surface.SurfaceAnalyzerApp}</p>
 */
public final class SurfaceAnalyzerApp extends Application {

    private static final String TITLE = "Surface Analyzer";
    private static final String ROOT_CSS = "/css/root.css";
    private static final String WORKBENCH_CSS = "/css/workbench.css";
    private static final String SURFACE_CSS = "/css/surface-analyzer.css";
    private static final String WINDOW_BORDER_STYLE = "window-border";
    private static final String TITLE_LABEL_STYLE = "title-bar-label";
    private static final double SCREEN_USE_FACTOR = 0.82;
    private static final int MIN_WIDTH = 700;
    private static final int MIN_HEIGHT = 500;

    @Override
    public void start(Stage stage) {
        FontLoader.loadAll();
        stage.initStyle(StageStyle.UNDECORATED);
        AppIcon.apply(stage);
        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        BorderPane root = new BorderPane();
        root.getStyleClass().add(WINDOW_BORDER_STYLE);
        Label titleLabel = new Label(TITLE);
        titleLabel.getStyleClass().add(TITLE_LABEL_STYLE);
        root.setTop(new TitleBar(stage, titleLabel));
        root.setCenter(new SurfaceAnalyzerPane());
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = bounds.getWidth() * SCREEN_USE_FACTOR;
        double height = bounds.getHeight() * SCREEN_USE_FACTOR;
        Scene scene = new Scene(root, width, height);
        applyStylesheets(scene);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setScene(scene);
        new WindowResizeHandler(stage);
        stage.show();
    }

    private void applyStylesheets(Scene scene) {
        scene.getStylesheets().addAll(
                resolveStylesheet(ROOT_CSS),
                resolveStylesheet(WORKBENCH_CSS),
                resolveStylesheet(SURFACE_CSS)
        );
    }

    private String resolveStylesheet(String path) {
        URL url = getClass().getResource(path);
        return url != null ? url.toExternalForm() : "";
    }

    public static void main(String[] args) {
        launch(args);
    }

}
