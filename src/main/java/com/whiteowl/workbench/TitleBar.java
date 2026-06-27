package com.whiteowl.workbench;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class TitleBar extends HBox {

    private static final String TITLE_BAR_STYLE = "title-bar";
    private static final String WINDOW_BUTTON_STYLE = "window-button";
    private static final String CLOSE_BUTTON_STYLE = "window-button-close";
    private static final String WINDOW_ICON_STYLE = "window-icon";
    private static final String ICON_MINIMIZE_STYLE = "icon-minimize";
    private static final String ICON_MAXIMIZE_STYLE = "icon-maximize";
    private static final String ICON_RESTORE_STYLE = "icon-restore";
    private static final String ICON_CLOSE_STYLE = "icon-close";
    private static final String ACCESSIBLE_MINIMIZE = "Minimize";
    private static final String ACCESSIBLE_MAXIMIZE = "Maximize";
    private static final String ACCESSIBLE_RESTORE = "Restore";
    private static final String ACCESSIBLE_CLOSE = "Close";

    private final Stage stage;
    private final Button maximizeButton;
    private Region maximizeIcon;
    private double dragOffsetX;
    private double dragOffsetY;
    private double restoreX;
    private double restoreY;
    private double restoreWidth;
    private double restoreHeight;
    private boolean maximized;

    public TitleBar(Stage stage, Node leftContent) {
        this.stage = stage;
        this.maximizeIcon = createIcon(ICON_MAXIMIZE_STYLE);
        this.maximizeButton = new Button();
        maximizeButton.setGraphic(maximizeIcon);
        getStyleClass().add(TITLE_BAR_STYLE);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox windowButtons = buildWindowButtons();
        getChildren().addAll(leftContent, spacer, windowButtons);
        enableDrag();
    }

    private HBox buildWindowButtons() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        Button minimizeButton = createWindowButton(ICON_MINIMIZE_STYLE, WINDOW_BUTTON_STYLE, ACCESSIBLE_MINIMIZE);
        minimizeButton.setOnAction(e -> stage.setIconified(true));
        maximizeButton.getStyleClass().add(WINDOW_BUTTON_STYLE);
        maximizeButton.setAccessibleText(ACCESSIBLE_MAXIMIZE);
        maximizeButton.setFocusTraversable(false);
        maximizeButton.setOnAction(e -> toggleMaximize());
        Button closeButton = createWindowButton(ICON_CLOSE_STYLE, CLOSE_BUTTON_STYLE, ACCESSIBLE_CLOSE);
        closeButton.setOnAction(e -> stage.close());
        box.getChildren().addAll(minimizeButton, maximizeButton, closeButton);
        return box;
    }

    private Button createWindowButton(String iconStyleClass, String buttonStyleClass, String accessibleName) {
        Region icon = createIcon(iconStyleClass);
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add(buttonStyleClass);
        button.setAccessibleText(accessibleName);
        button.setFocusTraversable(false);
        return button;
    }

    private Region createIcon(String iconStyleClass) {
        Region icon = new Region();
        icon.getStyleClass().addAll(WINDOW_ICON_STYLE, iconStyleClass);
        return icon;
    }

    private void enableDrag() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseClicked(this::handleDoubleClick);
    }

    private void handleMousePressed(MouseEvent event) {
        if (MouseButton.PRIMARY != event.getButton()) return;
        dragOffsetX = event.getScreenX() - stage.getX();
        dragOffsetY = event.getScreenY() - stage.getY();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (MouseButton.PRIMARY != event.getButton()) return;
        if (maximized) {
            restoreFromDrag(event);
        }
        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
    }

    private void handleDoubleClick(MouseEvent event) {
        if (MouseButton.PRIMARY == event.getButton() && event.getClickCount() == 2) {
            toggleMaximize();
        }
    }

    public void maximize() {
        if (!maximized) {
            applyMaximize();
        }
    }

    private void toggleMaximize() {
        if (maximized) {
            restore();
        } else {
            applyMaximize();
        }
    }

    private void applyMaximize() {
        restoreX = stage.getX();
        restoreY = stage.getY();
        restoreWidth = stage.getWidth();
        restoreHeight = stage.getHeight();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        maximized = true;
        maximizeIcon.getStyleClass().remove(ICON_MAXIMIZE_STYLE);
        maximizeIcon.getStyleClass().add(ICON_RESTORE_STYLE);
        maximizeButton.setAccessibleText(ACCESSIBLE_RESTORE);
    }

    private void restore() {
        stage.setX(restoreX);
        stage.setY(restoreY);
        stage.setWidth(restoreWidth);
        stage.setHeight(restoreHeight);
        maximized = false;
        maximizeIcon.getStyleClass().remove(ICON_RESTORE_STYLE);
        maximizeIcon.getStyleClass().add(ICON_MAXIMIZE_STYLE);
        maximizeButton.setAccessibleText(ACCESSIBLE_MAXIMIZE);
    }

    private void restoreFromDrag(MouseEvent event) {
        double relativeX = dragOffsetX / stage.getWidth();
        maximized = false;
        maximizeIcon.getStyleClass().remove(ICON_RESTORE_STYLE);
        maximizeIcon.getStyleClass().add(ICON_MAXIMIZE_STYLE);
        stage.setWidth(restoreWidth);
        stage.setHeight(restoreHeight);
        dragOffsetX = restoreWidth * relativeX;
        dragOffsetY = event.getScreenY() - stage.getY();
    }

}
