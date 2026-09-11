package com.whiteowl.workbench.charting;

import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.LimitType;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.order.model.Product;
import com.whiteowl.workbench.common.BaseDialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class OcoGttOrderDialog extends BaseDialog {

    private static final String TITLE_CREATE = "OCO GTT";
    private static final String TITLE_EDIT = "Edit OCO GTT";
    private static final String CREATE_TEXT = "Place OCO";
    private static final String UPDATE_TEXT = "Update OCO";
    private static final String SL_TRIGGER_LABEL = "SL Trigger";
    private static final String SL_ORDER_LABEL = "SL Order Price";
    private static final String TGT_TRIGGER_LABEL = "Target Trigger";
    private static final String TGT_ORDER_LABEL = "Target Order Price";
    private static final String QUANTITY_LABEL = "Quantity";
    private static final String PRODUCT_LABEL = "Product";
    private static final String TRAILING_LABEL = "Trailing";
    private static final String TRAILING_POINTS_LABEL = "Trailing Points";
    private static final int DIALOG_WIDTH = 420;
    private static final String EXPIRY_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int EXPIRY_YEARS = 1;

    private final OrderSide side;
    private final String scripId;
    private final float lastPrice;
    private final OcoGttOrder existingOco;
    private final TextField slTriggerField;
    private final TextField slOrderField;
    private final TextField tgtTriggerField;
    private final TextField tgtOrderField;
    private final TextField quantityField;
    private final ComboBox<Product> productCombo;
    private final CheckBox trailingCheckBox;
    private final TextField trailingPointsField;
    @Getter private OcoGttOrder result;

    public OcoGttOrderDialog(OrderSide side, float slPrice, float tgtPrice,
                             float lastPrice, String scripId, int quantity) {
        this.side = side;
        this.scripId = scripId;
        this.lastPrice = lastPrice;
        this.existingOco = null;
        this.slTriggerField = createTextField(formatPrice(slPrice));
        this.slOrderField = createTextField(formatPrice(slPrice));
        this.tgtTriggerField = createTextField(formatPrice(tgtPrice));
        this.tgtOrderField = createTextField(formatPrice(tgtPrice));
        this.quantityField = createTextField(String.valueOf(Math.max(quantity, 1)));
        this.productCombo = buildProductCombo(Product.CNC);
        this.trailingPointsField = createTextField("");
        this.trailingPointsField.setDisable(true);
        this.trailingCheckBox = new CheckBox();
        this.trailingCheckBox.setOnAction(e -> trailingPointsField.setDisable(!trailingCheckBox.isSelected()));
    }

    public OcoGttOrderDialog(OcoGttOrder oco) {
        this.side = oco.getSide();
        this.scripId = oco.getScripId();
        this.lastPrice = oco.getLastPrice();
        this.existingOco = oco;
        this.slTriggerField = createTextField(formatPrice(oco.getStoplossTriggerPrice()));
        this.slOrderField = createTextField(formatPrice(oco.getStoplossOrderPrice()));
        this.tgtTriggerField = createTextField(formatPrice(oco.getTargetTriggerPrice()));
        this.tgtOrderField = createTextField(formatPrice(oco.getTargetOrderPrice()));
        this.quantityField = createTextField(String.valueOf(oco.getQuantity()));
        Product defaultProduct = oco.getProduct() != null ? oco.getProduct() : Product.CNC;
        this.productCombo = buildProductCombo(defaultProduct);
        boolean hasTrailing = oco.getTrailingPoints() > 0;
        this.trailingPointsField = createTextField(hasTrailing ? formatPrice(oco.getTrailingPoints()) : "");
        this.trailingPointsField.setDisable(!hasTrailing);
        this.trailingCheckBox = new CheckBox();
        this.trailingCheckBox.setSelected(hasTrailing);
        this.trailingCheckBox.setOnAction(e -> trailingPointsField.setDisable(!trailingCheckBox.isSelected()));
    }

    @Override
    protected String getTitle() {
        return existingOco != null ? TITLE_EDIT : TITLE_CREATE;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return existingOco != null ? FluentUiRegularAL.EDIT_16 : FluentUiRegularAL.CLOCK_16;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        int row = 0;
        addFormRow(grid, SL_TRIGGER_LABEL, slTriggerField, row++);
        addFormRow(grid, SL_ORDER_LABEL, slOrderField, row++);
        addFormRow(grid, TGT_TRIGGER_LABEL, tgtTriggerField, row++);
        addFormRow(grid, TGT_ORDER_LABEL, tgtOrderField, row++);
        addFormRow(grid, QUANTITY_LABEL, quantityField, row++);
        addFormRow(grid, PRODUCT_LABEL, productCombo, row++);
        addFormRow(grid, TRAILING_LABEL, trailingCheckBox, row++);
        addFormRow(grid, TRAILING_POINTS_LABEL, trailingPointsField, row);
    }

    @Override
    protected String getPrimaryButtonText() {
        return existingOco != null ? UPDATE_TEXT : CREATE_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SEND_20;
    }

    @Override
    protected void onPrimaryAction() {
        try {
            float slTrigger = Float.parseFloat(slTriggerField.getText());
            float slOrder = Float.parseFloat(slOrderField.getText());
            float tgtTrigger = Float.parseFloat(tgtTriggerField.getText());
            float tgtOrder = Float.parseFloat(tgtOrderField.getText());
            int qty = Integer.parseInt(quantityField.getText());
            Product product = productCombo.getValue();
            String expiresAt = LocalDateTime.now().plusYears(EXPIRY_YEARS)
                    .format(DateTimeFormatter.ofPattern(EXPIRY_FORMAT));
            float trailing = 0;
            if (trailingCheckBox.isSelected() && !trailingPointsField.getText().isBlank()) {
                trailing = Float.parseFloat(trailingPointsField.getText());
            }
            result = OcoGttOrder.builder()
                    .id(existingOco != null ? existingOco.getId() : 0)
                    .scripId(scripId)
                    .side(side)
                    .limitType(LimitType.LIMIT)
                    .product(product)
                    .quantity(qty)
                    .stoplossTriggerPrice(slTrigger)
                    .stoplossOrderPrice(slOrder)
                    .targetTriggerPrice(tgtTrigger)
                    .targetOrderPrice(tgtOrder)
                    .lastPrice(lastPrice)
                    .trailingPoints(trailing)
                    .status(GttStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .build();
            closeDialog();
        } catch (NumberFormatException ex) {
            log.warn("Invalid OCO GTT value: {}", ex.getMessage());
        }
    }

    private ComboBox<Product> buildProductCombo(Product defaultValue) {
        ComboBox<Product> combo = createComboBox();
        combo.getItems().addAll(Product.values());
        combo.setValue(defaultValue);
        return combo;
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
