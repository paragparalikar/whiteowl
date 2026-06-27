package com.whiteowl.workbench.charting;

import com.whiteowl.core.order.model.LimitType;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderSide;
import com.whiteowl.core.order.model.Product;
import com.whiteowl.core.order.model.Validity;
import com.whiteowl.core.order.model.Variety;
import com.whiteowl.workbench.common.BaseDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

import java.util.UUID;

@Slf4j
public final class RegularOrderDialog extends BaseDialog {

    private static final String TITLE_CREATE_PREFIX = "Order ";
    private static final String TITLE_EDIT_PREFIX = "Edit Order ";
    private static final String CREATE_TEXT = "Place Order";
    private static final String UPDATE_TEXT = "Update Order";
    private static final String PRICE_LABEL = "Price";
    private static final String TRIGGER_PRICE_LABEL = "Trigger Price";
    private static final String QUANTITY_LABEL = "Quantity";
    private static final String PRODUCT_LABEL = "Product";
    private static final String ORDER_TYPE_LABEL = "Order Type";
    private static final String VALIDITY_LABEL = "Validity";
    private static final int DIALOG_WIDTH = 420;
    private static final int DEFAULT_QUANTITY = 1;
    private static final String ZERO_PRICE = "0";

    private final OrderSide side;
    private final float price;
    private final String scripId;
    private final Order existingOrder;
    private final TextField priceField;
    private final TextField triggerPriceField;
    private final TextField quantityField;
    private final ComboBox<LimitType> orderTypeCombo;
    private final ComboBox<Product> productCombo;
    private final ComboBox<Validity> validityCombo;
    private Label triggerPriceLabel;
    @Getter private Order result;

    public RegularOrderDialog(OrderSide side, float price, String scripId, int suggestedQuantity) {
        this.side = side;
        this.price = price;
        this.scripId = scripId;
        this.existingOrder = null;
        this.orderTypeCombo = createOrderTypeCombo(LimitType.LIMIT);
        this.priceField = createTextField(formatPrice(price));
        this.triggerPriceField = createTextField(ZERO_PRICE);
        int qty = suggestedQuantity > 0 ? suggestedQuantity : DEFAULT_QUANTITY;
        this.quantityField = createTextField(String.valueOf(qty));
        this.productCombo = createEnumCombo(Product.values(), Product.CNC);
        this.validityCombo = createEnumCombo(new Validity[]{Validity.DAY, Validity.IOC}, Validity.DAY);
    }

    public RegularOrderDialog(Order order) {
        this.side = order.getSide();
        this.price = order.getPrice();
        this.scripId = order.getScripId();
        this.existingOrder = order;
        LimitType defaultType = order.getLimitType() != null ? order.getLimitType() : LimitType.LIMIT;
        this.orderTypeCombo = createOrderTypeCombo(defaultType);
        this.priceField = createTextField(formatPrice(price));
        String triggerValue = order.getTriggerPrice() > 0 ? formatPrice(order.getTriggerPrice()) : ZERO_PRICE;
        this.triggerPriceField = createTextField(triggerValue);
        this.quantityField = createTextField(String.valueOf(order.getQuantity()));
        Product defaultProduct = order.getProduct() != null ? order.getProduct() : Product.CNC;
        this.productCombo = createEnumCombo(Product.values(), defaultProduct);
        Validity defaultValidity = order.getValidity() != null ? order.getValidity() : Validity.DAY;
        this.validityCombo = createEnumCombo(new Validity[]{Validity.DAY, Validity.IOC}, defaultValidity);
    }

    @Override
    protected String getTitle() {
        return existingOrder != null ? TITLE_EDIT_PREFIX + side.name() : TITLE_CREATE_PREFIX + side.name();
    }

    @Override
    protected Ikon getHeaderIcon() {
        return existingOrder != null ? FluentUiRegularAL.EDIT_16 : FluentUiRegularAL.CART_16;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        triggerPriceLabel = createLabel(TRIGGER_PRICE_LABEL);
        int row = 0;
        addFormRow(grid, ORDER_TYPE_LABEL, orderTypeCombo, row++);
        addFormRow(grid, PRICE_LABEL, priceField, row++);
        grid.add(triggerPriceLabel, 0, row);
        grid.add(triggerPriceField, 1, row);
        row++;
        addFormRow(grid, QUANTITY_LABEL, quantityField, row++);
        addFormRow(grid, PRODUCT_LABEL, productCombo, row++);
        addFormRow(grid, VALIDITY_LABEL, validityCombo, row);
        orderTypeCombo.setOnAction(e -> updateFieldVisibility());
        updateFieldVisibility();
    }

    @Override
    protected String getPrimaryButtonText() {
        return existingOrder != null ? UPDATE_TEXT : CREATE_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SEND_20;
    }

    @Override
    protected void onPrimaryAction() {
        try {
            LimitType limitType = orderTypeCombo.getValue();
            float orderPrice = limitType == LimitType.MARKET || limitType == LimitType.STOP_LOSS_MARKET
                    ? 0 : Float.parseFloat(priceField.getText());
            float trigger = (limitType == LimitType.STOP_LOSS || limitType == LimitType.STOP_LOSS_MARKET)
                    ? Float.parseFloat(triggerPriceField.getText()) : 0;
            int qty = Integer.parseInt(quantityField.getText());
            Product product = productCombo.getValue();
            Validity validity = validityCombo.getValue();
            result = Order.builder()
                    .id(existingOrder != null ? existingOrder.getId() : UUID.randomUUID().toString())
                    .exchangeOrderId(existingOrder != null ? existingOrder.getExchangeOrderId() : null)
                    .scripId(scripId)
                    .side(side)
                    .limitType(limitType)
                    .product(product)
                    .variety(Variety.REGULAR)
                    .validity(validity)
                    .quantity(qty)
                    .price(orderPrice)
                    .triggerPrice(trigger)
                    .build();
            closeDialog();
        } catch (NumberFormatException ex) {
            log.warn("Invalid order value: {}", ex.getMessage());
        }
    }

    private void updateFieldVisibility() {
        LimitType type = orderTypeCombo.getValue();
        boolean isMarket = type == LimitType.MARKET || type == LimitType.STOP_LOSS_MARKET;
        boolean hasTrigger = type == LimitType.STOP_LOSS || type == LimitType.STOP_LOSS_MARKET;
        priceField.setDisable(isMarket);
        triggerPriceField.setVisible(hasTrigger);
        triggerPriceField.setManaged(hasTrigger);
        triggerPriceLabel.setVisible(hasTrigger);
        triggerPriceLabel.setManaged(hasTrigger);
    }

    private ComboBox<LimitType> createOrderTypeCombo(LimitType defaultType) {
        ComboBox<LimitType> combo = createComboBox();
        combo.getItems().addAll(LimitType.values());
        combo.setValue(defaultType);
        return combo;
    }

    private <T> ComboBox<T> createEnumCombo(T[] values, T defaultValue) {
        ComboBox<T> combo = createComboBox();
        combo.getItems().addAll(values);
        combo.setValue(defaultValue);
        return combo;
    }

    private String formatPrice(float price) {
        return price >= 1000 ? String.format("%.0f", price) : String.format("%.2f", price);
    }

}
