package com.whiteowl.workbench.common;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.Ikon;

public abstract class BaseDialog {

    private static final String CARD_STYLE = "dialog-card";
    private static final String FORM_STYLE = "dialog-form";
    private static final String HEADER_STYLE = "dialog-header";
    private static final String HEADER_ICON_STYLE = "dialog-header-icon";
    private static final String LABEL_STYLE = "dialog-label";
    private static final String FIELD_STYLE = "dialog-field";
    private static final String COMBO_STYLE = "dialog-combo";
    private static final String SPINNER_STYLE = "dialog-spinner";
    private static final String CHECKBOX_STYLE = "dialog-checkbox";
    private static final String MESSAGE_STYLE = "dialog-message";
    private static final String ERROR_STYLE = "dialog-error";
    private static final String DIVIDER_STYLE = "dialog-divider";
    private static final String BUTTON_ROW_STYLE = "dialog-button-row";
    private static final String PRIMARY_BUTTON_STYLE = "dialog-primary-button";
    private static final String PRIMARY_ICON_STYLE = "dialog-primary-icon";
    private static final String SECONDARY_BUTTON_STYLE = "dialog-secondary-button";
    private static final String SECONDARY_ICON_STYLE = "dialog-secondary-icon";
    private static final String CANCEL_TEXT = "Cancel";
    private static final int ICON_SIZE = 14;
    private static final int SMALL_ICON_SIZE = 12;
    private static final int FORM_SPACING = 10;
    private static final int FORM_HGAP = 10;
    private static final int FORM_VGAP = 8;
    private static final int BUTTON_GAP = 8;
    private static final int HEADER_ICON_GAP = 6;
    private static final double LABEL_COL_PERCENT = 22;
    private static final double FIELD_COL_PERCENT = 78;
    private static final double RESIZE_BORDER = 6;
    private static final double MIN_WIDTH = 300;
    private static final double MIN_HEIGHT = 200;

    private final Label messageLabel;
    private Stage stage;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;
    private double resizeStartStageX;
    private double resizeStartStageY;
    private Cursor resizeCursor = Cursor.DEFAULT;

    protected BaseDialog() {
        this.messageLabel = createMessageLabel();
    }

    protected abstract String getTitle();

    protected abstract Ikon getHeaderIcon();

    protected abstract int getDialogWidth();

    protected abstract void buildFormFields(GridPane grid);

    protected abstract String getPrimaryButtonText();

    protected abstract Ikon getPrimaryButtonIcon();

    protected abstract void onPrimaryAction();

    public void show(Window owner) {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        VBox root = assembleContent();
        root.getStyleClass().add(CARD_STYLE);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        stage.setWidth(getDialogWidth());
        installResizeHandlers(root);
        centerOnOwner(owner);
        stage.showAndWait();
    }

    protected void closeDialog() {
        if (stage != null) {
            stage.close();
        }
    }

    protected void showError(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll(ERROR_STYLE);
        messageLabel.getStyleClass().add(ERROR_STYLE);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        double currentWidth = stage.getWidth();
        stage.sizeToScene();
        stage.setWidth(currentWidth);
    }

    protected void clearMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    protected Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(LABEL_STYLE);
        return label;
    }

    protected TextField createTextField() {
        TextField field = new TextField();
        field.getStyleClass().add(FIELD_STYLE);
        return field;
    }

    protected TextField createTextField(String text) {
        TextField field = new TextField(text);
        field.getStyleClass().add(FIELD_STYLE);
        return field;
    }

    protected PasswordField createPasswordField() {
        PasswordField field = new PasswordField();
        field.getStyleClass().add(FIELD_STYLE);
        return field;
    }

    protected <T> ComboBox<T> createComboBox() {
        ComboBox<T> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add(COMBO_STYLE);
        return combo;
    }

    protected Spinner<Integer> createSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.getStyleClass().add(SPINNER_STYLE);
        return spinner;
    }

    protected Spinner<Double> createDoubleSpinner(double min, double max, double initial, double step) {
        Spinner<Double> spinner = new Spinner<>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.getStyleClass().add(SPINNER_STYLE);
        return spinner;
    }

    protected CheckBox createCheckBox(boolean selected) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(selected);
        checkBox.getStyleClass().add(CHECKBOX_STYLE);
        return checkBox;
    }

    protected GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(FORM_HGAP);
        grid.setVgap(FORM_VGAP);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(LABEL_COL_PERCENT);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setPercentWidth(FIELD_COL_PERCENT);
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);
        return grid;
    }

    protected void addFormRow(GridPane grid, String labelText, Node field, int row) {
        grid.add(createLabel(labelText), 0, row);
        grid.add(field, 1, row);
    }

    protected void addExtraContent(VBox form) {
    }

    private VBox assembleContent() {
        GridPane grid = createFormGrid();
        buildFormFields(grid);
        Region divider = new Region();
        divider.getStyleClass().add(DIVIDER_STYLE);
        HBox buttonRow = buildButtonRow();
        VBox form = new VBox(FORM_SPACING, buildHeaderRow(), grid, messageLabel);
        addExtraContent(form);
        form.getChildren().addAll(divider, buttonRow);
        form.getStyleClass().add(FORM_STYLE);
        return form;
    }

    private HBox buildHeaderRow() {
        FontIcon icon = new FontIcon(getHeaderIcon());
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(HEADER_ICON_STYLE);
        Label header = new Label(getTitle());
        header.getStyleClass().add(HEADER_STYLE);
        HBox row = new HBox(HEADER_ICON_GAP, icon, header);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildButtonRow() {
        FontIcon primaryIcon = new FontIcon(getPrimaryButtonIcon());
        primaryIcon.setIconSize(ICON_SIZE);
        primaryIcon.getStyleClass().add(PRIMARY_ICON_STYLE);
        Button primaryButton = new Button(getPrimaryButtonText(), primaryIcon);
        primaryButton.getStyleClass().add(PRIMARY_BUTTON_STYLE);
        primaryButton.setDefaultButton(true);
        primaryButton.setOnAction(e -> onPrimaryAction());
        FontIcon cancelIcon = new FontIcon(FluentUiRegularAL.DISMISS_16);
        cancelIcon.setIconSize(SMALL_ICON_SIZE);
        cancelIcon.getStyleClass().add(SECONDARY_ICON_STYLE);
        Button cancelButton = new Button(CANCEL_TEXT, cancelIcon);
        cancelButton.getStyleClass().add(SECONDARY_BUTTON_STYLE);
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> closeDialog());
        HBox row = new HBox(BUTTON_GAP, primaryButton, cancelButton);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.getStyleClass().add(BUTTON_ROW_STYLE);
        return row;
    }

    private Label createMessageLabel() {
        Label label = new Label();
        label.getStyleClass().add(MESSAGE_STYLE);
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void centerOnOwner(Window owner) {
        stage.setOnShown(e -> {
            double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
            double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
            stage.setX(x);
            stage.setY(y);
        });
    }

    private void installResizeHandlers(Node root) {
        root.setOnMouseMoved(e -> {
            resizeCursor = detectResizeCursor(e.getX(), e.getY());
            stage.getScene().setCursor(resizeCursor);
        });
        root.setOnMousePressed(e -> {
            resizeCursor = detectResizeCursor(e.getX(), e.getY());
            if (resizeCursor != Cursor.DEFAULT) {
                resizeStartX = e.getScreenX();
                resizeStartY = e.getScreenY();
                resizeStartW = stage.getWidth();
                resizeStartH = stage.getHeight();
                resizeStartStageX = stage.getX();
                resizeStartStageY = stage.getY();
                e.consume();
            }
        });
        root.setOnMouseDragged(e -> {
            if (resizeCursor == Cursor.DEFAULT) return;
            double dx = e.getScreenX() - resizeStartX;
            double dy = e.getScreenY() - resizeStartY;
            if (resizeCursor == Cursor.E_RESIZE || resizeCursor == Cursor.SE_RESIZE || resizeCursor == Cursor.NE_RESIZE) {
                stage.setWidth(Math.max(MIN_WIDTH, resizeStartW + dx));
            }
            if (resizeCursor == Cursor.W_RESIZE || resizeCursor == Cursor.SW_RESIZE || resizeCursor == Cursor.NW_RESIZE) {
                double newW = Math.max(MIN_WIDTH, resizeStartW - dx);
                stage.setX(resizeStartStageX + resizeStartW - newW);
                stage.setWidth(newW);
            }
            if (resizeCursor == Cursor.S_RESIZE || resizeCursor == Cursor.SE_RESIZE || resizeCursor == Cursor.SW_RESIZE) {
                stage.setHeight(Math.max(MIN_HEIGHT, resizeStartH + dy));
            }
            if (resizeCursor == Cursor.N_RESIZE || resizeCursor == Cursor.NE_RESIZE || resizeCursor == Cursor.NW_RESIZE) {
                double newH = Math.max(MIN_HEIGHT, resizeStartH - dy);
                stage.setY(resizeStartStageY + resizeStartH - newH);
                stage.setHeight(newH);
            }
            e.consume();
        });
        root.setOnMouseReleased(e -> {
            if (resizeCursor != Cursor.DEFAULT) {
                resizeCursor = Cursor.DEFAULT;
                stage.getScene().setCursor(Cursor.DEFAULT);
                e.consume();
            }
        });
    }

    private Cursor detectResizeCursor(double x, double y) {
        double w = stage.getWidth();
        double h = stage.getHeight();
        boolean top = y < RESIZE_BORDER;
        boolean bottom = y > h - RESIZE_BORDER;
        boolean left = x < RESIZE_BORDER;
        boolean right = x > w - RESIZE_BORDER;
        if (top && left) return Cursor.NW_RESIZE;
        if (top && right) return Cursor.NE_RESIZE;
        if (bottom && left) return Cursor.SW_RESIZE;
        if (bottom && right) return Cursor.SE_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;
        return Cursor.DEFAULT;
    }

}
