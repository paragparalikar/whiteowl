package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteGttCondition;
import com.whiteowl.client.kite.model.KiteGttOrder;
import com.whiteowl.client.kite.model.KiteGttStatus;
import com.whiteowl.client.kite.model.KiteGttTrigger;
import com.whiteowl.client.kite.model.KiteGttType;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
final class KiteGttMapper {

    private final KiteScripMapper scripMapper;
    private final KiteExchangeMapper exchangeMapper;
    private final KiteProductMapper productMapper;
    private final KiteLimitTypeMapper limitTypeMapper;
    private final KiteOrderSideMapper orderSideMapper;

    public GttOrder toGttOrder(KiteGttTrigger trigger) {
        if (trigger == null) return null;
        KiteGttCondition condition = trigger.getCondition();
        KiteGttOrder kiteOrder = trigger.getOrders().isEmpty() ? null : trigger.getOrders().getFirst();
        if (condition == null || kiteOrder == null) return null;
        Scrip scrip = scripMapper.getScrip(condition.getExchange(), condition.getTradingsymbol());
        String scripId = scrip != null ? scrip.getId() : buildFallbackScripId(condition);
        float triggerPrice = condition.getTriggerValues().length > 0 ? condition.getTriggerValues()[0] : 0;
        return GttOrder.builder()
                .id(trigger.getId())
                .scripId(scripId)
                .side(orderSideMapper.toOrderSide(kiteOrder.getTransactionType()))
                .limitType(limitTypeMapper.toLimitType(kiteOrder.getOrderType()))
                .product(productMapper.toProduct(kiteOrder.getProduct()))
                .quantity(kiteOrder.getQuantity())
                .triggerPrice(triggerPrice)
                .orderPrice(kiteOrder.getPrice())
                .lastPrice(condition.getLastPrice())
                .status(toGttStatus(trigger.getStatus()))
                .expiresAt(trigger.getExpiresAt())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }

    public KiteGttCondition toKiteGttCondition(GttOrder gtt, Scrip scrip, KiteSymbol kiteSymbol) {
        return KiteGttCondition.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : gtt.getScripId())
                .triggerValues(new float[]{gtt.getTriggerPrice()})
                .lastPrice(gtt.getLastPrice())
                .instrumentToken(kiteSymbol != null ? kiteSymbol.getInstrumentToken() : 0)
                .build();
    }

    public KiteGttOrder toKiteGttOrder(GttOrder gtt, Scrip scrip) {
        return KiteGttOrder.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : gtt.getScripId())
                .transactionType(orderSideMapper.toKiteTransactionType(gtt.getSide()))
                .quantity(gtt.getQuantity())
                .orderType(limitTypeMapper.toKiteLimitType(gtt.getLimitType()))
                .product(productMapper.toKiteProduct(gtt.getProduct()))
                .price(gtt.getOrderPrice())
                .build();
    }

    private GttStatus toGttStatus(KiteGttStatus kiteStatus) {
        if (kiteStatus == null) return null;
        return switch (kiteStatus) {
            case ACTIVE -> GttStatus.ACTIVE;
            case TRIGGERED -> GttStatus.TRIGGERED;
            case DISABLED -> GttStatus.DISABLED;
            case EXPIRED -> GttStatus.EXPIRED;
            case CANCELLED -> GttStatus.CANCELLED;
            case REJECTED -> GttStatus.REJECTED;
        };
    }

    private String buildFallbackScripId(KiteGttCondition condition) {
        if (condition.getExchange() != null) {
            return condition.getExchange().name() + ":" + condition.getTradingsymbol();
        }
        return condition.getTradingsymbol();
    }

}
