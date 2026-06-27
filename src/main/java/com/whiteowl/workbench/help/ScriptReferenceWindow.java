package com.whiteowl.workbench.help;

import com.whiteowl.workbench.AppIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public final class ScriptReferenceWindow {

    private static final String RESOURCE_PATH = "/help/script-reference.html";
    private static final String TITLE = "Script Reference";
    private static final String ROOT_CSS = "/css/root.css";
    private static final String WORKBENCH_CSS = "/css/workbench.css";
    private static final String WINDOW_BORDER_STYLE = "window-border";
    private static final String TITLE_BAR_STYLE = "title-bar";
    private static final String TITLE_LABEL_STYLE = "title-bar-label";
    private static final String CLOSE_BUTTON_STYLE = "window-button-close";
    private static final String WINDOW_ICON_STYLE = "window-icon";
    private static final String ICON_CLOSE_STYLE = "icon-close";
    private static final int WIDTH = 820;
    private static final int HEIGHT = 700;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 300;
    private static final int RESIZE_MARGIN = 6;

    private Stage stage;
    private double dragOffsetX;
    private double dragOffsetY;

    public void show() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return;
        }
        stage = new Stage(StageStyle.TRANSPARENT);
        AppIcon.apply(stage);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        BorderPane root = new BorderPane();
        root.getStyleClass().add(WINDOW_BORDER_STYLE);
        root.setTop(buildTitleBar());
        WebView webView = new WebView();
        webView.getEngine().load(resolveUrl());
        root.setCenter(webView);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        applyStylesheets(scene);
        enableEdgeResize(scene, root);
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add(TITLE_BAR_STYLE);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 0, 0, 12));
        Label titleLabel = new Label(TITLE);
        titleLabel.getStyleClass().add(TITLE_LABEL_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Region closeIcon = new Region();
        closeIcon.getStyleClass().addAll(WINDOW_ICON_STYLE, ICON_CLOSE_STYLE);
        Button closeBtn = new Button();
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().add(CLOSE_BUTTON_STYLE);
        closeBtn.setFocusTraversable(false);
        closeBtn.setOnAction(e -> stage.close());
        titleBar.getChildren().addAll(titleLabel, spacer, closeBtn);
        titleBar.setOnMousePressed(this::handleMousePressed);
        titleBar.setOnMouseDragged(this::handleMouseDragged);
        return titleBar;
    }

    private void handleMousePressed(MouseEvent e) {
        if (MouseButton.PRIMARY != e.getButton()) return;
        dragOffsetX = e.getScreenX() - stage.getX();
        dragOffsetY = e.getScreenY() - stage.getY();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (MouseButton.PRIMARY != e.getButton()) return;
        stage.setX(e.getScreenX() - dragOffsetX);
        stage.setY(e.getScreenY() - dragOffsetY);
    }

    private void enableEdgeResize(Scene scene, Region root) {
        root.setOnMouseMoved(e -> {
            double x = e.getX(), y = e.getY();
            double w = root.getWidth(), h = root.getHeight();
            boolean right = x > w - RESIZE_MARGIN;
            boolean bottom = y > h - RESIZE_MARGIN;
            boolean left = x < RESIZE_MARGIN;
            boolean top = y < RESIZE_MARGIN;
            if (right && bottom) scene.setCursor(Cursor.SE_RESIZE);
            else if (left && bottom) scene.setCursor(Cursor.SW_RESIZE);
            else if (right && top) scene.setCursor(Cursor.NE_RESIZE);
            else if (left && top) scene.setCursor(Cursor.NW_RESIZE);
            else if (right) scene.setCursor(Cursor.E_RESIZE);
            else if (bottom) scene.setCursor(Cursor.S_RESIZE);
            else if (left) scene.setCursor(Cursor.W_RESIZE);
            else if (top) scene.setCursor(Cursor.N_RESIZE);
            else scene.setCursor(Cursor.DEFAULT);
        });
        root.setOnMouseDragged(e -> {
            Cursor cursor = scene.getCursor();
            if (cursor == Cursor.DEFAULT) return;
            double sx = e.getScreenX(), sy = e.getScreenY();
            if (cursor == Cursor.E_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.NE_RESIZE) {
                double newW = sx - stage.getX();
                if (newW >= MIN_WIDTH) stage.setWidth(newW);
            }
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
                double newH = sy - stage.getY();
                if (newH >= MIN_HEIGHT) stage.setHeight(newH);
            }
            if (cursor == Cursor.W_RESIZE || cursor == Cursor.SW_RESIZE || cursor == Cursor.NW_RESIZE) {
                double newW = stage.getX() + stage.getWidth() - sx;
                if (newW >= MIN_WIDTH) { stage.setX(sx); stage.setWidth(newW); }
            }
            if (cursor == Cursor.N_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.NW_RESIZE) {
                double newH = stage.getY() + stage.getHeight() - sy;
                if (newH >= MIN_HEIGHT) { stage.setY(sy); stage.setHeight(newH); }
            }
        });
    }

    private void applyStylesheets(Scene scene) {
        scene.getStylesheets().addAll(
                resolveStylesheet(ROOT_CSS),
                resolveStylesheet(WORKBENCH_CSS)
        );
    }

    private String resolveStylesheet(String path) {
        URL url = getClass().getResource(path);
        return url != null ? url.toExternalForm() : "";
    }

    private String resolveUrl() {
        URL url = getClass().getResource(RESOURCE_PATH);
        return url != null ? url.toExternalForm() : "";
    }

}
