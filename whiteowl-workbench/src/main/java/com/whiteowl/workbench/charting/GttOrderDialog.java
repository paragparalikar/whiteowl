package com.whiteowl.workbench.charting;

import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
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
public final class GttOrderDialog extends BaseDialog {

    private static final String TITLE_CREATE_PREFIX = "GTT ";
    private static final String TITLE_EDIT_PREFIX = "Edit GTT ";
    private static final String CREATE_TEXT = "Place GTT";
    private static final String UPDATE_TEXT = "Update GTT";
    private static final String TRIGGER_PRICE_LABEL = "Trigger Price";
    private static final String ORDER_PRICE_LABEL = "Order Price";
    private static final String QUANTITY_LABEL = "Quantity";
    private static final String PRODUCT_LABEL = "Product";
    private static final String TRAILING_LABEL = "Trailing";
    private static final String TRAILING_POINTS_LABEL = "Trailing Points";
    private static final int DIALOG_WIDTH = 420;
    private static final int DEFAULT_QUANTITY = 1;
    private static final String EXPIRY_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int EXPIRY_YEARS = 1;

    private final OrderSide side;
    private final float triggerPrice;
    private final float lastPrice;
    private final String scripId;
    private final GttOrder existingGtt;
    private final TextField triggerPriceField;
    private final TextField orderPriceField;
    private final TextField quantityField;
    private final ComboBox<Product> productCombo;
    private final CheckBox trailingCheckBox;
    private final TextField trailingPointsField;
    @Getter private GttOrder result;

    public GttOrderDialog(OrderSide side, float triggerPrice, float lastPrice, String scripId,
                          int suggestedQuantity) {
        this.side = side;
        this.triggerPrice = triggerPrice;
        this.lastPrice = lastPrice;
        this.scripId = scripId;
        this.existingGtt = null;
        this.triggerPriceField = createTextField(formatPrice(triggerPrice));
        this.orderPriceField = createTextField(formatPrice(triggerPrice));
        int qty = suggestedQuantity > 0 ? suggestedQuantity : DEFAULT_QUANTITY;
        this.quantityField = createTextField(String.valueOf(qty));
        this.productCombo = createProductCombo(Product.CNC);
        this.trailingPointsField = createTextField("");
        this.trailingPointsField.setDisable(true);
        this.trailingCheckBox = new CheckBox();
        this.trailingCheckBox.setOnAction(e -> trailingPointsField.setDisable(!trailingCheckBox.isSelected()));
    }

    public GttOrderDialog(GttOrder gtt) {
        this.side = gtt.getSide();
        this.triggerPrice = gtt.getTriggerPrice();
        this.lastPrice = gtt.getLastPrice();
        this.scripId = gtt.getScripId();
        this.existingGtt = gtt;
        this.triggerPriceField = createTextField(formatPrice(triggerPrice));
        this.orderPriceField = createTextField(formatPrice(gtt.getOrderPrice()));
        this.quantityField = createTextField(String.valueOf(gtt.getQuantity()));
        Product defaultProduct = gtt.getProduct() != null ? gtt.getProduct() : Product.CNC;
        this.productCombo = createProductCombo(defaultProduct);
        boolean hasTrailing = gtt.getTrailingPoints() > 0;
        this.trailingPointsField = createTextField(hasTrailing ? formatPrice(gtt.getTrailingPoints()) : "");
        this.trailingPointsField.setDisable(!hasTrailing);
        this.trailingCheckBox = new CheckBox();
        this.trailingCheckBox.setSelected(hasTrailing);
        this.trailingCheckBox.setOnAction(e -> trailingPointsField.setDisable(!trailingCheckBox.isSelected()));
    }

    @Override
    protected String getTitle() {
        return existingGtt != null ? TITLE_EDIT_PREFIX + side.name() : TITLE_CREATE_PREFIX + side.name();
    }

    @Override
    protected Ikon getHeaderIcon() {
        return existingGtt != null ? FluentUiRegularAL.EDIT_16 : FluentUiRegularAL.CLOCK_16;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        int row = 0;
        addFormRow(grid, TRIGGER_PRICE_LABEL, triggerPriceField, row++);
        addFormRow(grid, ORDER_PRICE_LABEL, orderPriceField, row++);
        addFormRow(grid, QUANTITY_LABEL, quantityField, row++);
        addFormRow(grid, PRODUCT_LABEL, productCombo, row++);
        addFormRow(grid, TRAILING_LABEL, trailingCheckBox, row++);
        addFormRow(grid, TRAILING_POINTS_LABEL, trailingPointsField, row);
    }

    @Override
    protected String getPrimaryButtonText() {
        return existingGtt != null ? UPDATE_TEXT : CREATE_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SEND_20;
    }

    @Override
    protected void onPrimaryAction() {
        try {
            float trigger = Float.parseFloat(triggerPriceField.getText());
            float orderPrice = Float.parseFloat(orderPriceField.getText());
            int qty = Integer.parseInt(quantityField.getText());
            Product product = productCombo.getValue();
            String expiresAt = LocalDateTime.now().plusYears(EXPIRY_YEARS)
                    .format(DateTimeFormatter.ofPattern(EXPIRY_FORMAT));
            float trailing = 0;
            if (trailingCheckBox.isSelected() && !trailingPointsField.getText().isBlank()) {
                trailing = Float.parseFloat(trailingPointsField.getText());
            }
            result = GttOrder.builder()
                    .id(existingGtt != null ? existingGtt.getId() : 0)
                    .scripId(scripId)
                    .side(side)
                    .limitType(LimitType.LIMIT)
                    .product(product)
                    .quantity(qty)
                    .triggerPrice(trigger)
                    .orderPrice(orderPrice)
                    .lastPrice(lastPrice)
                    .trailingPoints(trailing)
                    .status(GttStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .build();
            closeDialog();
        } catch (NumberFormatException ex) {
            log.warn("Invalid GTT order value: {}", ex.getMessage());
        }
    }

    private ComboBox<Product> createProductCombo(Product defaultValue) {
        ComboBox<Product> combo = createComboBox();
        combo.getItems().addAll(Product.values());
        combo.setValue(defaultValue);
        return combo;
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
