package com.whiteowl.workbench.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.Getter;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.javafx.FontIcon;

public final class ConfirmationDialog {

    private static final String ROOT_STYLE = "confirm-dialog";
    private static final String ICON_CONTAINER_STYLE = "confirm-dialog-icon-container";
    private static final String ICON_STYLE = "confirm-dialog-icon";
    private static final String TITLE_STYLE = "confirm-dialog-title";
    private static final String MESSAGE_STYLE = "confirm-dialog-message";
    private static final String WARNING_STYLE = "confirm-dialog-warning";
    private static final String CONFIRM_BUTTON_STYLE = "confirm-dialog-confirm";
    private static final String CANCEL_BUTTON_STYLE = "confirm-dialog-cancel";
    private static final String WARNING_TEXT = "This action cannot be undone.";
    private static final String CANCEL_TEXT = "Cancel";
    private static final int DIALOG_WIDTH = 420;
    private static final int CONTENT_SPACING = 10;
    private static final int CONTENT_PADDING = 20;
    private static final int BUTTON_GAP = 10;
    private static final int BUTTON_TOP_PADDING = 6;
    private static final int ICON_SIZE = 28;

    private final String title;
    private final String message;
    private final String confirmText;
    private Stage stage;
    @Getter private boolean confirmed;

    public ConfirmationDialog(String title, String message, String confirmText) {
        this.title = title;
        this.message = message;
        this.confirmText = confirmText;
    }

    public void show(Window owner) {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        VBox root = buildContent();
        root.getStyleClass().add(ROOT_STYLE);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        stage.setWidth(DIALOG_WIDTH);
        centerOnOwner(owner);
        stage.showAndWait();
    }

    private VBox buildContent() {
        VBox root = new VBox(CONTENT_SPACING);
        root.setPadding(new Insets(CONTENT_PADDING));
        root.setAlignment(Pos.CENTER);
        HBox iconContainer = buildIconRow();
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(TITLE_STYLE);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add(MESSAGE_STYLE);
        messageLabel.setWrapText(true);
        Label warningLabel = new Label(WARNING_TEXT);
        warningLabel.getStyleClass().add(WARNING_STYLE);
        HBox buttonBar = buildButtonBar();
        root.getChildren().addAll(iconContainer, titleLabel, messageLabel, warningLabel, buttonBar);
        return root;
    }

    private HBox buildIconRow() {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 0, 4, 0));
        VBox iconCircle = new VBox();
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.getStyleClass().add(ICON_CONTAINER_STYLE);
        FontIcon icon = new FontIcon(FluentUiRegularMZ.WARNING_16);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(ICON_STYLE);
        iconCircle.getChildren().add(icon);
        container.getChildren().add(iconCircle);
        return container;
    }

    private HBox buildButtonBar() {
        HBox bar = new HBox(BUTTON_GAP);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(BUTTON_TOP_PADDING, 0, 0, 0));
        Button cancelBtn = new Button(CANCEL_TEXT);
        cancelBtn.getStyleClass().add(CANCEL_BUTTON_STYLE);
        cancelBtn.setOnAction(e -> stage.close());
        cancelBtn.setCancelButton(true);
        Button confirmBtn = new Button(confirmText);
        confirmBtn.getStyleClass().add(CONFIRM_BUTTON_STYLE);
        confirmBtn.setOnAction(e -> {
            confirmed = true;
            stage.close();
        });
        confirmBtn.setDefaultButton(true);
        bar.getChildren().addAll(cancelBtn, confirmBtn);
        return bar;
    }

    private void centerOnOwner(Window owner) {
        stage.setOnShown(e -> {
            double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
            double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
            stage.setX(x);
            stage.setY(y);
        });
    }

}
