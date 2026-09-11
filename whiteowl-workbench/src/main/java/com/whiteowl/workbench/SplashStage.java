package com.whiteowl.workbench;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static javafx.scene.paint.CycleMethod.NO_CYCLE;

public final class SplashStage {

    private static final String SPLASH_CSS = "/css/splash.css";
    private static final String SPLASH_ROOT_STYLE = "splash-root";
    private static final String SPLASH_TITLE_STYLE = "splash-title";
    private static final String SPLASH_SUBTITLE_STYLE = "splash-subtitle";
    private static final String SPLASH_DIVIDER_STYLE = "splash-divider";
    private static final String APP_TITLE = "WhiteOwl";
    private static final String APP_SUBTITLE = "W O R K B E N C H";
    private static final double SPLASH_WIDTH = 520;
    private static final double SPLASH_HEIGHT = 300;
    private static final double DOT_SPACING = 28;
    private static final double DOT_RADIUS = 0.8;
    private static final int ARC_COUNT = 6;
    private static final double ARC_START_RADIUS = 60;
    private static final double ARC_SPACING = 50;
    private static final double ARC_CENTER_X = 0.85;
    private static final double ARC_CENTER_Y = 0.9;
    private static final double ARC_STROKE_WIDTH = 0.6;
    private static final double ARC_BASE_OPACITY = 0.08;
    private static final double DOT_BASE_OPACITY = 0.15;
    private static final double GLOW_CENTER_X = 0.7;
    private static final double GLOW_CENTER_Y = 0.6;
    private static final int CONTENT_SPACING = 6;
    private static final int CONTENT_BOTTOM_PADDING = 28;
    private static final int CONTENT_LEFT_PADDING = 40;
    private static final int DIVIDER_HEIGHT = 1;
    private static final int DIVIDER_WIDTH = 60;

    private final Stage stage;

    public SplashStage() {
        this.stage = new Stage(StageStyle.UNDECORATED);
        AppIcon.apply(stage);
        buildUi();
    }

    private void buildUi() {
        Canvas pattern = createPattern();
        VBox content = createContent();
        StackPane root = new StackPane(pattern, content);
        root.getStyleClass().add(SPLASH_ROOT_STYLE);
        Scene scene = new Scene(root, SPLASH_WIDTH, SPLASH_HEIGHT);
        scene.setFill(null);
        scene.getStylesheets().add(getClass().getResource(SPLASH_CSS).toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private Canvas createPattern() {
        Canvas canvas = new Canvas(SPLASH_WIDTH, SPLASH_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawDotGrid(gc);
        drawArcs(gc);
        drawGlow(gc);
        return canvas;
    }

    private void drawDotGrid(GraphicsContext gc) {
        double cx = SPLASH_WIDTH * GLOW_CENTER_X;
        double cy = SPLASH_HEIGHT * GLOW_CENTER_Y;
        double maxDist = Math.hypot(SPLASH_WIDTH, SPLASH_HEIGHT);
        for (double x = DOT_SPACING / 2; x < SPLASH_WIDTH; x += DOT_SPACING) {
            for (double y = DOT_SPACING / 2; y < SPLASH_HEIGHT; y += DOT_SPACING) {
                double dist = Math.hypot(x - cx, y - cy);
                double fade = 1.0 - (dist / maxDist);
                double opacity = DOT_BASE_OPACITY * fade * fade;
                if (opacity > 0.005) {
                    gc.setFill(Color.color(1, 1, 1, opacity));
                    gc.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
                }
            }
        }
    }

    private void drawArcs(GraphicsContext gc) {
        double cx = SPLASH_WIDTH * ARC_CENTER_X;
        double cy = SPLASH_HEIGHT * ARC_CENTER_Y;
        gc.setLineWidth(ARC_STROKE_WIDTH);
        for (int i = 0; i < ARC_COUNT; i++) {
            double radius = ARC_START_RADIUS + i * ARC_SPACING;
            double opacity = ARC_BASE_OPACITY * (1.0 - (double) i / ARC_COUNT);
            gc.setStroke(Color.color(0.29, 0.47, 0.76, opacity));
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                    120, 90, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawGlow(GraphicsContext gc) {
        double cx = SPLASH_WIDTH * GLOW_CENTER_X;
        double cy = SPLASH_HEIGHT * GLOW_CENTER_Y;
        double glowRadius = SPLASH_WIDTH * 0.4;
        RadialGradient glow = new RadialGradient(0, 0, cx, cy, glowRadius, false, NO_CYCLE,
                new Stop(0, Color.color(0.29, 0.47, 0.76, 0.15)),
                new Stop(0.4, Color.color(0.29, 0.47, 0.76, 0.06)),
                new Stop(1, Color.TRANSPARENT));
        gc.setFill(glow);
        gc.fillRect(0, 0, SPLASH_WIDTH, SPLASH_HEIGHT);
    }

    private VBox createContent() {
        Label title = new Label(APP_TITLE);
        title.getStyleClass().add(SPLASH_TITLE_STYLE);
        Label subtitle = new Label(APP_SUBTITLE);
        subtitle.getStyleClass().add(SPLASH_SUBTITLE_STYLE);
        Region divider = new Region();
        divider.getStyleClass().add(SPLASH_DIVIDER_STYLE);
        divider.setMaxWidth(DIVIDER_WIDTH);
        divider.setMinHeight(DIVIDER_HEIGHT);
        divider.setMaxHeight(DIVIDER_HEIGHT);
        VBox content = new VBox(CONTENT_SPACING, title, subtitle, divider);
        content.setAlignment(Pos.BOTTOM_LEFT);
        content.setPadding(new Insets(0, 0, CONTENT_BOTTOM_PADDING, CONTENT_LEFT_PADDING));
        return content;
    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }

}
